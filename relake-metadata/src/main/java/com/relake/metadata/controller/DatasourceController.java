package com.relake.metadata.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.relake.common.web.R;
import com.relake.metadata.dto.DatasourceRequest;
import com.relake.metadata.dto.DatasourceVO;
import com.relake.metadata.service.DatasourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/datasources")
@RequiredArgsConstructor
public class DatasourceController {

    private final DatasourceService datasourceService;

    @PostMapping
    public R<DatasourceVO> create(@RequestBody DatasourceRequest request) {
        return R.ok(datasourceService.create(request), "数据源创建成功");
    }

    @PutMapping("/{id}")
    public R<DatasourceVO> update(@PathVariable Long id, @RequestBody DatasourceRequest request) {
        return R.ok(datasourceService.update(id, request), "数据源更新成功");
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        datasourceService.delete(id);
        return R.ok(null, "数据源删除成功");
    }

    @GetMapping("/{id}")
    public R<DatasourceVO> getById(@PathVariable Long id) {
        return R.ok(datasourceService.getById(id));
    }

    @GetMapping
    public R<Page<DatasourceVO>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return R.ok(datasourceService.page(page, size, keyword));
    }

    @GetMapping("/list")
    public R<List<DatasourceVO>> listAll() {
        return R.ok(datasourceService.listAll());
    }

    @PostMapping("/{id}/test")
    public R<Boolean> testConnection(@PathVariable Long id) {
        boolean result = datasourceService.testConnection(id);
        return R.ok(result, "连接测试成功");
    }
}
