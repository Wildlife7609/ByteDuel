package com.kob.matchingsystem;

import com.kob.matchingsystem.service.impl.MatchingServieImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
@SpringBootApplication
public class MatchingSystemApplication {
    static void main(String[] args) {
        MatchingServieImpl.matchingPool.start(); // Start the matching pool thread
        SpringApplication.run(MatchingSystemApplication.class, args);
    }
}
