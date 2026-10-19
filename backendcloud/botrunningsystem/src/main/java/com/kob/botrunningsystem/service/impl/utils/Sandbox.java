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

// Compiles and runs user code through go-judge. Every run gets its own sandbox: no network, with limits on CPU time, memory and process count
// API docs: https://docs.goj.ac/api
@Component
public class Sandbox {
    private static final long MS = 1_000_000L; // go-judge measures time in nanoseconds
    private static final long MB = 1024L * 1024L;
    private static final int MAX_CACHE_SIZE = 1000;
    private static final List<String> ENV = List.of(
            "PATH=/usr/bin:/bin",
            "HOME=/tmp",
            // Go needs a writable cache directory, and must not download toolchains
            "GOCACHE=/tmp/gocache", "GOPATH=/tmp/go", "CGO_ENABLED=0", "GOTOOLCHAIN=local");

    @Autowired
    @Qualifier("sandboxClient")
    private RestClient sandboxClient;

    // Compile cache: key is language + code, value is the fileId of the compiled output in go-judge. The same code is compiled only once
    private final ConcurrentHashMap<String, String> compiledBots = new ConcurrentHashMap<>();

    // The cache starts empty, so compiled files left in go-judge by the previous run are unreachable; without cleanup, every restart would leak another batch of memory
    // only this service uses go-judge, so every file in it can be deleted
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
            e.printStackTrace(); // startup continues even if go-judge is not up yet
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Result(String status, String error, Map<String, String> files, Map<String, String> fileIds) {
        String output(String name) {
            return files == null ? "" : files.getOrDefault(name, "");
        }
    }

    // compilation failed; message is the error shown to the user
    public static class CompileException extends RuntimeException {
        public CompileException(String message) {
            super(message);
        }
    }

    // Called by the pre-save check and the pre-game warm-up: makes sure the compiled output is in go-judge, so turns never compile on the spot
    // returns null on success, otherwise an error message for the user
    public String check(Language language, String code) {
        try {
            String fileId = getFileId(language, code);
            if (!cachedInSandbox(fileId)) {
                // go-judge lost its files after a restart but the fileId is still cached here, so compile again
                compiledBots.remove(cacheKey(language, code));
                getFileId(language, code);
            }
            return null;
        } catch (CompileException e) {
            return e.getMessage();
        }
    }

    // Runs a bot and returns its stdout; returns null on compile error, timeout or runtime error
    public String run(Language language, String code, String input) {
        Result result;
        try {
            result = execute(language, getFileId(language, code), input);
            if ("File Error".equals(result.status())) {
                // go-judge lost its files after a restart, so compile again
                compiledBots.remove(cacheKey(language, code));
                result = execute(language, getFileId(language, code), input);
            }
        } catch (CompileException e) {
            return null;
        }
        if (!"Accepted".equals(result.status())) {
            System.out.println("Bot run failed: " + result.status() + " " + result.output("stderr"));
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
        // when several threads request the same code at once, one compiles and the others wait for its result
        return compiledBots.computeIfAbsent(cacheKey(language, code), key -> compile(language, code));
    }

    private boolean cachedInSandbox(String fileId) {
        // all files cached in go-judge: fileId -> file name
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
        // a turn is limited to 5 seconds, and both bots plus network round trips must fit in it
        Map<String, Object> cmd = command(language.getRunArgs(), input + "\n", 2000, 3000);
        cmd.put("copyIn", Map.of(language.getExecutable(), Map.of("fileId", fileId)));
        try {
            return send(cmd);
        } catch (HttpClientErrorException.NotFound e) {
            // the file is gone from go-judge (it restarted) and it returns 404; treat it as File Error so the caller recompiles
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
        cmd.put("procLimit", 128); // the JVM and the Go runtime both start many threads, and threads count as processes
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
            return "Compilation timed out";
        String message = (result.output("stderr") + result.output("stdout")).trim();
        if (message.isEmpty())
            message = "Compilation failed: " + result.status() + (result.error() == null ? "" : " " + result.error());
        return message.length() > 3000 ? message.substring(0, 3000) + "\n..." : message;
    }
}
