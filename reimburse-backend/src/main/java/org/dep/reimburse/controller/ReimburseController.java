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

@RestController
@RequestMapping("/api/reimburse")
public class ReimburseController {

    @Autowired
    private ReimburseService reimburseService;

    @GetMapping
    public Result<PageResult<ReimburseListItemVO>> list(ReimburseListQueryDTO query) {
        return Result.success(reimburseService.list(query));
    }

    @GetMapping("/{id}")
    public Result<ReimburseFormVO> detail(@PathVariable Long id) {
        return Result.success(reimburseService.getById(id));
    }

    @PostMapping
    public Result<ReimburseFormVO> create(@RequestBody ReimburseFormDTO form) {
        form.setId(null);
        return Result.success(reimburseService.save(form));
    }

    @PutMapping("/{id}")
    public Result<ReimburseFormVO> update(@PathVariable Long id, @RequestBody ReimburseFormDTO form) {
        form.setId(String.valueOf(id));
        return Result.success(reimburseService.save(form));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        reimburseService.delete(id);
        return Result.success(null);
    }

    @PostMapping("/{id}/copy")
    public Result<ReimburseListItemVO> copy(@PathVariable Long id) {
        return Result.success(reimburseService.copy(id));
    }

    @PostMapping("/validate")
    public Result<ValidateResultVO> validate(@RequestBody ReimburseValidateRequestDTO request) {
        return Result.success(reimburseService.validate(request));
    }
}
