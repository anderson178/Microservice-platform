package com.iprody.crm.payment.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "payment", produces = MediaType.APPLICATION_JSON_VALUE)
public class PaymentController {

    /**
     *  Simple hello.
     *
     *  @return - hello
     */
    @GetMapping
    public String hello() {
        return "Hello World";
    }
}
