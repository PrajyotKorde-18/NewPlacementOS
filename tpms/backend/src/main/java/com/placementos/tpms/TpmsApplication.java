package com.placementos.tpms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TpmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(TpmsApplication.class, args);
    }
}
