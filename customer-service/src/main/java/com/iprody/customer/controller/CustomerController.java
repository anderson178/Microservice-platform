package com.iprody.customer.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "customer", produces = MediaType.APPLICATION_JSON_VALUE)
public class CustomerController {

    @GetMapping
    public String hello() {
        return "Hello World";
    }
}
