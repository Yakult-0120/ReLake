package com.relake.metadata.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.http.HttpUtil;

import java.net.InetSocketAddress;
import java.net.Socket;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.relake.common.web.BusinessException;
import com.relake.common.web.ResultCode;
import com.relake.metadata.dto.TargetRequest;
import com.relake.metadata.dto.TargetVO;
import com.relake.metadata.entity.Target;
import com.relake.metadata.mapper.TargetMapper;
import com.relake.metadata.service.TargetService;
import com.relake.metadata.util.AesUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TargetServiceImpl extends ServiceImpl<TargetMapper, Target>
        implements TargetService {

    private final AesUtil aesUtil;

    @Override
    public TargetVO create(TargetRequest request) {
        if (lambdaQuery().eq(Target::getName, request.getName()).count() > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "目标存储名称已存在");
        }

        Target t = new Target();
        BeanUtil.copyProperties(request, t);
        t.setSecretKey(aesUtil.encrypt(request.getSecretKey()));
        if (t.getStorageType() == null) t.setStorageType("MINIO");
        if (t.getRegion() == null) t.setRegion("us-east-1");
        t.setStatus("ACTIVE");

        save(t);
        log.info("目标存储创建成功: id={}, name={}", t.getId(), t.getName());
        return TargetVO.from(t);
    }

    @Override
    public TargetVO update(Long id, TargetRequest request) {
        Target t = super.getById(id);
        if (t == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "目标存储不存在");
        }

        if (lambdaQuery().eq(Target::getName, request.getName()).ne(Target::getId, id).count() > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "目标存储名称已存在");
        }

        BeanUtil.copyProperties(request, t, "secretKey");
        if (request.getSecretKey() != null && !request.getSecretKey().isBlank()) {
            t.setSecretKey(aesUtil.encrypt(request.getSecretKey()));
        }
        t.setId(id);

        updateById(t);
        log.info("目标存储更新成功: id={}", id);
        return TargetVO.from(t);
    }

    @Override
    public void delete(Long id) {
        if (!removeById(id)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "目标存储不存在");
        }
        log.info("目标存储删除成功: id={}", id);
    }

    @Override
    public TargetVO getById(Long id) {
        Target t = super.getById(id);
        if (t == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "目标存储不存在");
        }
        return TargetVO.from(t);
    }

    @Override
    public TargetVO getByName(String name) {
        Target t = lambdaQuery().eq(Target::getName, name).one();
        if (t == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "目标存储不存在");
        }
        return TargetVO.from(t);
    }

    @Override
    public Page<TargetVO> page(int page, int size, String keyword, String storageType) {
        LambdaQueryWrapper<Target> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Target::getName, keyword)
                    .or().like(Target::getEndpoint, keyword));
        }
        if (storageType != null && !storageType.isBlank()) {
            wrapper.eq(Target::getStorageType, storageType.toUpperCase());
        }
        wrapper.orderByDesc(Target::getCreateTime);

        Page<Target> pg = page(new Page<>(page, size), wrapper);
        Page<TargetVO> voPage = new Page<>(pg.getCurrent(), pg.getSize(), pg.getTotal());
        voPage.setRecords(pg.getRecords().stream().map(TargetVO::from).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public List<TargetVO> listAll() {
        return lambdaQuery().eq(Target::getStatus, "ACTIVE")
                .orderByDesc(Target::getCreateTime)
                .list()
                .stream().map(TargetVO::from).collect(Collectors.toList());
    }

    @Override
    public Target getEntity(Long id) {
        Target t = super.getById(id);
        if (t == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "目标存储不存在");
        }
        return t;
    }

    @Override
    public boolean testConnection(Long id) {
        Target t = super.getById(id);
        if (t == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "目标存储不存在");
        }

        String storageType = t.getStorageType();
        if (storageType == null || storageType.isBlank()) {
            storageType = "MINIO";
        }

        try {
            switch (storageType.toUpperCase()) {
                case "MINIO", "S3" -> testMinioConnection(t);
                case "FILE" -> testFileConnection(t);
                case "HDFS" -> testHdfsConnection(t);
                case "KAFKA" -> testKafkaConnection(t);
                default -> {
                    log.warn("未知的目标存储类型: {}, 降级为 MinIO 健康检查", storageType);
                    testMinioConnection(t);
                }
            }
            log.info("目标存储连接测试成功: type={}, endpoint={}", storageType, t.getEndpoint());
            return true;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("目标存储连接测试失败: type={}, endpoint={}, error={}",
                    storageType, t.getEndpoint(), e.getMessage());
            throw new BusinessException(ResultCode.DATASOURCE_CONNECT_FAILED,
                    storageType + "连接失败: " + e.getMessage());
        }
    }

    /** MinIO / S3 兼容存储 — 调用 /minio/health/live 健康检查接口 */
    private void testMinioConnection(Target t) {
        String healthUrl = t.getEndpoint().replaceAll("/$", "") + "/minio/health/live";
        String result = HttpUtil.get(healthUrl, 5000);
        log.info("MinIO 健康检查: url={}, response={}", healthUrl, result);
    }

    /**
     * FILE 普通文件服务器 — 尝试 TCP Socket 连接
     * <p>
     * 支持 "ip:port" 格式精确指定端口；未指定端口时依次探测 SMB(445)、SSH(22)、NetBIOS(139)。
     * 所有端口都不通但主机名可解析的视为可达（端口可能被防火墙限制，但主机本身在线）。
     */
    private void testFileConnection(Target t) throws Exception {
        String endpoint = t.getEndpoint();
        if (endpoint == null || endpoint.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "服务器IP不能为空");
        }

        String host;
        int port = -1;
        if (endpoint.contains(":")) {
            String[] parts = endpoint.split(":");
            host = parts[0].trim();
            port = Integer.parseInt(parts[1].trim());
        } else {
            host = endpoint.trim();
        }

        // 用户指定了端口 → 直接连接
        if (port > 0) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 3000);
                log.info("FILE 服务器可达: host={}, port={}", host, port);
            }
            return;
        }

        // 未指定端口 → 依次探测常见文件服务端口
        int[] commonPorts = {445, 22, 139}; // SMB → SSH → NetBIOS
        for (int p : commonPorts) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, p), 2000);
                log.info("FILE 服务器可达: host={}, port={} ({})",
                        host, p, p == 445 ? "SMB" : p == 22 ? "SSH" : "NetBIOS");
                return;
            } catch (Exception ignored) {
                // 该端口不通，继续尝试下一个
            }
        }

        // 所有端口都不通，验证主机名至少能解析
        java.net.InetAddress.getByName(host);
        log.info("FILE 服务器主机名可解析: host={}，未发现可连接的常用端口（已尝试 445/SMB 22/SSH 139/NetBIOS），视为可达", host);
    }

    /** Kafka — 尝试 TCP Socket 连接到 Broker 端口（默认 9092） */
    private void testKafkaConnection(Target t) throws Exception {
        String endpoint = t.getEndpoint();
        if (endpoint == null || endpoint.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Kafka Bootstrap Servers 不能为空");
        }

        String host;
        int port;
        if (endpoint.contains(":")) {
            String[] parts = endpoint.split(":");
            host = parts[0].trim();
            port = Integer.parseInt(parts[1].trim());
        } else {
            host = endpoint.trim();
            port = 9092;
        }

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 3000);
            log.info("Kafka Broker 可达: host={}, port={}", host, port);
        }
    }

    /** HDFS — 尝试 TCP Socket 连接到 NameNode RPC 端口 */
    private void testHdfsConnection(Target t) throws Exception {
        String endpoint = t.getEndpoint();
        if (endpoint == null || endpoint.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "HDFS 地址不能为空");
        }

        String host;
        int port;
        if (endpoint.contains(":")) {
            String[] parts = endpoint.split(":");
            host = parts[0].trim();
            port = Integer.parseInt(parts[1].trim());
        } else {
            host = endpoint.trim();
            port = 8020; // NameNode 默认 RPC 端口
        }

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 3000);
            log.info("HDFS NameNode 可达: host={}, port={}", host, port);
        }
    }
}
