package com.relake.metadata.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.http.HttpUtil;
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
    public Page<TargetVO> page(int page, int size, String keyword) {
        LambdaQueryWrapper<Target> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Target::getName, keyword)
                   .or().like(Target::getEndpoint, keyword);
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

        try {
            String healthUrl = t.getEndpoint().replaceAll("/$", "") + "/minio/health/live";
            String result = HttpUtil.get(healthUrl, 5000);
            log.info("目标存储连接测试成功: {}, status={}", t.getEndpoint(), result);
            return true;
        } catch (Exception e) {
            log.error("目标存储连接测试失败: {}, error={}", t.getEndpoint(), e.getMessage());
            throw new BusinessException(ResultCode.DATASOURCE_CONNECT_FAILED, "MinIO连接失败: " + e.getMessage());
        }
    }
}
