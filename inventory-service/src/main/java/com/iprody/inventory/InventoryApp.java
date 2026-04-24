package com.iprody.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class InventoryApp {

    private InventoryApp() {
    }

    public static void main(final String[] args) {
        SpringApplication.run(InventoryApp.class, args);
        System.out.println("Inventory service started!");
    }
}
