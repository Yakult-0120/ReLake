package com.relake.metadata.dto;

import lombok.Data;

@Data
public class TopicRequest {
    private String topicName;
    private int numPartitions = 1;
    private short replicationFactor = 1;
}
