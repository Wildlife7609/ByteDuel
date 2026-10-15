package com.kob.botrunningsystem.service.impl.utils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 通过 go-judge 编译、运行用户代码。每次运行都在独立的沙箱里：没有网络，限制 CPU 时间、内存和进程数
// 接口文档：https://docs.goj.ac/api
@Component
public class Sandbox {
    private static final long MS = 1_000_000L; // go-judge 的时间单位是纳秒
    private static final long MB = 1024L * 1024L;
    private static final int MAX_CACHE_SIZE = 1000;
    private static final List<String> ENV = List.of(
            "PATH=/usr/bin:/bin",
            "HOME=/tmp",
            // Go 编译需要可写的缓存目录，且禁止联网下载工具链
            "GOCACHE=/tmp/gocache", "GOPATH=/tmp/go", "CGO_ENABLED=0", "GOTOOLCHAIN=local");

    @Autowired
    @Qualifier("sandboxClient")
    private RestClient sandboxClient;

    // 编译缓存：key 是语言 + 代码，value 是编译产物在 go-judge 里的 fileId。同一份代码只编译一次
    private final ConcurrentHashMap<String, String> compiledBots = new ConcurrentHashMap<>();

    // 启动时缓存是空的，go-judge 里上次运行留下的编译产物再也用不到了，不删的话每次重启都会多一批一直占着内存
    // 只有本服务用 go-judge，所以里面的文件都可以删
    @PostConstruct
    public void clearSandboxFiles() {
        try {
            Map<String, String> files = sandboxClient.get().uri("/file")
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, String>>() {});
            if (files != null)
                for (String fileId : files.keySet())
                    sandboxClient.delete().uri("/file/{id}", fileId).retrieve().toBodilessEntity();
        } catch (Exception e) {
            e.printStackTrace(); // go-judge 还没启动好也不影响本服务启动
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Result(String status, String error, Map<String, String> files, Map<String, String> fileIds) {
        String output(String name) {
            return files == null ? "" : files.getOrDefault(name, "");
        }
    }

    // 编译失败，message 是给用户看的错误信息
    public static class CompileException extends RuntimeException {
        public CompileException(String message) {
            super(message);
        }
    }

    // 保存前检查、对局开始前预热都会调用：确保编译产物在 go-judge 里可用，对局时每回合就不用现场编译
    // 通过返回 null，否则返回给用户看的错误信息
    public String check(Language language, String code) {
        try {
            String fileId = getFileId(language, code);
            if (!cachedInSandbox(fileId)) {
                // go-judge 重启后缓存的文件就没了，但这边还记着 fileId，重新编译一次
                compiledBots.remove(cacheKey(language, code));
                getFileId(language, code);
            }
            return null;
        } catch (CompileException e) {
            return e.getMessage();
        }
    }

    // 运行 Bot，返回标准输出；编译失败、超时、运行出错返回 null
    public String run(Language language, String code, String input) {
        Result result;
        try {
            result = execute(language, getFileId(language, code), input);
            if ("File Error".equals(result.status())) {
                // go-judge 重启后缓存的文件就没了，重新编译一次
                compiledBots.remove(cacheKey(language, code));
                result = execute(language, getFileId(language, code), input);
            }
        } catch (CompileException e) {
            return null;
        }
        if (!"Accepted".equals(result.status())) {
            System.out.println("Bot 运行失败：" + result.status() + " " + result.output("stderr"));
            return null;
        }
        return result.output("stdout");
    }

    private String cacheKey(Language language, String code) {
        return language.getId() + "\n" + code;
    }

    private String getFileId(Language language, String code) {
        if (compiledBots.size() >= MAX_CACHE_SIZE)
            clearCache();
        // 同一份代码被多个线程同时请求时，只有一个线程编译，其他线程等它编译完直接用
        return compiledBots.computeIfAbsent(cacheKey(language, code), key -> compile(language, code));
    }

    private boolean cachedInSandbox(String fileId) {
        // 返回 go-judge 里所有缓存文件：fileId -> 文件名
        Map<String, String> files = sandboxClient.get().uri("/file")
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, String>>() {});
        return files != null && files.containsKey(fileId);
    }

    private void clearCache() {
        for (String fileId : compiledBots.values()) {
            try {
                sandboxClient.delete().uri("/file/{id}", fileId).retrieve().toBodilessEntity();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        compiledBots.clear();
    }

    private String compile(Language language, String code) {
        Map<String, Object> cmd = command(language.getCompileArgs(), "", 30000, 60000);
        cmd.put("copyIn", Map.of(language.getSourceFile(), Map.of("content", code)));
        cmd.put("copyOutCached", List.of(language.getExecutable()));
        Result result = send(cmd);
        if (!"Accepted".equals(result.status()))
            throw new CompileException(compileMessage(result));
        return result.fileIds().get(language.getExecutable());
    }

    private Result execute(Language language, String fileId, String input) {
        // 每回合限时 5 秒，两个 Bot 加上网络往返都要在这之内完成
        Map<String, Object> cmd = command(language.getRunArgs(), input + "\n", 2000, 3000);
        cmd.put("copyIn", Map.of(language.getExecutable(), Map.of("fileId", fileId)));
        try {
            return send(cmd);
        } catch (HttpClientErrorException.NotFound e) {
            // 文件不在 go-judge 里了（go-judge 重启过），go-judge 返回 404，当作 File Error 让调用方重新编译
            return new Result("File Error", e.getMessage(), null, null);
        }
    }

    private Map<String, Object> command(List<String> args, String stdin, long cpuLimitMs, long clockLimitMs) {
        Map<String, Object> cmd = new HashMap<>();
        cmd.put("args", args);
        cmd.put("env", ENV);
        cmd.put("files", List.of(
                Map.of("content", stdin),
                Map.of("name", "stdout", "max", 64 * 1024),
                Map.of("name", "stderr", "max", 64 * 1024)));
        cmd.put("cpuLimit", cpuLimitMs * MS);
        cmd.put("clockLimit", clockLimitMs * MS);
        cmd.put("memoryLimit", 512 * MB);
        cmd.put("procLimit", 128); // JVM 和 Go 运行时都会开不少线程，线程也算在进程数里
        return cmd;
    }

    private Result send(Map<String, Object> cmd) {
        List<Result> results = sandboxClient.post().uri("/run")
                .body(Map.of("cmd", List.of(cmd)))
                .retrieve()
                .body(new ParameterizedTypeReference<List<Result>>() {});
        return results.getFirst();
    }

    private String compileMessage(Result result) {
        if ("Time Limit Exceeded".equals(result.status()))
            return "编译超时";
        String message = (result.output("stderr") + result.output("stdout")).trim();
        if (message.isEmpty())
            message = "编译失败：" + result.status() + (result.error() == null ? "" : " " + result.error());
        return message.length() > 3000 ? message.substring(0, 3000) + "\n..." : message;
    }
}
