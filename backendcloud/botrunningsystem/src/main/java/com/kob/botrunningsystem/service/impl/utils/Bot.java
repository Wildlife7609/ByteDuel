package com.kob.botrunningsystem.service.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Bot {
    Integer uerId;
    String botCode;
    String input;
}
