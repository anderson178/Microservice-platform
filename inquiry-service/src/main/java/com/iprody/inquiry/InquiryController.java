package com.iprody.inquiry;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "inquiry", produces = MediaType.APPLICATION_JSON_VALUE)
public class InquiryController {

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
