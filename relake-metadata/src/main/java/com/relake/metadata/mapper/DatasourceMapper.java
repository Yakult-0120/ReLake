package com.relake.metadata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.relake.metadata.entity.Datasource;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DatasourceMapper extends BaseMapper<Datasource> {
}
