package com.kob.botrunningsystem.service.impl.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

// How each language is compiled and run. Commands run in the go-judge sandbox with /w as the working directory
// Protocol: the program reads one line of game state from stdin and prints a direction 0-3 to stdout
@Getter
@AllArgsConstructor
public enum Language {
    CPP("cpp", "main.cpp",
            List.of("/usr/bin/g++", "-O2", "-std=c++17", "-pipe", "-o", "main", "main.cpp"),
            "main",
            List.of("./main")),
    JAVA("java", "Main.java",
            // inner classes produce several .class files; pack them into one jar so only one file is cached
            // jar writes temp files to /tmp and then moves them, and the sandbox blocks cross-filesystem moves, so use the working directory for temp files
            List.of("/bin/sh", "-c",
                    "/usr/bin/javac -J-Xmx256m -J-XX:+UseSerialGC -encoding UTF-8 Main.java"
                            + " && /usr/bin/jar -J-Djava.io.tmpdir=. cf main.jar *.class"),
            "main.jar",
            // a JVM starts every turn; stopping tiered compilation at level 1 makes startup faster
            List.of("/usr/bin/java", "-Xmx256m", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1", "-cp", "main.jar", "Main")),
    PYTHON("python", "main.py",
            List.of("/usr/bin/python3", "-m", "py_compile", "main.py"), // Python is not compiled; only check the syntax
            "main.py",
            List.of("/usr/bin/python3", "main.py")),
    GO("go", "main.go",
            // copy the precompiled standard library cache from the image first (it is mounted read-only, and Go needs a writable cache), so only the user's code is compiled
            List.of("/bin/sh", "-c", "cp -r /opt/gocache /tmp/gocache && /usr/bin/go build -o main main.go"),
            "main",
            List.of("./main"));

    private final String id; // matches the language name used by the frontend and the database
    private final String sourceFile;
    private final List<String> compileArgs;
    private final String executable; // compiled output, cached in go-judge and run directly each turn
    private final List<String> runArgs;

    public static Language fromId(String id) {
        for (Language language : values())
            if (language.id.equals(id))
                return language;
        return null;
    }
}
