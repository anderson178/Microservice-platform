package com.iprody.inquiry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Start inquiry app class.
 */
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
