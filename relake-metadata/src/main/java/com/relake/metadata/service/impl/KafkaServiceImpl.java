package com.relake.metadata.service.impl;

import com.relake.common.web.BusinessException;
import com.relake.common.web.ResultCode;
import com.relake.metadata.dto.*;
import com.relake.metadata.entity.Target;
import com.relake.metadata.service.KafkaAdminClientFactory;
import com.relake.metadata.service.KafkaService;
import com.relake.metadata.service.TargetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.ScramCredentialInfo;
import org.apache.kafka.clients.admin.ScramMechanism;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.admin.UserScramCredentialAlteration;
import org.apache.kafka.clients.admin.UserScramCredentialUpsertion;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.acl.*;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourcePatternFilter;
import org.apache.kafka.common.resource.ResourceType;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaServiceImpl implements KafkaService {

    private final KafkaAdminClientFactory factory;
    private final TargetService targetService;

    // ==================== Topic ====================

    @Override
    public List<TopicVO> listTopics(Long targetId) {
        getAndValidateKafkaTarget(targetId);
        AdminClient client = factory.getOrCreate(targetId);
        try {
            Set<String> names = client.listTopics().names().get(10, TimeUnit.SECONDS);
            Map<String, TopicDescription> descs = client.describeTopics(names).all().get(15, TimeUnit.SECONDS);
            return descs.values().stream()
                    .map(d -> new TopicVO(
                            d.name(),
                            d.partitions().size(),
                            d.partitions().isEmpty() ? 0 : d.partitions().get(0).replicas().size(),
                            d.isInternal()))
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("listTopics failed: targetId={}", targetId, e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "获取Topic列表失败: " + e.getMessage());
        }
    }

    @Override
    public void createTopic(Long targetId, TopicRequest request) {
        getAndValidateKafkaTarget(targetId);
        AdminClient client = factory.getOrCreate(targetId);
        try {
            NewTopic nt = new NewTopic(request.getTopicName(), request.getNumPartitions(), request.getReplicationFactor());
            client.createTopics(List.of(nt)).all().get(15, TimeUnit.SECONDS);
            log.info("Topic created: targetId={}, topic={}", targetId, request.getTopicName());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("createTopic failed: targetId={}, topic={}", targetId, request.getTopicName(), e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "创建Topic失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteTopic(Long targetId, String topicName) {
        getAndValidateKafkaTarget(targetId);
        AdminClient client = factory.getOrCreate(targetId);
        try {
            client.deleteTopics(List.of(topicName)).all().get(15, TimeUnit.SECONDS);
            log.info("Topic deleted: targetId={}, topic={}", targetId, topicName);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("deleteTopic failed: targetId={}, topic={}", targetId, topicName, e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "删除Topic失败: " + e.getMessage());
        }
    }

    // ==================== ACL ====================

    @Override
    public List<AclVO> listAcls(Long targetId) {
        getAndValidateKafkaTarget(targetId);
        AdminClient client = factory.getOrCreate(targetId);
        try {
            return client.describeAcls(AclBindingFilter.ANY).values().get(15, TimeUnit.SECONDS)
                    .stream()
                    .map(b -> new AclVO(
                            b.entry().principal(),
                            b.pattern().resourceType().name(),
                            b.pattern().name(),
                            b.entry().operation().name(),
                            b.entry().permissionType().name(),
                            b.entry().host()))
                    .collect(Collectors.toList());
        } catch (ExecutionException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.toLowerCase().contains("authorizer") || msg.toLowerCase().contains("not configured")) {
                log.info("listAcls: ACL 未启用，返回空列表: targetId={}", targetId);
                return List.of();
            }
            log.error("listAcls failed: targetId={}", targetId, e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "获取ACL列表失败: " + msg);
        } catch (InterruptedException | TimeoutException e) {
            log.error("listAcls failed: targetId={}", targetId, e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "获取ACL列表失败: " + e.getMessage());
        }
    }

    @Override
    public void createAcl(Long targetId, AclRequest request) {
        getAndValidateKafkaTarget(targetId);
        AdminClient client = factory.getOrCreate(targetId);
        try {
            AccessControlEntry entry = new AccessControlEntry(
                    request.getPrincipal(), request.getHost(),
                    AclOperation.fromString(request.getOperation()),
                    AclPermissionType.fromString(request.getPermissionType()));
            ResourcePattern pattern = new ResourcePattern(
                    ResourceType.fromString(request.getResourceType()),
                    request.getResourceName(), PatternType.LITERAL);
            client.createAcls(List.of(new AclBinding(pattern, entry))).all().get(10, TimeUnit.SECONDS);
            log.info("ACL created: targetId={}, principal={}", targetId, request.getPrincipal());
        } catch (ExecutionException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.toLowerCase().contains("authorizer") || msg.toLowerCase().contains("not configured")) {
                throw new BusinessException(ResultCode.BAD_REQUEST,
                        "Kafka 未启用 ACL 授权，请在 broker 上配置 authorizer.class.name 后重试");
            }
            log.error("createAcl failed: targetId={}", targetId, e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "创建ACL失败: " + msg);
        } catch (InterruptedException | TimeoutException e) {
            log.error("createAcl failed: targetId={}", targetId, e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "创建ACL失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteAcl(Long targetId, AclRequest request) {
        getAndValidateKafkaTarget(targetId);
        AdminClient client = factory.getOrCreate(targetId);
        try {
            AccessControlEntryFilter entryFilter = new AccessControlEntryFilter(
                    request.getPrincipal(), request.getHost(),
                    AclOperation.fromString(request.getOperation()),
                    AclPermissionType.fromString(request.getPermissionType()));
            ResourcePatternFilter patternFilter = new ResourcePatternFilter(
                    ResourceType.fromString(request.getResourceType()),
                    request.getResourceName(), PatternType.LITERAL);
            client.deleteAcls(List.of(new AclBindingFilter(patternFilter, entryFilter)))
                    .all().get(10, TimeUnit.SECONDS);
            log.info("ACL deleted: targetId={}, principal={}", targetId, request.getPrincipal());
        } catch (ExecutionException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.toLowerCase().contains("authorizer") || msg.toLowerCase().contains("not configured")) {
                throw new BusinessException(ResultCode.BAD_REQUEST,
                        "Kafka 未启用 ACL 授权，请在 broker 上配置 authorizer.class.name 后重试");
            }
            log.error("deleteAcl failed: targetId={}", targetId, e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "删除ACL失败: " + msg);
        } catch (InterruptedException | TimeoutException e) {
            log.error("deleteAcl failed: targetId={}", targetId, e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "删除ACL失败: " + e.getMessage());
        }
    }

    @Override
    public void createScramUser(Long targetId, String username, String password) {
        getAndValidateKafkaTarget(targetId);
        AdminClient client = factory.getOrCreate(targetId);
        try {
            UserScramCredentialAlteration alteration = new UserScramCredentialUpsertion(
                    username,
                    new ScramCredentialInfo(ScramMechanism.SCRAM_SHA_256, 4096),
                    password);
            client.alterUserScramCredentials(List.of(alteration)).all().get(10, TimeUnit.SECONDS);
            log.info("SCRAM user created: targetId={}, username={}", targetId, username);
        } catch (ExecutionException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.toLowerCase().contains("not support") || msg.toLowerCase().contains("not configured") || msg.toLowerCase().contains("disabled")) {
                throw new BusinessException(ResultCode.BAD_REQUEST,
                        "Kafka 未启用 SCRAM 认证，请在 broker 上配置 SASL/SCRAM 后重试");
            }
            log.error("createScramUser failed: targetId={}, username={}", targetId, username, e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "创建SCRAM用户失败: " + msg);
        } catch (InterruptedException | TimeoutException e) {
            log.error("createScramUser failed: targetId={}, username={}", targetId, username, e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "创建SCRAM用户失败: " + e.getMessage());
        }
    }

    // ==================== Consumer Group ====================

    @Override
    public List<ConsumerGroupVO> listConsumerGroups(Long targetId) {
        getAndValidateKafkaTarget(targetId);
        AdminClient client = factory.getOrCreate(targetId);
        try {
            List<ConsumerGroupListing> groups = client.listConsumerGroups().all()
                    .get(10, TimeUnit.SECONDS).stream().toList();
            if (groups.isEmpty()) return List.of();

            List<String> groupIds = groups.stream().map(ConsumerGroupListing::groupId).toList();
            Map<String, ConsumerGroupDescription> descs = client.describeConsumerGroups(groupIds)
                    .all().get(15, TimeUnit.SECONDS);

            List<ConsumerGroupVO> results = new ArrayList<>();
            for (ConsumerGroupListing g : groups) {
                ConsumerGroupDescription desc = descs.get(g.groupId());
                if (desc == null) continue;

                Set<String> subscribedTopics = new HashSet<>();
                for (MemberDescription member : desc.members()) {
                    for (TopicPartition tp : member.assignment().topicPartitions()) {
                        subscribedTopics.add(tp.topic());
                    }
                }

                long totalLag = 0;
                try {
                    Map<TopicPartition, OffsetAndMetadata> offsets =
                            client.listConsumerGroupOffsets(g.groupId())
                                    .partitionsToOffsetAndMetadata().get(10, TimeUnit.SECONDS);
                    if (!offsets.isEmpty()) {
                        Map<TopicPartition, Long> tpToEndOffset = new HashMap<>();
                        Map<TopicPartition, OffsetSpec> lgSpecMap = new HashMap<>();
                        offsets.keySet().forEach(tp -> lgSpecMap.put(tp, OffsetSpec.latest()));
                        Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> endRes =
                                client.listOffsets(lgSpecMap).all().get(10, TimeUnit.SECONDS);
                        endRes.forEach((tp, info) -> tpToEndOffset.put(tp, info.offset()));
                        for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : offsets.entrySet()) {
                            long committed = entry.getValue().offset();
                            Long end = tpToEndOffset.get(entry.getKey());
                            if (end != null) {
                                totalLag += Math.max(0, end - committed);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to compute lag for group={}", g.groupId(), e);
                }

                results.add(new ConsumerGroupVO(
                        g.groupId(),
                        desc.state().toString(),
                        desc.members().size(),
                        subscribedTopics.size(),
                        subscribedTopics.size(), // active = subscribed for now
                        totalLag));
            }
            return results;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("listConsumerGroups failed: targetId={}", targetId, e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "获取消费者组列表失败: " + e.getMessage());
        }
    }

    @Override
    public ConsumerGroupDetailVO describeConsumerGroup(Long targetId, String groupId) {
        getAndValidateKafkaTarget(targetId);
        AdminClient client = factory.getOrCreate(targetId);
        try {
            Map<String, ConsumerGroupDescription> descs =
                    client.describeConsumerGroups(List.of(groupId)).all().get(10, TimeUnit.SECONDS);
            ConsumerGroupDescription desc = descs.get(groupId);
            if (desc == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "消费者组不存在: " + groupId);
            }

            Map<TopicPartition, OffsetAndMetadata> offsets =
                    client.listConsumerGroupOffsets(groupId)
                            .partitionsToOffsetAndMetadata().get(10, TimeUnit.SECONDS);

            Map<TopicPartition, Long> endOffsets = new HashMap<>();
            if (!offsets.isEmpty()) {
                Map<TopicPartition, OffsetSpec> specMap = new HashMap<>();
                offsets.keySet().forEach(tp -> specMap.put(tp, OffsetSpec.latest()));
                Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> endResult =
                        client.listOffsets(specMap).all().get(10, TimeUnit.SECONDS);
                endResult.forEach((tp, info) -> endOffsets.put(tp, info.offset()));
            }

            List<ConsumerGroupDetailVO.MemberInfo> memberInfos = new ArrayList<>();
            for (MemberDescription member : desc.members()) {
                List<ConsumerGroupDetailVO.PartitionOffset> partitionOffsets = new ArrayList<>();
                for (TopicPartition tp : member.assignment().topicPartitions()) {
                    long currentOffset = Optional.ofNullable(offsets.get(tp))
                            .map(OffsetAndMetadata::offset).orElse(0L);
                    long endOffset = endOffsets.getOrDefault(tp, 0L);
                    long lag = Math.max(0, endOffset - currentOffset);
                    partitionOffsets.add(new ConsumerGroupDetailVO.PartitionOffset(
                            tp.topic(), tp.partition(), currentOffset, endOffset, lag));
                }
                memberInfos.add(new ConsumerGroupDetailVO.MemberInfo(
                        member.consumerId(), member.clientId(), member.host(), partitionOffsets));
            }

            return new ConsumerGroupDetailVO(groupId, memberInfos);
        } catch (BusinessException be) {
            throw be;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("describeConsumerGroup failed: targetId={}, groupId={}", targetId, groupId, e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "获取消费者组详情失败: " + e.getMessage());
        }
    }

    private Target getAndValidateKafkaTarget(Long targetId) {
        Target target;
        try {
            target = targetService.getEntity(targetId);
        } catch (BusinessException e) {
            throw new BusinessException(ResultCode.NOT_FOUND, "目标存储不存在: targetId=" + targetId);
        }
        if (!"KAFKA".equalsIgnoreCase(target.getStorageType())) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "目标存储类型不是 KAFKA: targetId=" + targetId + ", type=" + target.getStorageType());
        }
        return target;
    }
}
