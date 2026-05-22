package org.dep.reimburse.vo;

import lombok.Data;

import java.util.List;

@Data
public class MasterDataBundleVO {
    private List<ReimCompanyVO> companies;
    private List<ReimDepartmentVO> departments;
    private List<ReimburserVO> reimbursers;
    private List<BusinessTypeVO> businessTypes;
    private List<CityVO> cities;
    private List<ProjectVO> projects;
}
