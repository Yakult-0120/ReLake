package com.relake.integration.orchestration;

import com.relake.integration.model.TaskStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * 任务状态机 — 定义并校验合法的状态转换
 * <pre>
 * DRAFT → VALIDATING → READY → RUNNING → STOPPED
 *   ↓        ↓          ↓         ↓
 *   └────────┴──────────┴──→ FAILED
 * </pre>
 */
@Slf4j
@Component
public class TaskStateMachine {

    private static final Map<TaskStatus, Set<TaskStatus>> TRANSITIONS = Map.of(
            TaskStatus.DRAFT, Set.of(TaskStatus.VALIDATING, TaskStatus.FAILED),
            TaskStatus.VALIDATING, Set.of(TaskStatus.READY, TaskStatus.FAILED),
            TaskStatus.READY, Set.of(TaskStatus.RUNNING, TaskStatus.FAILED),
            TaskStatus.RUNNING, Set.of(TaskStatus.FAILED, TaskStatus.STOPPED),
            TaskStatus.FAILED, Set.of(TaskStatus.VALIDATING),      // 重试校验
            TaskStatus.STOPPED, Set.of()
    );

    /**
     * 判断是否可以从当前状态转换到目标状态
     */
    public boolean canTransition(TaskStatus current, TaskStatus target) {
        if (current == null) return false;
        Set<TaskStatus> allowed = TRANSITIONS.getOrDefault(current, Set.of());
        boolean allowed1 = allowed.contains(target);
        if (!allowed1) {
            log.warn("状态转换被拒绝: {} -> {}", current, target);
        }
        return allowed1;
    }

    /**
     * 执行状态转换，非法转换抛异常
     */
    public void transition(TaskStatus current, TaskStatus target) {
        if (!canTransition(current, target)) {
            throw new IllegalStateException(
                    String.format("非法状态转换: %s -> %s", current, target));
        }
        log.info("状态转换: {} -> {}", current, target);
    }
}
