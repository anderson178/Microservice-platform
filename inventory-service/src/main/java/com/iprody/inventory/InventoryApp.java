package com.iprody.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Start inventory app class.
 */
@SpringBootApplication
public class InventoryApp {
    private InventoryApp() {
    }

    /**
     * Start.
     *
     * @param args - arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(InventoryApp.class, args);
        System.out.println("Payment service started! ");
    }
}
