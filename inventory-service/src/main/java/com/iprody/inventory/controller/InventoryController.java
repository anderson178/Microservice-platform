package com.iprody.inventory.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "inventory", produces = MediaType.APPLICATION_JSON_VALUE)
public class InventoryController {

    @GetMapping
    public String hello() {
        return "Hello World";
    }
}
