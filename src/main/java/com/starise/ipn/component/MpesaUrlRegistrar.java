package com.starise.ipn.component;

import com.starise.ipn.service.MpesaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class MpesaUrlRegistrar implements CommandLineRunner {

    @Autowired
    private MpesaService mpesaService;

    @Override
    public void run(String... args) throws Exception {
        mpesaService.registerUrl();
    }
}

