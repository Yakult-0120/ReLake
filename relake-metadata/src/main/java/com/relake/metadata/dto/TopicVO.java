package com.relake.metadata.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopicVO {
    private String name;
    private int partitions;
    private int replicationFactor;
    private boolean internal;
}
