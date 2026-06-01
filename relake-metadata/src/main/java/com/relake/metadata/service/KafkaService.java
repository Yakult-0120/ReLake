package com.relake.metadata.service;

import com.relake.metadata.dto.*;

import java.util.List;

public interface KafkaService {

    // Topic
    List<TopicVO> listTopics(Long targetId);
    void createTopic(Long targetId, TopicRequest request);
    void deleteTopic(Long targetId, String topicName);

    // ACL
    List<AclVO> listAcls(Long targetId);
    void createAcl(Long targetId, AclRequest request);
    void deleteAcl(Long targetId, AclRequest request);
    void createScramUser(Long targetId, String username, String password);

    // Consumer Group
    List<ConsumerGroupVO> listConsumerGroups(Long targetId);
    ConsumerGroupDetailVO describeConsumerGroup(Long targetId, String groupId);
}
