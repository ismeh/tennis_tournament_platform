package com.tfm.tennis_platform.domain.models;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class TennisTaskScheduler {
    private static final Logger log = LoggerFactory.getLogger(TennisTaskScheduler.class);

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static final int MILLI_SECONDS_RATE = 60000;

    @Scheduled(fixedRate = MILLI_SECONDS_RATE)
    public void reportCurrentTime() {
        log.info("The time is now {}", DATE_FORMAT.format(new Date()));
    }
}
