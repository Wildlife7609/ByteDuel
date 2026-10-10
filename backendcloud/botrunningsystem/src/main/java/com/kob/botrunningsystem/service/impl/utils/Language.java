package com.kob.botrunningsystem.service.impl.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

// 每种语言怎么编译、怎么运行。命令在 go-judge 沙箱里执行，工作目录是 /w
// 约定：程序从标准输入读一行局面，向标准输出打印方向 0~3
@Getter
@AllArgsConstructor
public enum Language {
    CPP("cpp", "main.cpp",
            List.of("/usr/bin/g++", "-O2", "-std=c++17", "-pipe", "-o", "main", "main.cpp"),
            "main",
            List.of("./main")),
    JAVA("java", "Main.java",
            // 内部类会生成多个 .class 文件，打成一个 jar，缓存一个文件就够了
            // jar 默认把临时文件写在 /tmp 再移过来，跨文件系统的复制会被沙箱拦截，所以临时目录设为当前目录
            List.of("/bin/sh", "-c",
                    "/usr/bin/javac -J-Xmx256m -J-XX:+UseSerialGC -encoding UTF-8 Main.java"
                            + " && /usr/bin/jar -J-Djava.io.tmpdir=. cf main.jar *.class"),
            "main.jar",
            // 每回合都要启动一次 JVM，关掉分层编译的高级阶段，启动更快
            List.of("/usr/bin/java", "-Xmx256m", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1", "-cp", "main.jar", "Main")),
    PYTHON("python", "main.py",
            List.of("/usr/bin/python3", "-m", "py_compile", "main.py"), // Python 不用编译，只检查语法
            "main.py",
            List.of("/usr/bin/python3", "main.py")),
    GO("go", "main.go",
            // 先复制镜像里预编译好的标准库缓存（只读挂载，Go 要求缓存可写），这样只需编译用户代码
            List.of("/bin/sh", "-c", "cp -r /opt/gocache /tmp/gocache && /usr/bin/go build -o main main.go"),
            "main",
            List.of("./main"));

    private final String id; // 和前端、数据库里存的语言名一致
    private final String sourceFile;
    private final List<String> compileArgs;
    private final String executable; // 编译产物，缓存在 go-judge 里，每回合直接运行
    private final List<String> runArgs;

    public static Language fromId(String id) {
        for (Language language : values())
            if (language.id.equals(id))
                return language;
        return null;
    }
}
