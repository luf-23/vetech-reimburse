package org.dep.reimbursebackend.dto.master;

import lombok.Data;

import java.util.List;

@Data
public class MasterDataBundleDto {
    private List<ReimCompanyDto> companies;
    private List<ReimDepartmentDto> departments;
    private List<ReimburserDto> reimbursers;
    private List<BusinessTypeDto> businessTypes;
    private List<CityDto> cities;
    private List<ProjectDto> projects;
}
