package org.psd.server.ServerPSD.controllers;

import org.psd.server.ServerPSD.model.network.IBEKeySharing;
import org.psd.server.ServerPSD.service.AccountService;
import org.psd.server.ServerPSD.service.IBECypherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class IBEController {
    @Autowired
    IBECypherService ibeCypherService;
    @Autowired
    AccountService accountService;

    @GetMapping("/ibe/generate/secretKey")
    public ResponseEntity<IBEKeySharing> getIbeSecretKey(@RequestHeader("Authorization") String bearerToken) {
        String user = accountService.verifyToken(bearerToken.substring(7, bearerToken.length()));
        if(user == null)
            return ResponseEntity.badRequest().build();
        IBEKeySharing serializedKey = ibeCypherService.getSecretKey(user);
        return serializedKey == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(serializedKey);
    }
}
