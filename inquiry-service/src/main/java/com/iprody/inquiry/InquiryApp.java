package com.iprody.inquiry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Start inquiry app class.
 */
@EnableScheduling
@SpringBootApplication
public class InquiryApp {
    private InquiryApp() {
    }

    /**
     * Start.
     *
     * @param args - arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(InquiryApp.class, args);
        System.out.println("Payment service started! ");
    }
}
