package com.relake.metadata.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.relake.metadata.dto.DatasourceRequest;
import com.relake.metadata.dto.DatasourceVO;
import com.relake.metadata.entity.Datasource;

import java.util.List;

public interface DatasourceService extends IService<Datasource> {

    DatasourceVO create(DatasourceRequest request);

    DatasourceVO update(Long id, DatasourceRequest request);

    void delete(Long id);

    DatasourceVO getById(Long id);

    DatasourceVO getByName(String name);

    Page<DatasourceVO> page(int page, int size, String keyword);

    List<DatasourceVO> listAll();

    /** 获取完整实体（含加密密码），供内部 Schema 发现等场景使用 */
    Datasource getEntity(Long id);

    /** 测试数据源连接是否可达 */
    boolean testConnection(Long id);
}
