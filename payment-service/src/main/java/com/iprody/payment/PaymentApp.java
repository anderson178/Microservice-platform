package com.iprody.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PaymentApp {

    private PaymentApp() {
    }

    public static void main(final String[] args) {
        SpringApplication.run(PaymentApp.class, args);
    }
}
