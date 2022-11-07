package org.psd.server.ServerPSD.controllers;

import org.psd.server.ServerPSD.security.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MessageController {


    @GetMapping("/message")
    ResponseEntity<?> recieveMessage(@CurrentSecurityContext(expression = "authentication.principal") UserDetailsImpl userDetails) {
        return ResponseEntity.ok(userDetails);
    }


}
