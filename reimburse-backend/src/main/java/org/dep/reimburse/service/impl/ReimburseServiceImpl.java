package org.dep.reimburse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dep.reimburse.entity.*;
import org.dep.reimburse.mapper.*;
import org.dep.reimburse.service.ReimburseService;
import org.dep.reimburse.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ReimburseServiceImpl implements ReimburseService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    @Autowired
    private ReimburseDocMapper docMapper;
    @Autowired
    private ReimburseItineraryMapper itineraryMapper;
    @Autowired
    private ReimburseSubsidyMapper subsidyMapper;
    @Autowired
    private ReimburseAllocationMapper allocationMapper;
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
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<ReimburseListItemVO> list(ReimburseListQuery query) {
        int pageNum = Math.max(query.getPage(), 1);
        int size = query.getSize() <= 0 ? 10 : query.getSize();

        LambdaQueryWrapper<ReimburseDoc> wrapper = buildQueryWrapper(query);
        wrapper.orderByDesc(ReimburseDoc::getCreateTime).orderByDesc(ReimburseDoc::getId);

        Page<ReimburseDoc> pageData = docMapper.selectPage(new Page<>(pageNum, size), wrapper);

        Map<String, ReimCompany> companyMap = indexCompanies();
        Map<String, ReimDepartment> deptMap = indexDepartments();
        Map<String, Reimburser> reimburserMap = indexReimbursers();
        Map<String, BusinessType> businessTypeMap = indexBusinessTypes();

        List<ReimburseListItemVO> records = pageData.getRecords().stream()
                .map(doc -> toListItem(doc, companyMap, deptMap, reimburserMap, businessTypeMap))
                .toList();

        return new PageResult<>(records, pageData.getTotal(), pageNum, size);
    }

    @Override
    @Transactional(readOnly = true)
    public ReimburseFormVO getById(Long id) {
        ReimburseDoc doc = docMapper.selectById(id);
        if (doc == null) {
            throw new NoSuchElementException("报销单不存在");
        }
        return toFormVO(doc);
    }

    @Override
    @Transactional
    public ReimburseFormVO save(ReimburseFormVO form) {
        ReimburseDoc doc;
        if (StringUtils.hasText(form.getId())) {
            Long id = Long.parseLong(form.getId());
            doc = docMapper.selectById(id);
            if (doc == null) {
                throw new NoSuchElementException("报销单不存在");
            }
            deleteChildren(id);
        } else {
            doc = new ReimburseDoc();
            doc.setReimburseNo(generateReimburseNo());
            doc.setDocType("差旅费用报销单");
            doc.setStatus(0);
            doc.setCreateTime(LocalDate.now());
        }

        applyHeader(doc, form);
        if (doc.getId() == null) {
            docMapper.insert(doc);
        } else {
            docMapper.updateById(doc);
        }

        saveChildren(doc.getId(), form);
        doc.setSubsidyAmount(calcDocSubsidyTotal(doc.getId()));
        docMapper.updateById(doc);

        return getById(doc.getId());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (docMapper.selectById(id) == null) {
            throw new NoSuchElementException("报销单不存在");
        }
        docMapper.deleteById(id);
    }

    @Override
    @Transactional
    public ReimburseListItemVO copy(Long id) {
        ReimburseFormVO source = getById(id);
        ReimburseFormVO copy = new ReimburseFormVO();
        copy.setStatus(0);
        copy.setTitle(source.getTitle() + "-副本");
        copy.setReason(source.getReason());
        copy.setReimburserId(source.getReimburserId());
        copy.setDepartmentId(source.getDepartmentId());
        copy.setCompanyId(source.getCompanyId());
        copy.setBusinessTypeId(source.getBusinessTypeId());
        copy.setSubmitDate(LocalDate.now().format(DATE_FMT));
        copy.setItineraries(source.getItineraries());
        copy.setSubsidies(source.getSubsidies());
        copy.setAllocations(source.getAllocations());
        copy.setRemark(source.getRemark());
        for (ItineraryItemVO it : copy.getItineraries()) {
            it.setId(null);
        }
        for (SubsidyInfoItemVO sub : copy.getSubsidies()) {
            sub.setId(null);
            sub.setItineraryId(null);
        }
        for (AllocationItemVO alloc : copy.getAllocations()) {
            alloc.setId(null);
        }
        ReimburseFormVO saved = save(copy);
        ReimburseDoc doc = docMapper.selectById(Long.parseLong(saved.getId()));
        return toListItem(doc, indexCompanies(), indexDepartments(), indexReimbursers(), indexBusinessTypes());
    }

    private LambdaQueryWrapper<ReimburseDoc> buildQueryWrapper(ReimburseListQuery query) {
        LambdaQueryWrapper<ReimburseDoc> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(query.getReimburseNo())) {
            wrapper.like(ReimburseDoc::getReimburseNo, query.getReimburseNo().trim());
        }
        if (StringUtils.hasText(query.getTitle())) {
            wrapper.like(ReimburseDoc::getTitle, query.getTitle().trim());
        }
        if (StringUtils.hasText(query.getReason())) {
            wrapper.like(ReimburseDoc::getReason, query.getReason().trim());
        }
        if (StringUtils.hasText(query.getCompanyId())) {
            wrapper.eq(ReimburseDoc::getCompanyId, query.getCompanyId());
        }
        if (StringUtils.hasText(query.getDepartmentId())) {
            wrapper.eq(ReimburseDoc::getDepartmentId, query.getDepartmentId());
        }
        if (StringUtils.hasText(query.getReimburserId())) {
            wrapper.eq(ReimburseDoc::getReimburserId, query.getReimburserId());
        }
        if (StringUtils.hasText(query.getBusinessTypeId())) {
            wrapper.eq(ReimburseDoc::getBusinessTypeId, query.getBusinessTypeId());
        }
        return wrapper;
    }

    private void deleteChildren(Long docId) {
        itineraryMapper.delete(Wrappers.<ReimburseItinerary>lambdaQuery().eq(ReimburseItinerary::getDocId, docId));
        subsidyMapper.delete(Wrappers.<ReimburseSubsidy>lambdaQuery().eq(ReimburseSubsidy::getDocId, docId));
        allocationMapper.delete(Wrappers.<ReimburseAllocation>lambdaQuery().eq(ReimburseAllocation::getDocId, docId));
    }

    private List<ReimburseItinerary> listItineraries(Long docId) {
        return itineraryMapper.selectList(
                Wrappers.<ReimburseItinerary>lambdaQuery()
                        .eq(ReimburseItinerary::getDocId, docId)
                        .orderByAsc(ReimburseItinerary::getId)
        );
    }

    private List<ReimburseSubsidy> listSubsidies(Long docId) {
        return subsidyMapper.selectList(
                Wrappers.<ReimburseSubsidy>lambdaQuery()
                        .eq(ReimburseSubsidy::getDocId, docId)
                        .orderByAsc(ReimburseSubsidy::getId)
        );
    }

    private List<ReimburseAllocation> listAllocations(Long docId) {
        return allocationMapper.selectList(
                Wrappers.<ReimburseAllocation>lambdaQuery()
                        .eq(ReimburseAllocation::getDocId, docId)
                        .orderByAsc(ReimburseAllocation::getSortOrder)
                        .orderByAsc(ReimburseAllocation::getId)
        );
    }

    private ReimburseListItemVO toListItem(
            ReimburseDoc doc,
            Map<String, ReimCompany> companyMap,
            Map<String, ReimDepartment> deptMap,
            Map<String, Reimburser> reimburserMap,
            Map<String, BusinessType> businessTypeMap
    ) {
        ReimburseListItemVO vo = new ReimburseListItemVO();
        vo.setId(String.valueOf(doc.getId()));
        vo.setReimburseNo(doc.getReimburseNo());
        vo.setDocType(doc.getDocType());
        vo.setStatus(doc.getStatus());
        vo.setReimburserId(doc.getReimburserId());
        vo.setDepartmentId(doc.getDepartmentId());
        vo.setCompanyId(doc.getCompanyId());
        vo.setBusinessTypeId(doc.getBusinessTypeId());
        vo.setTitle(doc.getTitle());
        vo.setReason(doc.getReason());
        vo.setSubsidyAmount(doc.getSubsidyAmount());

        Reimburser r = reimburserMap.get(doc.getReimburserId());
        if (r != null) {
            vo.setReimburserName(r.getReimburserName());
            vo.setReimburserNo(r.getReimburserNo());
        }
        ReimDepartment d = deptMap.get(doc.getDepartmentId());
        if (d != null) {
            vo.setDepartmentName(d.getReimDepartmentName());
            vo.setDepartmentNo(d.getReimDepartmentNo());
        }
        ReimCompany c = companyMap.get(doc.getCompanyId());
        if (c != null) {
            vo.setCompanyName(c.getReimCompanyName());
        }
        BusinessType b = businessTypeMap.get(doc.getBusinessTypeId());
        if (b != null) {
            vo.setBusinessTypeName(b.getBusinessTypeName());
        }
        if (doc.getCreateTime() != null) {
            vo.setCreateTime(doc.getCreateTime().format(DATE_FMT));
        }
        return vo;
    }

    private ReimburseFormVO toFormVO(ReimburseDoc doc) {
        Map<String, Reimburser> reimburserMap = indexReimbursers();
        Map<String, City> cityMap = indexCities();
        Map<String, ReimCompany> companyMap = indexCompanies();
        Map<String, Project> projectMap = indexProjects();

        ReimburseFormVO vo = new ReimburseFormVO();
        vo.setId(String.valueOf(doc.getId()));
        vo.setReimburseNo(doc.getReimburseNo());
        vo.setStatus(doc.getStatus());
        vo.setTitle(doc.getTitle());
        vo.setReason(doc.getReason());
        vo.setReimburserId(doc.getReimburserId());
        vo.setDepartmentId(doc.getDepartmentId());
        vo.setCompanyId(doc.getCompanyId());
        vo.setBusinessTypeId(doc.getBusinessTypeId());
        vo.setRemark(doc.getRemark());
        if (doc.getSubmitDate() != null) {
            vo.setSubmitDate(doc.getSubmitDate().format(DATE_FMT));
        } else if (doc.getCreateTime() != null) {
            vo.setSubmitDate(doc.getCreateTime().format(DATE_FMT));
        }

        for (ReimburseItinerary it : listItineraries(doc.getId())) {
            ItineraryItemVO item = new ItineraryItemVO();
            item.setId(String.valueOf(it.getId()));
            item.setTravelerId(it.getTravelerId());
            Reimburser traveler = reimburserMap.get(it.getTravelerId());
            if (traveler != null) {
                item.setTravelerName(traveler.getReimburserName());
                item.setTravelerNo(traveler.getReimburserNo());
            }
            item.setDepartCityNo(it.getDepartCityNo());
            item.setArriveCityNo(it.getArriveCityNo());
            City depart = cityMap.get(it.getDepartCityNo());
            City arrive = cityMap.get(it.getArriveCityNo());
            if (depart != null) {
                item.setDepartCityName(depart.getCityName());
            }
            if (arrive != null) {
                item.setArriveCityName(arrive.getCityName());
            }
            item.setStartDate(it.getStartDate().format(DATE_FMT));
            item.setEndDate(it.getEndDate().format(DATE_FMT));
            item.setDescription(it.getDescription());
            vo.getItineraries().add(item);
        }

        for (ReimburseSubsidy sub : listSubsidies(doc.getId())) {
            SubsidyInfoItemVO item = new SubsidyInfoItemVO();
            item.setId(String.valueOf(sub.getId()));
            if (sub.getItineraryId() != null) {
                item.setItineraryId(String.valueOf(sub.getItineraryId()));
            }
            item.setTravelerId(sub.getTravelerId());
            Reimburser traveler = reimburserMap.get(sub.getTravelerId());
            if (traveler != null) {
                item.setTravelerName(traveler.getReimburserName());
            }
            item.setStartDate(sub.getStartDate().format(DATE_FMT));
            item.setEndDate(sub.getEndDate().format(DATE_FMT));
            item.setDays(sub.getDays());
            item.setRoute(sub.getRoute());
            item.setSubsidyCityNo(sub.getSubsidyCityNo());
            City city = cityMap.get(sub.getSubsidyCityNo());
            if (city != null) {
                item.setSubsidyCityName(city.getCityName());
            }
            item.setApplyAmount(sub.getApplyAmount());
            item.setSubsidyAmount(sub.getSubsidyAmount());
            item.setMealTotal(sub.getMealTotal());
            item.setTransportTotal(sub.getTransportTotal());
            item.setCommTotal(sub.getCommTotal());
            if (StringUtils.hasText(sub.getCalendarJson())) {
                try {
                    item.setCalendar(objectMapper.readTree(sub.getCalendarJson()));
                } catch (JsonProcessingException ignored) {
                    /* keep calendar null */
                }
            }
            vo.getSubsidies().add(item);
        }

        for (ReimburseAllocation alloc : listAllocations(doc.getId())) {
            AllocationItemVO item = new AllocationItemVO();
            item.setId(String.valueOf(alloc.getId()));
            item.setCostAttributionId(alloc.getCostAttributionId());
            ReimCompany company = companyMap.get(alloc.getCostAttributionId());
            if (company != null) {
                item.setCostAttributionName(company.getReimCompanyName());
            }
            item.setProjectId(alloc.getProjectId());
            Project project = projectMap.get(alloc.getProjectId());
            if (project != null) {
                item.setProjectName(project.getProjectName());
            }
            item.setRatio(alloc.getRatio());
            item.setAmount(alloc.getAmount());
            vo.getAllocations().add(item);
        }

        return vo;
    }

    private void applyHeader(ReimburseDoc doc, ReimburseFormVO form) {
        doc.setTitle(form.getTitle());
        doc.setReason(form.getReason() != null ? form.getReason() : "");
        doc.setReimburserId(form.getReimburserId());
        doc.setDepartmentId(form.getDepartmentId());
        doc.setCompanyId(form.getCompanyId());
        doc.setBusinessTypeId(form.getBusinessTypeId());
        doc.setRemark(form.getRemark() != null ? form.getRemark() : "");
        if (form.getStatus() != null) {
            doc.setStatus(form.getStatus());
        }
        if (StringUtils.hasText(form.getSubmitDate())) {
            doc.setSubmitDate(LocalDate.parse(form.getSubmitDate(), DATE_FMT));
        }
    }

    private void saveChildren(Long docId, ReimburseFormVO form) {
        Map<String, Long> itineraryIdMap = new HashMap<>();
        int idx = 0;
        for (ItineraryItemVO it : form.getItineraries()) {
            ReimburseItinerary entity = new ReimburseItinerary();
            entity.setDocId(docId);
            entity.setTravelerId(it.getTravelerId());
            entity.setDepartCityNo(it.getDepartCityNo());
            entity.setArriveCityNo(it.getArriveCityNo());
            entity.setStartDate(LocalDate.parse(it.getStartDate(), DATE_FMT));
            entity.setEndDate(LocalDate.parse(it.getEndDate(), DATE_FMT));
            entity.setDescription(it.getDescription() != null ? it.getDescription() : "");
            itineraryMapper.insert(entity);
            String clientKey = StringUtils.hasText(it.getId()) ? it.getId() : "new-" + idx;
            itineraryIdMap.put(clientKey, entity.getId());
            idx++;
        }

        for (SubsidyInfoItemVO sub : form.getSubsidies()) {
            ReimburseSubsidy entity = new ReimburseSubsidy();
            entity.setDocId(docId);
            if (StringUtils.hasText(sub.getItineraryId())) {
                Long mapped = itineraryIdMap.get(sub.getItineraryId());
                if (mapped == null) {
                    try {
                        mapped = Long.parseLong(sub.getItineraryId());
                    } catch (NumberFormatException ignored) {
                        /* optional link */
                    }
                }
                entity.setItineraryId(mapped);
            }
            entity.setTravelerId(sub.getTravelerId());
            entity.setStartDate(LocalDate.parse(sub.getStartDate(), DATE_FMT));
            entity.setEndDate(LocalDate.parse(sub.getEndDate(), DATE_FMT));
            entity.setDays(sub.getDays() != null ? sub.getDays() : 0);
            entity.setRoute(sub.getRoute() != null ? sub.getRoute() : "");
            entity.setSubsidyCityNo(sub.getSubsidyCityNo());
            entity.setApplyAmount(defaultDecimal(sub.getApplyAmount()));
            entity.setSubsidyAmount(defaultDecimal(sub.getSubsidyAmount()));
            entity.setMealTotal(defaultDecimal(sub.getMealTotal()));
            entity.setTransportTotal(defaultDecimal(sub.getTransportTotal()));
            entity.setCommTotal(defaultDecimal(sub.getCommTotal()));
            if (sub.getCalendar() != null) {
                try {
                    entity.setCalendarJson(objectMapper.writeValueAsString(sub.getCalendar()));
                } catch (JsonProcessingException e) {
                    throw new IllegalArgumentException("补助日历数据格式错误");
                }
            }
            subsidyMapper.insert(entity);
        }

        int sort = 0;
        for (AllocationItemVO alloc : form.getAllocations()) {
            ReimburseAllocation entity = new ReimburseAllocation();
            entity.setDocId(docId);
            entity.setCostAttributionId(alloc.getCostAttributionId());
            entity.setProjectId(alloc.getProjectId() != null ? alloc.getProjectId() : "");
            entity.setRatio(alloc.getRatio() != null ? alloc.getRatio() : BigDecimal.ZERO);
            entity.setAmount(alloc.getAmount() != null ? alloc.getAmount() : BigDecimal.ZERO);
            entity.setSortOrder(sort++);
            allocationMapper.insert(entity);
        }
    }

    private BigDecimal calcDocSubsidyTotal(Long docId) {
        return listSubsidies(docId).stream()
                .map(s -> s.getSubsidyAmount() != null ? s.getSubsidyAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String generateReimburseNo() {
        return "RCBX" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%04d", System.currentTimeMillis() % 10000);
    }

    private Map<String, ReimCompany> indexCompanies() {
        Map<String, ReimCompany> map = new HashMap<>();
        companyMapper.selectList(null).forEach(c -> map.put(c.getReimCompanyId(), c));
        return map;
    }

    private Map<String, ReimDepartment> indexDepartments() {
        Map<String, ReimDepartment> map = new HashMap<>();
        departmentMapper.selectList(null).forEach(d -> map.put(d.getReimDepartmentId(), d));
        return map;
    }

    private Map<String, Reimburser> indexReimbursers() {
        Map<String, Reimburser> map = new HashMap<>();
        reimburserMapper.selectList(null).forEach(r -> map.put(r.getReimburserId(), r));
        return map;
    }

    private Map<String, BusinessType> indexBusinessTypes() {
        Map<String, BusinessType> map = new HashMap<>();
        businessTypeMapper.selectList(null).forEach(b -> map.put(b.getBusinessTypeId(), b));
        return map;
    }

    private Map<String, City> indexCities() {
        Map<String, City> map = new HashMap<>();
        cityMapper.selectList(null).forEach(c -> map.put(c.getCityNo(), c));
        return map;
    }

    private Map<String, Project> indexProjects() {
        Map<String, Project> map = new HashMap<>();
        projectMapper.selectList(null).forEach(p -> map.put(p.getProjectId(), p));
        return map;
    }
}
