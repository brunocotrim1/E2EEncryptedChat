package org.psd.CloudPSD.controllers;

import org.psd.CloudPSD.models.network.MessageDTO;
import org.psd.CloudPSD.models.network.SseDTO;
import org.psd.CloudPSD.service.SSEService;
import org.psd.CloudPSD.service.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/sse")
public class SSEController {
    @Autowired
    ValidationService validationService;
    @Autowired
    SSEService sseService;

    @PostMapping("/reset")
    public ResponseEntity<?> storeMessage(@RequestHeader("Authorization") String bearerToken) {

        String user = validationService.verifyToken(bearerToken);
        if(user == null)
            return ResponseEntity.badRequest().build();
        sseService.deleteAll(user);
        return ResponseEntity.ok().build();
    }
    @PostMapping("/update")
    public ResponseEntity<?> updateMessage(@RequestBody SseDTO sseDTO, @RequestHeader("Authorization") String bearerToken) {

        String user = validationService.verifyToken(bearerToken);
        if(user == null)
            return ResponseEntity.badRequest().build();
        return sseService.update(sseDTO,user);
    }
    @PostMapping("/search")
    public ResponseEntity<?> searchMessage(@RequestBody SseDTO sseDTO, @RequestHeader("Authorization") String bearerToken) {
        String user = validationService.verifyToken(bearerToken);
        if(user == null)
            return ResponseEntity.badRequest().build();
        try {
            return sseService.search(sseDTO,user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
