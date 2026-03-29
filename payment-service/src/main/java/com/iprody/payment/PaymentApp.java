package com.iprody.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Start customer app class.
 */
@SpringBootApplication
public class PaymentApp {

    /**
     * Start.
     *
     * @param args - arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(PaymentApp.class, args);
        System.out.println("Payment service started!");
    }
}
