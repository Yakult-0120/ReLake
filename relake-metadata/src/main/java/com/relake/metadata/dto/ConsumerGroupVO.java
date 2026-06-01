package com.relake.metadata.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerGroupVO {
    private String groupId;
    private String state;
    private int members;
    private int subscribedTopics;
    private int activeTopics;
    private long totalLag;
}
