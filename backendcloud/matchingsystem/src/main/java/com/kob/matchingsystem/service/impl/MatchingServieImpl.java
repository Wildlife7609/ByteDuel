package com.kob.matchingsystem.service.impl;

import com.kob.matchingsystem.service.MatchingService;
import org.springframework.stereotype.Service;

@Service
public class MatchingServieImpl implements MatchingService {
    @Override
    public String addPlayer(Integer userId, Integer rating) {
        System.out.println("add Player: " + userId + " " + rating );
        return "add player success";
    }

    @Override
    public String removePlayer(Integer userId) {
        System.out.println("remove Player: " + userId );
        return "remove player success";
    }
}
