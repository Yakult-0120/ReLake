package com.relake.metadata.service;

import com.relake.common.web.BusinessException;
import com.relake.common.web.ResultCode;
import com.relake.metadata.entity.Target;
import com.relake.metadata.util.AesUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaAdminClientFactory {

    private final ConcurrentHashMap<Long, AdminClient> clients = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> cachedEndpoints = new ConcurrentHashMap<>();
    private final TargetService targetService;
    private final AesUtil aesUtil;

    public AdminClient getOrCreate(Long targetId) {
        Target target = targetService.getEntity(targetId);
        String currentEndpoint = target.getEndpoint();
        if (currentEndpoint == null || currentEndpoint.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Kafka endpoint 未配置: targetId=" + targetId);
        }
        String cachedEndpoint = cachedEndpoints.get(targetId);

        // 如果 endpoint 变了（用户修改了配置），重建 AdminClient
        if (cachedEndpoint != null && cachedEndpoint.equals(currentEndpoint)) {
            AdminClient existing = clients.get(targetId);
            if (existing != null) {
                return existing;
            }
        }

        // endpoint 变更或首次创建 — 先关闭旧连接，再创建新连接
        evict(targetId);
        Properties props = buildProperties(target);
        AdminClient client = AdminClient.create(props);
        clients.put(targetId, client);
        cachedEndpoints.put(targetId, currentEndpoint);
        log.info("创建 Kafka AdminClient: targetId={}, bootstrapServers={}", targetId, currentEndpoint);
        return client;
    }

    public void evict(Long targetId) {
        cachedEndpoints.remove(targetId);
        AdminClient old = clients.remove(targetId);
        if (old != null) {
            log.info("关闭 Kafka AdminClient: targetId={}", targetId);
            old.close(Duration.ofSeconds(5));
        }
    }

    private Properties buildProperties(Target target) {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, target.getEndpoint());
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 10000);

        String accessKey = target.getAccessKey();
        String secretKey = target.getSecretKey();
        if (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank()) {
            props.put("security.protocol", "SASL_PLAINTEXT");
            props.put("sasl.mechanism", "PLAIN");
            String decryptedPwd = aesUtil.decrypt(secretKey);
            String jaasTemplate = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";";
            props.put("sasl.jaas.config", String.format(jaasTemplate, accessKey, decryptedPwd));
        }

        return props;
    }
}
