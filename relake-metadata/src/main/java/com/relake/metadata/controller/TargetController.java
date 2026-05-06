package com.relake.metadata.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.relake.common.web.R;
import com.relake.metadata.dto.TargetRequest;
import com.relake.metadata.dto.TargetVO;
import com.relake.metadata.service.TargetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/targets")
@RequiredArgsConstructor
public class TargetController {

    private final TargetService targetService;

    @PostMapping
    public R<TargetVO> create(@RequestBody TargetRequest request) {
        return R.ok(targetService.create(request), "目标存储创建成功");
    }

    @PutMapping("/{id}")
    public R<TargetVO> update(@PathVariable Long id, @RequestBody TargetRequest request) {
        return R.ok(targetService.update(id, request), "目标存储更新成功");
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        targetService.delete(id);
        return R.ok(null, "目标存储删除成功");
    }

    @GetMapping("/{id}")
    public R<TargetVO> getById(@PathVariable Long id) {
        return R.ok(targetService.getById(id));
    }

    @GetMapping
    public R<Page<TargetVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return R.ok(targetService.page(page, size, keyword));
    }

    @GetMapping("/list")
    public R<List<TargetVO>> listAll() {
        return R.ok(targetService.listAll());
    }

    @PostMapping("/{id}/test")
    public R<Boolean> testConnection(@PathVariable Long id) {
        boolean result = targetService.testConnection(id);
        return R.ok(result, "连接测试成功");
    }
}
