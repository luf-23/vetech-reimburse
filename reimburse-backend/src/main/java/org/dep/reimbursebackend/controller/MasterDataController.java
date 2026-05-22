package org.dep.reimbursebackend.controller;

import lombok.RequiredArgsConstructor;
import org.dep.reimbursebackend.common.ApiResponse;
import org.dep.reimbursebackend.dto.master.*;
import org.dep.reimbursebackend.service.MasterDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/master")
@RequiredArgsConstructor
public class MasterDataController {

    private final MasterDataService masterDataService;

    @GetMapping
    public ApiResponse<MasterDataBundleDto> loadAll() {
        return ApiResponse.ok(masterDataService.loadAll());
    }

    @GetMapping("/companies")
    public ApiResponse<List<ReimCompanyDto>> companies() {
        return ApiResponse.ok(masterDataService.listCompanies());
    }

    @GetMapping("/departments")
    public ApiResponse<List<ReimDepartmentDto>> departments() {
        return ApiResponse.ok(masterDataService.listDepartments());
    }

    @GetMapping("/reimbursers")
    public ApiResponse<List<ReimburserDto>> reimbursers() {
        return ApiResponse.ok(masterDataService.listReimbursers());
    }

    @GetMapping("/business-types")
    public ApiResponse<List<BusinessTypeDto>> businessTypes() {
        return ApiResponse.ok(masterDataService.listBusinessTypes());
    }

    @GetMapping("/cities")
    public ApiResponse<List<CityDto>> cities() {
        return ApiResponse.ok(masterDataService.listCities());
    }

    @GetMapping("/projects")
    public ApiResponse<List<ProjectDto>> projects() {
        return ApiResponse.ok(masterDataService.listProjects());
    }
}
