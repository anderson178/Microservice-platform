package com.iprody.customer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CustomerApp {

    private CustomerApp() {
    }

    public static void main(final String[] args) {
        SpringApplication.run(CustomerApp.class, args);
        System.out.println("Customer service started!");
    }
}
