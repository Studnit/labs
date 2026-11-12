package com.studnit.labs;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping(value = "/", produces = "text/plain")
    public String hello() {
        return "labs is running";
    }
}
