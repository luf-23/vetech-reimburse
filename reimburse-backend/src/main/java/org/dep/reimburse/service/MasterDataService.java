package org.dep.reimburse.service;

import org.dep.reimburse.vo.*;

import java.util.List;

public interface MasterDataService {

    MasterDataBundleVO loadAll();

    List<ReimCompanyVO> listCompanies();

    List<ReimDepartmentVO> listDepartments();

    List<ReimburserVO> listReimbursers();

    List<BusinessTypeVO> listBusinessTypes();

    List<CityVO> listCities();

    List<ProjectVO> listProjects();
}
