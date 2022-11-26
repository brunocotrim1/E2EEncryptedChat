package org.psd.ClientPSD.controller;

import org.psd.ClientPSD.configuration.Properties;
import org.psd.ClientPSD.model.Share;
import org.psd.ClientPSD.model.network.AddRequest;
import org.psd.ClientPSD.model.network.MessageDTO;
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
        return  cryptoService.addFriend(friend.getUsername()) ? ResponseEntity.ok("Friend added") : ResponseEntity.badRequest().build();
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

    @PostMapping("/send/message/{user2}")
    public ResponseEntity<?> sendMessage(@PathVariable String user2, @RequestBody String message) {

        if(user2 == null)
            return ResponseEntity.badRequest().build();
        MessageDTO messageDTO = cryptoService.sendMessage(message, user2);
        return messageDTO !=null ? ResponseEntity.ok("Message sent") : ResponseEntity.badRequest().build();
    }

    @PostMapping("/receive/message")
    public ResponseEntity<?> receiveMessage(@RequestBody MessageDTO messageDTO, @RequestHeader("Authorization") String bearerToken) {
        String user2 = cryptoService.authenticateUser(bearerToken);
        if(user2 == null)
            return ResponseEntity.badRequest().build();
        return  cryptoService.receiveMessage(messageDTO, user2) ? ResponseEntity.ok("Message saved successfully")
                : ResponseEntity.badRequest().body("Friend not added");
    }

    @GetMapping("/messages/{user2}")
    public ResponseEntity<?> getMessages(@PathVariable String user2) {
        if(user2 == null)
            return ResponseEntity.badRequest().build();
        return  ResponseEntity.ok(cryptoService.getMessages(user2));
    }

    @GetMapping("/messages")
    public ResponseEntity<?> getMessagesFromFriend(@RequestHeader("Authorization") String bearerToken) {
        String user2 = cryptoService.authenticateUser(bearerToken);
        if(user2 == null)
            return ResponseEntity.badRequest().build();
        return  ResponseEntity.ok(cryptoService.getMessagesDTO(user2));
    }

}
