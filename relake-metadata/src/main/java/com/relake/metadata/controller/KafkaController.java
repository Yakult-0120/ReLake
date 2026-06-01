package com.relake.metadata.controller;

import com.relake.common.web.R;
import com.relake.metadata.dto.*;
import com.relake.metadata.service.KafkaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/kafka")
@RequiredArgsConstructor
public class KafkaController {

    private final KafkaService kafkaService;

    // ==================== Topic ====================

    @GetMapping("/{targetId}/topics")
    public R<List<TopicVO>> listTopics(@PathVariable Long targetId) {
        return R.ok(kafkaService.listTopics(targetId));
    }

    @PostMapping("/{targetId}/topics")
    public R<Void> createTopic(@PathVariable Long targetId, @RequestBody TopicRequest request) {
        kafkaService.createTopic(targetId, request);
        return R.ok(null, "Topic created");
    }

    @DeleteMapping("/{targetId}/topics/{topicName}")
    public R<Void> deleteTopic(@PathVariable Long targetId, @PathVariable String topicName) {
        kafkaService.deleteTopic(targetId, topicName);
        return R.ok(null, "Topic deleted");
    }

    // ==================== ACL ====================

    @GetMapping("/{targetId}/acls")
    public R<List<AclVO>> listAcls(@PathVariable Long targetId) {
        return R.ok(kafkaService.listAcls(targetId));
    }

    @PostMapping("/{targetId}/acls")
    public R<Void> createAcl(@PathVariable Long targetId, @RequestBody AclRequest request) {
        kafkaService.createAcl(targetId, request);
        return R.ok(null, "ACL created");
    }

    @DeleteMapping("/{targetId}/acls")
    public R<Void> deleteAcl(@PathVariable Long targetId, @RequestBody AclRequest request) {
        kafkaService.deleteAcl(targetId, request);
        return R.ok(null, "ACL deleted");
    }

    @PostMapping("/{targetId}/scram-users")
    public R<Void> createScramUser(@PathVariable Long targetId, @RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        kafkaService.createScramUser(targetId, username, password);
        return R.ok(null, "SCRAM user created");
    }

    // ==================== Consumer Group ====================

    @GetMapping("/{targetId}/consumer-groups")
    public R<List<ConsumerGroupVO>> listConsumerGroups(@PathVariable Long targetId) {
        return R.ok(kafkaService.listConsumerGroups(targetId));
    }

    @GetMapping("/{targetId}/consumer-groups/{groupId}")
    public R<ConsumerGroupDetailVO> describeConsumerGroup(
            @PathVariable Long targetId, @PathVariable String groupId) {
        return R.ok(kafkaService.describeConsumerGroup(targetId, groupId));
    }
}
