package com.relake.integration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.relake.integration.entity.Task;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}
