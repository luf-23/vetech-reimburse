package org.dep.reimbursebackend.service;

import lombok.RequiredArgsConstructor;
import org.dep.reimbursebackend.dto.master.*;
import org.dep.reimbursebackend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MasterDataService {

    private final ReimCompanyRepository companyRepository;
    private final ReimDepartmentRepository departmentRepository;
    private final ReimburserRepository reimburserRepository;
    private final BusinessTypeRepository businessTypeRepository;
    private final CityRepository cityRepository;
    private final ProjectRepository projectRepository;

    public MasterDataBundleDto loadAll() {
        MasterDataBundleDto bundle = new MasterDataBundleDto();
        bundle.setCompanies(companyRepository.findAll().stream().map(ReimCompanyDto::from).toList());
        bundle.setDepartments(departmentRepository.findAll().stream().map(ReimDepartmentDto::from).toList());
        bundle.setReimbursers(reimburserRepository.findAll().stream().map(ReimburserDto::from).toList());
        bundle.setBusinessTypes(businessTypeRepository.findAll().stream().map(BusinessTypeDto::from).toList());
        bundle.setCities(cityRepository.findAll().stream().map(CityDto::from).toList());
        bundle.setProjects(projectRepository.findAll().stream().map(ProjectDto::from).toList());
        return bundle;
    }

    public List<ReimCompanyDto> listCompanies() {
        return companyRepository.findAll().stream().map(ReimCompanyDto::from).toList();
    }

    public List<ReimDepartmentDto> listDepartments() {
        return departmentRepository.findAll().stream().map(ReimDepartmentDto::from).toList();
    }

    public List<ReimburserDto> listReimbursers() {
        return reimburserRepository.findAll().stream().map(ReimburserDto::from).toList();
    }

    public List<BusinessTypeDto> listBusinessTypes() {
        return businessTypeRepository.findAll().stream().map(BusinessTypeDto::from).toList();
    }

    public List<CityDto> listCities() {
        return cityRepository.findAll().stream().map(CityDto::from).toList();
    }

    public List<ProjectDto> listProjects() {
        return projectRepository.findAll().stream().map(ProjectDto::from).toList();
    }
}
