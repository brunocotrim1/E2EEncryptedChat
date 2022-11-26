package org.psd.CloudPSD.controllers;


import lombok.extern.slf4j.Slf4j;
import org.psd.CloudPSD.models.Share;
import org.psd.CloudPSD.models.network.MessageDTO;
import org.psd.CloudPSD.models.network.ShareDTO;
import org.psd.CloudPSD.service.ShareService;
import org.psd.CloudPSD.service.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
public class ShareController {

    @Autowired
    ShareService shareService;
    @Autowired
    ValidationService validationService;

    @PostMapping("/share")
    public ResponseEntity<?> storeShare(@RequestBody ShareDTO shareDTO) {
        Share share = shareService.storeShare(shareDTO);
        if(share !=null)
            log.info("Share stored: " + share);
        return share == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(share.toShareDto());
    }

    @GetMapping("/share/{user1}")
    public ResponseEntity<?> getShare(@PathVariable String user1,@RequestHeader("Authorization") String bearerToken) {
        String user2 = validationService.verifyToken(bearerToken);
        if(user2 == null){
            log.info("USER UNVERIFIED");
            return ResponseEntity.badRequest().build();}
        Share share = shareService.getShareByUsers(user1,user2);
        if(share !=null)
            log.info("Share Retrieved: " + share);
        return share == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(share.toShareDto());
    }

    @DeleteMapping("/share/{user1}")
    public ResponseEntity<?> deleteShare(@PathVariable String user1,@RequestHeader("Authorization") String bearerToken) {
        String user2 = validationService.verifyToken(bearerToken);
        if(user2 == null)
            return ResponseEntity.badRequest().build();
        Share share = shareService.getShareByUsers(user1,user2);
        if(share !=null)
            log.info("Share Deleted: " + share);
        return share == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(share.toShareDto());
    }

    @PostMapping("/receive/message")
    public ResponseEntity<?> storeMessage(@RequestBody MessageDTO messageDTO,@RequestHeader("Authorization") String bearerToken) {

        String user = validationService.verifyToken(bearerToken);
        if(user == null)
            return ResponseEntity.badRequest().build();
        return shareService.storeMessage(messageDTO,user) ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
    @GetMapping("/messages/{user1}")
    public ResponseEntity<?> getMessage(@PathVariable String user1,@RequestHeader("Authorization") String bearerToken) {
        String user2 = validationService.verifyToken(bearerToken);
        if(user2 == null)
            return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(shareService.getMessages(user1,user2));
    }
}
