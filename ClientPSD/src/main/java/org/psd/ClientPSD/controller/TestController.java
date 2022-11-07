package org.psd.ClientPSD.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class TestController {

    @GetMapping("/main")
    public ResponseEntity<?> test() {
        return ResponseEntity.ok("test");
    }
}
