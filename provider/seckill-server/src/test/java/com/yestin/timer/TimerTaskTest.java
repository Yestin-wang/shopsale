package com.yestin.timer;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TimerTaskTest {
    @Test
    public void test() throws InterruptedException {
        TimerTask timerTask = new TimerTask() {
            int i = 0;
            @Override
            public void run() {
                System.out.println(LocalDateTime.now());

                if (i++ == 2) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        ScheduledExecutorService service = Executors.newScheduledThreadPool(10);
        service.scheduleAtFixedRate(timerTask, 0, 2, TimeUnit.SECONDS);
    }
}
