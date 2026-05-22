package org.dep.reimbursebackend.controller;

import lombok.RequiredArgsConstructor;
import org.dep.reimbursebackend.common.ApiResponse;
import org.dep.reimbursebackend.common.PageResult;
import org.dep.reimbursebackend.dto.reimburse.ReimburseFormDto;
import org.dep.reimbursebackend.dto.reimburse.ReimburseListItemDto;
import org.dep.reimbursebackend.dto.reimburse.ReimburseListQuery;
import org.dep.reimbursebackend.service.ReimburseService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reimburse")
@RequiredArgsConstructor
public class ReimburseController {

    private final ReimburseService reimburseService;

    @GetMapping
    public ApiResponse<PageResult<ReimburseListItemDto>> list(ReimburseListQuery query) {
        return ApiResponse.ok(reimburseService.list(query));
    }

    @GetMapping("/{id}")
    public ApiResponse<ReimburseFormDto> detail(@PathVariable Long id) {
        return ApiResponse.ok(reimburseService.getById(id));
    }

    @PostMapping
    public ApiResponse<ReimburseFormDto> create(@RequestBody ReimburseFormDto form) {
        form.setId(null);
        return ApiResponse.ok(reimburseService.save(form));
    }

    @PutMapping("/{id}")
    public ApiResponse<ReimburseFormDto> update(@PathVariable Long id, @RequestBody ReimburseFormDto form) {
        form.setId(String.valueOf(id));
        return ApiResponse.ok(reimburseService.save(form));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        reimburseService.delete(id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/copy")
    public ApiResponse<ReimburseListItemDto> copy(@PathVariable Long id) {
        return ApiResponse.ok(reimburseService.copy(id));
    }
}
