package org.psd.server.ServerPSD.controllers;


import lombok.extern.slf4j.Slf4j;
import org.psd.server.ServerPSD.model.Share;
import org.psd.server.ServerPSD.model.network.ShareDTO;
import org.psd.server.ServerPSD.service.AccountService;
import org.psd.server.ServerPSD.service.ShareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
public class ShareController {

    @Autowired
    ShareService shareService;
    @Autowired
    AccountService accountService;
    @PostMapping("/share")
    public ResponseEntity<?> storeShare(@RequestBody ShareDTO shareDTO) {
        Share share = shareService.storeShare(shareDTO);
        if(share !=null)
            log.info("Share stored: " + share);
        return share == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(share.toShareDto());
    }

    @GetMapping("/share/{user1}")
    public ResponseEntity<?> getShare(@PathVariable String user1,@RequestHeader("Authorization") String bearerToken) {
        String user2 = accountService.verifyToken(bearerToken.substring(7, bearerToken.length()));
        if(user2 == null)
            return ResponseEntity.badRequest().build();
        Share share = shareService.getShareByUsers(user1,user2);
        if(share !=null)
            log.info("Share Retrieved: " + share);
        return share == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(share.toShareDto());
    }

    @DeleteMapping("/share/{user1}")
    public ResponseEntity<?> deleteShare(@PathVariable String user1,@RequestHeader("Authorization") String bearerToken) {
        String user2 = accountService.verifyToken(bearerToken.substring(7, bearerToken.length()));
        if(user2 == null)
            return ResponseEntity.badRequest().build();
        Share share = shareService.getShareByUsers(user1,user2);
        if(share !=null)
            log.info("Share Deleted: " + share);
        return share == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(share.toShareDto());
    }



}
