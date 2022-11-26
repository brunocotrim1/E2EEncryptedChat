package org.psd.ClientPSD.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.psd.ClientPSD.model.network.CreateGroup;
import org.psd.ClientPSD.service.GroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

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
}
