package com.relake.metadata.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.relake.metadata.dto.TargetRequest;
import com.relake.metadata.dto.TargetVO;
import com.relake.metadata.entity.Target;

import java.util.List;

public interface TargetService extends IService<Target> {

    TargetVO create(TargetRequest request);

    TargetVO update(Long id, TargetRequest request);

    void delete(Long id);

    TargetVO getById(Long id);

    TargetVO getByName(String name);

    Page<TargetVO> page(int page, int size, String keyword);

    List<TargetVO> listAll();

    /** 获取完整实体（含加密 SecretKey），供内部调用使用 */
    Target getEntity(Long id);

    /** 测试目标存储连接是否可达 */
    boolean testConnection(Long id);
}
