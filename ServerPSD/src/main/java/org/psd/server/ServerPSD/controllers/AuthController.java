package org.psd.server.ServerPSD.controllers;

import lombok.extern.slf4j.Slf4j;
import org.psd.server.ServerPSD.model.network.SignupRequest;
import org.psd.server.ServerPSD.model.network.SignupResponse;
import org.psd.server.ServerPSD.service.AccountService;
import org.psd.server.ServerPSD.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/auth")
@Slf4j
class AuthController {

    @Autowired
    UserService userService;

    @Autowired
    AccountService accountService;

    @PostMapping("/register")
    ResponseEntity<? > registration(@RequestBody SignupRequest request) {
        accountService.register(request.getUsername(), request.getPassword(),request.getAddress());
        log.info("User registered: " + request.getUsername());
        return ResponseEntity.ok("User registered Successfully");
    }

    @PostMapping("/login")
    ResponseEntity<? > login(@RequestBody SignupRequest request) throws IOException {
        return ResponseEntity.ok(accountService.signIn(request.getUsername(), request.getPassword()));
    }

    @GetMapping("/refresh/{token}")
    ResponseEntity<? > refresh(@PathVariable String token) throws IOException {
        SignupResponse response = accountService.refreshToken(token);
        return response ==null ? ResponseEntity.badRequest().body("Invalid token") : ResponseEntity.ok(response);
    }

    @GetMapping("/verify/{token}")
    ResponseEntity<? > verify(@PathVariable String token) throws IOException {
        String username= accountService.verifyToken(token);
        return !username.isEmpty() ? ResponseEntity.ok(username) : ResponseEntity.badRequest().body("Invalid token");
    }

    @GetMapping("/address/{id}")
    ResponseEntity<? > getAddress(@PathVariable String id) throws IOException {
        String address= accountService.getAddress(id);
        return address != null? ResponseEntity.ok(address) : ResponseEntity.notFound().build();
    }

}