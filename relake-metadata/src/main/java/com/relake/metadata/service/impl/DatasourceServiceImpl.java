package com.relake.metadata.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.relake.common.web.BusinessException;
import com.relake.common.web.ResultCode;
import com.relake.metadata.dto.DatasourceRequest;
import com.relake.metadata.dto.DatasourceVO;
import com.relake.metadata.entity.Datasource;
import com.relake.metadata.mapper.DatasourceMapper;
import com.relake.metadata.service.DatasourceService;
import com.relake.metadata.util.AesUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatasourceServiceImpl extends ServiceImpl<DatasourceMapper, Datasource>
        implements DatasourceService {

    private final AesUtil aesUtil;

    @Override
    public DatasourceVO create(DatasourceRequest request) {
        // 名称唯一性检查
        if (lambdaQuery().eq(Datasource::getName, request.getName()).count() > 0) {
            throw new BusinessException(ResultCode.DATASOURCE_ALREADY_EXISTS);
        }

        Datasource ds = new Datasource();
        BeanUtil.copyProperties(request, ds);
        ds.setPassword(aesUtil.encrypt(request.getPassword()));
        ds.setStatus("ACTIVE");

        save(ds);
        log.info("数据源创建成功: id={}, name={}", ds.getId(), ds.getName());
        return DatasourceVO.from(ds);
    }

    @Override
    public DatasourceVO update(Long id, DatasourceRequest request) {
        Datasource ds = super.getById(id);
        if (ds == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "数据源不存在");
        }

        // 名称冲突检查（排除自身）
        if (lambdaQuery().eq(Datasource::getName, request.getName()).ne(Datasource::getId, id).count() > 0) {
            throw new BusinessException(ResultCode.DATASOURCE_ALREADY_EXISTS);
        }

        BeanUtil.copyProperties(request, ds, "password");
        // 仅当传入新密码时才更新
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            ds.setPassword(aesUtil.encrypt(request.getPassword()));
        }
        ds.setId(id);

        updateById(ds);
        log.info("数据源更新成功: id={}", id);
        return DatasourceVO.from(ds);
    }

    @Override
    public void delete(Long id) {
        if (!removeById(id)) {
            throw new BusinessException(ResultCode.NOT_FOUND, "数据源不存在");
        }
        log.info("数据源删除成功: id={}", id);
    }

    @Override
    public DatasourceVO getById(Long id) {
        Datasource ds = super.getById(id);
        if (ds == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "数据源不存在");
        }
        return DatasourceVO.from(ds);
    }

    @Override
    public Datasource getEntity(Long id) {
        Datasource ds = super.getById(id);
        if (ds == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "数据源不存在");
        }
        return ds;
    }

    @Override
    public DatasourceVO getByName(String name) {
        Datasource ds = lambdaQuery().eq(Datasource::getName, name).one();
        if (ds == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "数据源不存在");
        }
        return DatasourceVO.from(ds);
    }

    @Override
    public Page<DatasourceVO> page(int page, int size, String keyword) {
        LambdaQueryWrapper<Datasource> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Datasource::getName, keyword)
                   .or().like(Datasource::getHost, keyword)
                   .or().like(Datasource::getDbName, keyword);
        }
        wrapper.orderByDesc(Datasource::getCreateTime);

        Page<Datasource> pg = page(new Page<>(page, size), wrapper);
        Page<DatasourceVO> voPage = new Page<>(pg.getCurrent(), pg.getSize(), pg.getTotal());
        voPage.setRecords(pg.getRecords().stream().map(DatasourceVO::from).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public List<DatasourceVO> listAll() {
        return lambdaQuery().eq(Datasource::getStatus, "ACTIVE")
                .orderByDesc(Datasource::getCreateTime)
                .list()
                .stream().map(DatasourceVO::from).collect(Collectors.toList());
    }

    @Override
    public boolean testConnection(Long id) {
        Datasource ds = super.getById(id);
        if (ds == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "数据源不存在");
        }

        String url = buildJdbcUrl(ds);
        String password = aesUtil.decrypt(ds.getPassword());
        try (Connection conn = DriverManager.getConnection(url, ds.getUsername(), password)) {
            boolean valid = conn.isValid(5);
            log.info("数据源连接测试成功: {}:{}", ds.getHost(), ds.getPort());
            return valid;
        } catch (Exception e) {
            log.error("数据源连接测试失败: {}:{}, error={}", ds.getHost(), ds.getPort(), e.getMessage());
            throw new BusinessException(ResultCode.DATASOURCE_CONNECT_FAILED, e.getMessage());
        }
    }

    private String buildJdbcUrl(Datasource ds) {
        String params = ds.getExtraParams() != null ? "?" + ds.getExtraParams() : "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
        return "jdbc:mysql://" + ds.getHost() + ":" + ds.getPort() + "/" + ds.getDbName() + params;
    }
}
