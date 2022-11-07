package org.psd.ClientPSD.controller;

import org.psd.ClientPSD.configuration.Properties;
import org.psd.ClientPSD.model.Share;
import org.psd.ClientPSD.model.network.AddRequest;
import org.psd.ClientPSD.service.CryptoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class CryptoController {

    @Autowired
    CryptoService cryptoService;
    @Autowired
    Properties properties;

    @PostMapping("/share")
    public ResponseEntity<?> receiveShare(@RequestBody Share share, @RequestHeader("Authorization") String bearerToken) {
        return  cryptoService.receiveShare(share, bearerToken) ? ResponseEntity.ok("Share stored") : ResponseEntity.badRequest().build();
    }

    @PostMapping("/addFriend")
    public ResponseEntity<?> addFriend(@RequestBody AddRequest friend) {
        cryptoService.addFriend(friend.getUsername(),friend.getAddress());
        return  ResponseEntity.ok("Friend added");
    }

    @GetMapping("/share")
    public ResponseEntity<?> getShare(@RequestHeader("Authorization") String bearerToken) {
        String user2 = cryptoService.authenticateUser(bearerToken);
        if(user2 == null)
            return ResponseEntity.badRequest().build();
        Share share = cryptoService.getShare(properties.getUser(), user2);
        return share == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(share);
    }

    @GetMapping("/ibe/sessionKey")
    public ResponseEntity<?> getSessionKey(@RequestHeader("Authorization") String bearerToken) {
        String user2 = cryptoService.authenticateUser(bearerToken);
        if(user2 == null)
            return ResponseEntity.badRequest().build();
       String ibeSecretHeader = cryptoService.getSecretKey(user2);
        return ibeSecretHeader == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(ibeSecretHeader);
    }

}
