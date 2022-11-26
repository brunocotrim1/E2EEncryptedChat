package org.psd.server.ServerPSD.controllers;

import cn.edu.buaa.crypto.access.parser.PolicySyntaxException;
import lombok.RequiredArgsConstructor;
import org.psd.server.ServerPSD.model.network.CreateGroup;
import org.psd.server.ServerPSD.service.AccountService;
import org.psd.server.ServerPSD.service.TopicService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TopicController {
    private final AccountService accountService;

    private final TopicService topicService;

    @GetMapping("/subscribe/{topic}")
    public ResponseEntity<?> subscribeTopic(@PathVariable String topic, @RequestHeader("Authorization") String bearerToken) {
        String user = accountService.verifyToken(bearerToken.substring(7, bearerToken.length()));
        try {
            return topicService.accessGroup(user,topic);
        } catch (PolicySyntaxException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/create/topic")
    public ResponseEntity<?> createTopic(@RequestBody CreateGroup group, @RequestHeader("Authorization") String bearerToken){
        String user = accountService.verifyToken(bearerToken.substring(7, bearerToken.length()));
        if(user == null)
            return ResponseEntity.badRequest().build();
        return topicService.createGroup(group);
    }

}
