package com.iprody.customer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Start customer app class.
 */
@SpringBootApplication
public class CustomerApp {

    /**
     * Start.
     *
     * @param args - arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(CustomerApp.class, args);
        System.out.println("Customer service started!");
    }
}
