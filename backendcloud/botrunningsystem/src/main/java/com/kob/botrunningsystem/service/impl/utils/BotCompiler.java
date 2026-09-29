package com.kob.botrunningsystem.service.impl.utils;

import org.joor.Reflect;

import java.lang.reflect.Modifier;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

// 编译 Bot 代码，对局（Consumer）和保存前的检查（/bot/compile/）共用同一份缓存
public class BotCompiler {
    // 编译缓存：key 是 Bot 代码，value 是编译好的类。同一份代码只编译一次，之后每回合直接创建对象
    private static final ConcurrentHashMap<String, Class<?>> compiledBots = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 1000; // 缓存的类不会被回收，超过上限就清空，防止内存一直增长

    public static Class<?> getBotClass(String botCode) {
        if (compiledBots.size() >= MAX_CACHE_SIZE)
            compiledBots.clear();
        // 同一份代码被多个线程同时请求时，只有一个线程编译，其他线程等它编译完直接用
        return compiledBots.computeIfAbsent(botCode, code -> {
            // 类名加上随机后缀，避免和模板类 utils.Bot 同名
            // 替换所有独立的 Bot 单词，构造函数 Bot()、new Bot() 等引用也要一起改名
            String uid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            String renamed = code.replaceAll("\\bBot\\b", "Bot" + uid);
            try {
                return Reflect.compile("com.kob.botrunningsystem.utils.Bot" + uid, renamed).type();
            } catch (RuntimeException e) {
                // 把报错里的随机类名还原成用户写的 Bot，否则用户看不懂
                String message = String.valueOf(e.getMessage())
                        .replace("/com/kob/botrunningsystem/utils/Bot" + uid + ".java", "Bot.java")
                        .replace("Bot" + uid, "Bot");
                throw new IllegalArgumentException(message, e);
            }
        });
    }

    // 保存前检查：能编译，且符合约定。通过返回 null，否则返回给用户看的错误信息
    public static String check(String botCode) {
        Class<?> botClass;
        try {
            botClass = getBotClass(botCode);
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        } catch (RuntimeException e) {
            return "编译失败：" + e.getMessage();
        }
        if (!Function.class.isAssignableFrom(botClass))
            return "Bot 必须实现 Function<String, Integer>";
        // Consumer 在另一个包里创建对象，类必须是 public
        if (!Modifier.isPublic(botClass.getModifiers()))
            return "Bot 类必须是 public 的";
        try {
            botClass.getConstructor(); // 只会找 public 的构造函数，找不到就抛异常
        } catch (NoSuchMethodException e) {
            return "Bot 必须有 public 的无参构造函数";
        }
        return null;
    }
}
