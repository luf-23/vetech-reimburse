package org.dep.reimburse.service.impl;

import org.dep.reimburse.mapper.*;
import org.dep.reimburse.service.MasterDataService;
import org.dep.reimburse.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MasterDataServiceImpl implements MasterDataService {

    @Autowired
    private ReimCompanyMapper companyMapper;
    @Autowired
    private ReimDepartmentMapper departmentMapper;
    @Autowired
    private ReimburserMapper reimburserMapper;
    @Autowired
    private BusinessTypeMapper businessTypeMapper;
    @Autowired
    private CityMapper cityMapper;
    @Autowired
    private ProjectMapper projectMapper;

    @Override
    public MasterDataBundleVO loadAll() {
        MasterDataBundleVO bundle = new MasterDataBundleVO();
        bundle.setCompanies(companyMapper.selectList(null).stream().map(ReimCompanyVO::from).toList());
        bundle.setDepartments(departmentMapper.selectList(null).stream().map(ReimDepartmentVO::from).toList());
        bundle.setReimbursers(reimburserMapper.selectList(null).stream().map(ReimburserVO::from).toList());
        bundle.setBusinessTypes(businessTypeMapper.selectList(null).stream().map(BusinessTypeVO::from).toList());
        bundle.setCities(cityMapper.selectList(null).stream().map(CityVO::from).toList());
        bundle.setProjects(projectMapper.selectList(null).stream().map(ProjectVO::from).toList());
        return bundle;
    }

    @Override
    public List<ReimCompanyVO> listCompanies() {
        return companyMapper.selectList(null).stream().map(ReimCompanyVO::from).toList();
    }

    @Override
    public List<ReimDepartmentVO> listDepartments() {
        return departmentMapper.selectList(null).stream().map(ReimDepartmentVO::from).toList();
    }

    @Override
    public List<ReimburserVO> listReimbursers() {
        return reimburserMapper.selectList(null).stream().map(ReimburserVO::from).toList();
    }

    @Override
    public List<BusinessTypeVO> listBusinessTypes() {
        return businessTypeMapper.selectList(null).stream().map(BusinessTypeVO::from).toList();
    }

    @Override
    public List<CityVO> listCities() {
        return cityMapper.selectList(null).stream().map(CityVO::from).toList();
    }

    @Override
    public List<ProjectVO> listProjects() {
        return projectMapper.selectList(null).stream().map(ProjectVO::from).toList();
    }
}
