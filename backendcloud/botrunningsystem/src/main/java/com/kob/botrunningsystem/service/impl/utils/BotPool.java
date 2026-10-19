package com.kob.botrunningsystem.service.impl.utils;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class BotPool extends Thread {
    @Autowired
    @Qualifier("backendClient") // there are two RestClients, so pick one by name
    private RestClient backendClient;
    @Autowired
    private Sandbox sandbox;

    private final BlockingQueue<BotTask> bots = new LinkedBlockingQueue<>();

    @PostConstruct // start the thread after injection, so backendClient is ready
    public void init() {
        start();
    }

    public void addBot(Integer userId, Language language, String botCode, String input) {
        bots.add(new BotTask(userId, language, botCode, input)); // thread-safe, returns immediately
    }

    private void consumeBot(BotTask botTask) {
        // a virtual thread waits for the sandbox result, so the boss thread is free to take the next task
        Thread.ofVirtual().start(() -> runBot(botTask));
    }

    private void runBot(BotTask botTask) {
        try {
            // The sandbox enforces the time limit. On timeout, runtime error or invalid output, send nothing; Game then rules it a loss on timeout
            String output = sandbox.run(botTask.getLanguage(), botTask.getBotCode(), botTask.getInput());
            Integer direction = parseDirection(output);
            if (direction == null)
                return;

            Map<String, Integer> data = new HashMap<>();
            data.put("user_id", botTask.getUserId());
            data.put("direction", direction);
            backendClient.post().uri("/pk/receive/bot/move/").body(data).retrieve().toBodilessEntity();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Integer parseDirection(String output) {
        if (output == null)
            return null;
        try {
            int direction = Integer.parseInt(output.trim());
            return direction >= 0 && direction < 4 ? direction : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                BotTask botTask = bots.take();
                consumeBot(botTask);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                e.printStackTrace(); // one bot failed; move on to the next
            }
        }
    }
}
