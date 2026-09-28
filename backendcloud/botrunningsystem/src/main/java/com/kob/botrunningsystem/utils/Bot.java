package com.kob.botrunningsystem.utils;

import java.util.function.Function;

// 用户 Bot 代码的模板，运行时不会用到这个类
// 用 JDK 自带的 Function 作为接口：打成 jar 后 javac 找不到项目里自定义的接口
public class Bot implements Function<String, Integer> {

    @Override
    public Integer apply(String input) {
        return 0;
    }
}
