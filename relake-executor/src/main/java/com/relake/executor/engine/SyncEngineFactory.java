package com.relake.executor.engine;

import com.relake.common.web.BusinessException;
import com.relake.common.web.ResultCode;
import com.relake.executor.model.EngineType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 引擎工厂 — 根据引擎类型路由到对应实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SyncEngineFactory {

    private final List<SyncEngine> engines;
    private Map<EngineType, SyncEngine> engineMap = Collections.emptyMap();

    @PostConstruct
    public void init() {
        engineMap = new EnumMap<>(EngineType.class);
        for (SyncEngine engine : engines) {
            EngineType type = engine.getType();
            if (engineMap.containsKey(type)) {
                log.warn("引擎类型重复注册: {}，后注册的覆盖前者", type);
            }
            engineMap.put(type, engine);
            log.info("注册引擎: {} -> {}", type, engine.getClass().getSimpleName());
        }
    }

    /**
     * 根据引擎类型获取对应引擎实现
     *
     * @throws BusinessException 如果引擎类型不支持
     */
    public SyncEngine getEngine(EngineType type) {
        SyncEngine engine = engineMap.get(type);
        if (engine == null) {
            throw new BusinessException(ResultCode.ENGINE_NOT_SUPPORTED,
                    "不支持的采集引擎: " + type);
        }
        return engine;
    }

    /** 获取当前已注册的所有引擎类型 */
    public List<EngineType> getSupportedEngines() {
        return List.copyOf(engineMap.keySet());
    }
}
