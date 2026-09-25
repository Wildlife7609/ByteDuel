package com.kob.matchingsystem.service.impl;

import com.kob.matchingsystem.service.MatchingService;
import com.kob.matchingsystem.service.impl.utils.MatchingPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MatchingServieImpl implements MatchingService {
    @Autowired
    private MatchingPool matchingPool;

    @Override
    public String addPlayer(Integer userId, Integer rating) {
        System.out.println("add Player: " + userId + " " + rating);
        matchingPool.addPlayer(userId, rating);
        return "add player success";
    }

    @Override
    public String removePlayer(Integer userId) {
        System.out.println("remove Player: " + userId);
        matchingPool.removePlayer(userId);
        return "remove player success";
    }
}
