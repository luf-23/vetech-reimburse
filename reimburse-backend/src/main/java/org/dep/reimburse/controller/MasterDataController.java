package org.dep.reimburse.controller;

import org.dep.reimburse.service.MasterDataService;
import org.dep.reimburse.vo.Result;
import org.dep.reimburse.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/master")
public class MasterDataController {

    @Autowired
    private MasterDataService masterDataService;

    @GetMapping
    public Result<MasterDataBundleVO> loadAll() {
        return Result.success(masterDataService.loadAll());
    }

    @GetMapping("/companies")
    public Result<List<ReimCompanyVO>> companies() {
        return Result.success(masterDataService.listCompanies());
    }

    @GetMapping("/departments")
    public Result<List<ReimDepartmentVO>> departments() {
        return Result.success(masterDataService.listDepartments());
    }

    @GetMapping("/reimbursers")
    public Result<List<ReimburserVO>> reimbursers() {
        return Result.success(masterDataService.listReimbursers());
    }

    @GetMapping("/business-types")
    public Result<List<BusinessTypeVO>> businessTypes() {
        return Result.success(masterDataService.listBusinessTypes());
    }

    @GetMapping("/cities")
    public Result<List<CityVO>> cities() {
        return Result.success(masterDataService.listCities());
    }

    @GetMapping("/projects")
    public Result<List<ProjectVO>> projects() {
        return Result.success(masterDataService.listProjects());
    }
}
