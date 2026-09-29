package com.kob.matchingsystem.controller;

import com.kob.matchingsystem.service.MatchingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Objects;

@RestController
public class MatchingController {
    @Autowired
    private MatchingService matchingService;

    @PostMapping("/player/add/")
    public String addPlayer(@RequestBody Map<String, Integer> data) {
        Integer userId = Objects.requireNonNull(data.get("user_id"));
        Integer rating = Objects.requireNonNull(data.get("rating"));
        return matchingService.addPlayer(userId, rating);
    }

    @PostMapping("/player/remove/")
    public String removePlayer(@RequestBody Map<String, Integer> data) {
        Integer userId = Objects.requireNonNull(data.get("user_id"));
        return matchingService.removePlayer(userId);
    }
}
