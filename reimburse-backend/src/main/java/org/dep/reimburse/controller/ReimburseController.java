package org.dep.reimburse.controller;

import org.dep.reimburse.common.PageResult;
import org.dep.reimburse.common.Result;
import org.dep.reimburse.dto.ReimburseFormDTO;
import org.dep.reimburse.dto.ReimburseListQueryDTO;
import org.dep.reimburse.dto.ReimburseValidateRequestDTO;
import org.dep.reimburse.service.ReimburseService;
import org.dep.reimburse.vo.ReimburseFormVO;
import org.dep.reimburse.vo.ReimburseListItemVO;
import org.dep.reimburse.vo.ValidateResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

// 报销单接口，统一前缀 /api/reimburse
@RestController
@RequestMapping("/api/reimburse")
public class ReimburseController {

    @Autowired
    private ReimburseService reimburseService;

    // GET /api/reimburse/list — 分页查询报销单列表（支持单号、标题、事由及主数据筛选）
    @GetMapping("/list")
    public Result<PageResult<ReimburseListItemVO>> list(ReimburseListQueryDTO query) {
        return Result.success(reimburseService.list(query));
    }

    // GET /api/reimburse/detail/{id} — 获取报销单详情（含行程、补贴、分摊等子表）
    @GetMapping("/detail/{id}")
    public Result<ReimburseFormVO> detail(@PathVariable Long id) {
        return Result.success(reimburseService.getById(id));
    }

    // POST /api/reimburse/create — 新建报销单
    @PostMapping("/create")
    public Result<ReimburseFormVO> create(@RequestBody ReimburseFormDTO form) {
        form.setId(null);
        return Result.success(reimburseService.save(form));
    }

    // PUT /api/reimburse/update/{id} — 更新指定报销单
    @PutMapping("/update/{id}")
    public Result<ReimburseFormVO> update(@PathVariable Long id, @RequestBody ReimburseFormDTO form) {
        form.setId(String.valueOf(id));
        return Result.success(reimburseService.save(form));
    }

    // DELETE /api/reimburse/delete/{id} — 删除指定报销单
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        reimburseService.delete(id);
        return Result.success(null);
    }

    // POST /api/reimburse/copy/{id} — 复制报销单，返回新列表项
    @PostMapping("/copy/{id}")
    public Result<ReimburseListItemVO> copy(@PathVariable Long id) {
        return Result.success(reimburseService.copy(id));
    }

    // POST /api/reimburse/validate — 提交前服务端校验（表单 + 补贴合计）
    @PostMapping("/validate")
    public Result<ValidateResultVO> validate(@RequestBody ReimburseValidateRequestDTO request) {
        return Result.success(reimburseService.validate(request));
    }
}
