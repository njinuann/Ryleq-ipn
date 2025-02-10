package com.starise.ipn.config;

import com.starise.ipn.sms.ProcessAlert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class JobScheduller {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    private final ProcessAlert processAlert;

    @Autowired
    public JobScheduller(ProcessAlert processAlert) {
        this.processAlert = processAlert;
    }

    @Scheduled(initialDelay = 600, fixedRate = 30000)
    public void processMPESAPendingTransactions() {
        ExecutorService executorService = Executors.newFixedThreadPool(20);
        executorService.execute(() -> {
            System.out.println("Checking SMS balance Cron at " + dateFormat.format(new Date()));
            processAlert.sendBalanceAlert();
        });
    }
}
