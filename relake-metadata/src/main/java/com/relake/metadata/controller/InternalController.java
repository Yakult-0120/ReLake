package com.relake.metadata.controller;

import com.relake.common.web.BusinessException;
import com.relake.common.web.R;
import com.relake.common.web.ResultCode;
import com.relake.metadata.dto.DatasourceInternalVO;
import com.relake.metadata.dto.TargetInternalVO;
import com.relake.metadata.entity.Datasource;
import com.relake.metadata.entity.Target;
import com.relake.metadata.service.DatasourceService;
import com.relake.metadata.service.TargetService;
import com.relake.metadata.util.AesUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 内部服务端点 — 供 Integration 服务调用，返回解密后的凭据
 * <p>
 * 路径前缀 /internal/ 不在 Gateway 路由白名单中，仅服务间 Feign 调用可达。
 */
@Slf4j
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {

    private final DatasourceService datasourceService;
    private final TargetService targetService;
    private final AesUtil aesUtil;

    /**
     * 获取数据源完整信息（含解密密码）
     */
    @GetMapping("/datasources/{id}")
    public R<DatasourceInternalVO> getDatasource(@PathVariable Long id) {
        Datasource ds = datasourceService.getEntity(id);
        DatasourceInternalVO vo = new DatasourceInternalVO();
        vo.setId(ds.getId());
        vo.setName(ds.getName());
        vo.setDbType(ds.getDbType());
        vo.setHost(ds.getHost());
        vo.setPort(ds.getPort());
        vo.setDbName(ds.getDbName());
        vo.setUsername(ds.getUsername());
        vo.setPassword(aesUtil.decrypt(ds.getPassword()));
        vo.setExtraParams(ds.getExtraParams());
        vo.setStatus(ds.getStatus());
        vo.setDescription(ds.getDescription());
        vo.setCreateTime(ds.getCreateTime());
        vo.setUpdateTime(ds.getUpdateTime());
        log.info("内部调用: 获取数据源详情 datasourceId={}", id);
        return R.ok(vo);
    }

    /**
     * 获取目标存储完整信息（含解密 SecretKey）
     */
    @GetMapping("/targets/{id}")
    public R<TargetInternalVO> getTarget(@PathVariable Long id) {
        Target t = targetService.getEntity(id);
        TargetInternalVO vo = new TargetInternalVO();
        vo.setId(t.getId());
        vo.setName(t.getName());
        vo.setStorageType(t.getStorageType());
        vo.setEndpoint(t.getEndpoint());
        vo.setAccessKey(t.getAccessKey());
        vo.setSecretKey(aesUtil.decrypt(t.getSecretKey()));
        vo.setBucket(t.getBucket());
        vo.setRegion(t.getRegion());
        vo.setPaimonWarehouse(t.getPaimonWarehouse());
        vo.setStatus(t.getStatus());
        vo.setDescription(t.getDescription());
        vo.setCreateTime(t.getCreateTime());
        vo.setUpdateTime(t.getUpdateTime());
        log.info("内部调用: 获取目标存储详情 targetId={}", id);
        return R.ok(vo);
    }
}
