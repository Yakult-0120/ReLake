package com.relake.metadata.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsumerGroupDetailVO {
    private String groupId;
    private List<MemberInfo> members;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberInfo {
        private String memberId;
        private String clientId;
        private String host;
        private List<PartitionOffset> partitions;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartitionOffset {
        private String topic;
        private int partition;
        private long currentOffset;
        private long endOffset;
        private long lag;
    }
}
