package org.psd.ClientPSD.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.psd.ClientPSD.model.network.CreateGroup;
import org.psd.ClientPSD.model.network.MessageDTO;
import org.psd.ClientPSD.service.GroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class GroupController {

    private final GroupService groupService;

    @GetMapping("/create/topic")
    public ResponseEntity<?> getShare(@RequestBody CreateGroup group) {
        return groupService.createGroup(group);
    }

    @GetMapping("/subscribe/{topic}")
    public ResponseEntity<?> getShare(@PathVariable String topic) {
        log.info("Subscribe to topic: " + topic);
        return groupService.accessGroup(topic);
    }

    @PostMapping("/receive/group/message")
    public ResponseEntity<?> receiveMessage(@RequestBody MessageDTO messageDTO, @RequestHeader("Authorization") String bearerToken) {
        return groupService.receiveGroupMsg(messageDTO);
    }

    @GetMapping("/messages/group/{group}")
    public ResponseEntity<?> getMessages(@PathVariable String group) {
        return groupService.getGroupMessage(group);
    }

    @PostMapping("/send/message/group/{group}")
    public ResponseEntity<?> sendMessage(@PathVariable String group, @RequestBody String message) {
        if(group == null)
            return ResponseEntity.badRequest().build();
        return groupService.sendMessage(message,group);
    }


}
