package org.dep.reimbursebackend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.dep.reimbursebackend.common.PageResult;
import org.dep.reimbursebackend.dto.reimburse.*;
import org.dep.reimbursebackend.entity.*;
import org.dep.reimbursebackend.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReimburseService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ReimburseDocRepository docRepository;
    private final ReimburseItineraryRepository itineraryRepository;
    private final ReimburseSubsidyRepository subsidyRepository;
    private final ReimburseAllocationRepository allocationRepository;
    private final ReimCompanyRepository companyRepository;
    private final ReimDepartmentRepository departmentRepository;
    private final ReimburserRepository reimburserRepository;
    private final BusinessTypeRepository businessTypeRepository;
    private final CityRepository cityRepository;
    private final ProjectRepository projectRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PageResult<ReimburseListItemDto> list(ReimburseListQuery query) {
        Specification<ReimburseDoc> spec = buildSpec(query);
        int page = Math.max(query.getPage(), 1) - 1;
        int size = query.getSize() <= 0 ? 10 : query.getSize();
        Page<ReimburseDoc> pageData = docRepository.findAll(
                spec,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime", "id"))
        );

        Map<String, ReimCompany> companyMap = indexCompanies();
        Map<String, ReimDepartment> deptMap = indexDepartments();
        Map<String, Reimburser> reimburserMap = indexReimbursers();
        Map<String, BusinessType> businessTypeMap = indexBusinessTypes();

        List<ReimburseListItemDto> records = pageData.getContent().stream()
                .map(doc -> toListItem(doc, companyMap, deptMap, reimburserMap, businessTypeMap))
                .toList();

        return new PageResult<>(records, pageData.getTotalElements(), query.getPage(), size);
    }

    @Transactional(readOnly = true)
    public ReimburseFormDto getById(Long id) {
        ReimburseDoc doc = docRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("报销单不存在"));
        return toFormDto(doc);
    }

    @Transactional
    public ReimburseFormDto save(ReimburseFormDto form) {
        ReimburseDoc doc;
        if (StringUtils.hasText(form.getId())) {
            Long id = Long.parseLong(form.getId());
            doc = docRepository.findById(id).orElseThrow(() -> new NoSuchElementException("报销单不存在"));
            itineraryRepository.deleteByDocId(id);
            subsidyRepository.deleteByDocId(id);
            allocationRepository.deleteByDocId(id);
        } else {
            doc = new ReimburseDoc();
            doc.setReimburseNo(generateReimburseNo());
            doc.setDocType("差旅费用报销单");
            doc.setStatus(0);
            doc.setCreateTime(LocalDate.now());
        }

        applyHeader(doc, form);
        doc = docRepository.save(doc);

        saveChildren(doc.getId(), form);
        doc.setSubsidyAmount(calcDocSubsidyTotal(doc.getId()));
        docRepository.save(doc);

        return getById(doc.getId());
    }

    @Transactional
    public void delete(Long id) {
        if (!docRepository.existsById(id)) {
            throw new NoSuchElementException("报销单不存在");
        }
        docRepository.deleteById(id);
    }

    @Transactional
    public ReimburseListItemDto copy(Long id) {
        ReimburseFormDto source = getById(id);
        ReimburseFormDto copy = new ReimburseFormDto();
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
        for (ItineraryItemDto it : copy.getItineraries()) {
            it.setId(null);
        }
        for (SubsidyInfoItemDto sub : copy.getSubsidies()) {
            sub.setId(null);
            sub.setItineraryId(null);
        }
        for (AllocationItemDto alloc : copy.getAllocations()) {
            alloc.setId(null);
        }
        ReimburseFormDto saved = save(copy);
        ReimburseDoc doc = docRepository.findById(Long.parseLong(saved.getId())).orElseThrow();
        return toListItem(doc, indexCompanies(), indexDepartments(), indexReimbursers(), indexBusinessTypes());
    }

    private Specification<ReimburseDoc> buildSpec(ReimburseListQuery query) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(query.getReimburseNo())) {
                predicates.add(cb.like(root.get("reimburseNo"), "%" + query.getReimburseNo().trim() + "%"));
            }
            if (StringUtils.hasText(query.getTitle())) {
                predicates.add(cb.like(root.get("title"), "%" + query.getTitle().trim() + "%"));
            }
            if (StringUtils.hasText(query.getReason())) {
                predicates.add(cb.like(root.get("reason"), "%" + query.getReason().trim() + "%"));
            }
            if (StringUtils.hasText(query.getCompanyId())) {
                predicates.add(cb.equal(root.get("companyId"), query.getCompanyId()));
            }
            if (StringUtils.hasText(query.getDepartmentId())) {
                predicates.add(cb.equal(root.get("departmentId"), query.getDepartmentId()));
            }
            if (StringUtils.hasText(query.getReimburserId())) {
                predicates.add(cb.equal(root.get("reimburserId"), query.getReimburserId()));
            }
            if (StringUtils.hasText(query.getBusinessTypeId())) {
                predicates.add(cb.equal(root.get("businessTypeId"), query.getBusinessTypeId()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private ReimburseListItemDto toListItem(
            ReimburseDoc doc,
            Map<String, ReimCompany> companyMap,
            Map<String, ReimDepartment> deptMap,
            Map<String, Reimburser> reimburserMap,
            Map<String, BusinessType> businessTypeMap
    ) {
        ReimburseListItemDto dto = new ReimburseListItemDto();
        dto.setId(String.valueOf(doc.getId()));
        dto.setReimburseNo(doc.getReimburseNo());
        dto.setDocType(doc.getDocType());
        dto.setStatus(doc.getStatus());
        dto.setReimburserId(doc.getReimburserId());
        dto.setDepartmentId(doc.getDepartmentId());
        dto.setCompanyId(doc.getCompanyId());
        dto.setBusinessTypeId(doc.getBusinessTypeId());
        dto.setTitle(doc.getTitle());
        dto.setReason(doc.getReason());
        dto.setSubsidyAmount(doc.getSubsidyAmount());

        Reimburser r = reimburserMap.get(doc.getReimburserId());
        if (r != null) {
            dto.setReimburserName(r.getReimburserName());
            dto.setReimburserNo(r.getReimburserNo());
        }
        ReimDepartment d = deptMap.get(doc.getDepartmentId());
        if (d != null) {
            dto.setDepartmentName(d.getReimDepartmentName());
            dto.setDepartmentNo(d.getReimDepartmentNo());
        }
        ReimCompany c = companyMap.get(doc.getCompanyId());
        if (c != null) {
            dto.setCompanyName(c.getReimCompanyName());
        }
        BusinessType b = businessTypeMap.get(doc.getBusinessTypeId());
        if (b != null) {
            dto.setBusinessTypeName(b.getBusinessTypeName());
        }
        if (doc.getCreateTime() != null) {
            dto.setCreateTime(doc.getCreateTime().format(DATE_FMT));
        }
        return dto;
    }

    private ReimburseFormDto toFormDto(ReimburseDoc doc) {
        Map<String, Reimburser> reimburserMap = indexReimbursers();
        Map<String, City> cityMap = indexCities();
        Map<String, ReimCompany> companyMap = indexCompanies();
        Map<String, Project> projectMap = indexProjects();

        ReimburseFormDto dto = new ReimburseFormDto();
        dto.setId(String.valueOf(doc.getId()));
        dto.setReimburseNo(doc.getReimburseNo());
        dto.setStatus(doc.getStatus());
        dto.setTitle(doc.getTitle());
        dto.setReason(doc.getReason());
        dto.setReimburserId(doc.getReimburserId());
        dto.setDepartmentId(doc.getDepartmentId());
        dto.setCompanyId(doc.getCompanyId());
        dto.setBusinessTypeId(doc.getBusinessTypeId());
        dto.setRemark(doc.getRemark());
        if (doc.getSubmitDate() != null) {
            dto.setSubmitDate(doc.getSubmitDate().format(DATE_FMT));
        } else if (doc.getCreateTime() != null) {
            dto.setSubmitDate(doc.getCreateTime().format(DATE_FMT));
        }

        List<ReimburseItinerary> itineraries = itineraryRepository.findByDocIdOrderByIdAsc(doc.getId());
        for (ReimburseItinerary it : itineraries) {
            ItineraryItemDto item = new ItineraryItemDto();
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
            if (depart != null) item.setDepartCityName(depart.getCityName());
            if (arrive != null) item.setArriveCityName(arrive.getCityName());
            item.setStartDate(it.getStartDate().format(DATE_FMT));
            item.setEndDate(it.getEndDate().format(DATE_FMT));
            item.setDescription(it.getDescription());
            dto.getItineraries().add(item);
        }

        List<ReimburseSubsidy> subsidies = subsidyRepository.findByDocIdOrderByIdAsc(doc.getId());
        for (ReimburseSubsidy sub : subsidies) {
            SubsidyInfoItemDto item = new SubsidyInfoItemDto();
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
            if (city != null) item.setSubsidyCityName(city.getCityName());
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
            dto.getSubsidies().add(item);
        }

        List<ReimburseAllocation> allocations = allocationRepository.findByDocIdOrderBySortOrderAscIdAsc(doc.getId());
        for (ReimburseAllocation alloc : allocations) {
            AllocationItemDto item = new AllocationItemDto();
            item.setId(String.valueOf(alloc.getId()));
            item.setCostAttributionId(alloc.getCostAttributionId());
            ReimCompany company = companyMap.get(alloc.getCostAttributionId());
            if (company != null) item.setCostAttributionName(company.getReimCompanyName());
            item.setProjectId(alloc.getProjectId());
            Project project = projectMap.get(alloc.getProjectId());
            if (project != null) item.setProjectName(project.getProjectName());
            item.setRatio(alloc.getRatio());
            item.setAmount(alloc.getAmount());
            dto.getAllocations().add(item);
        }

        return dto;
    }

    private void applyHeader(ReimburseDoc doc, ReimburseFormDto form) {
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

    private void saveChildren(Long docId, ReimburseFormDto form) {
        Map<String, Long> itineraryIdMap = new HashMap<>();
        int idx = 0;
        for (ItineraryItemDto it : form.getItineraries()) {
            ReimburseItinerary entity = new ReimburseItinerary();
            entity.setDocId(docId);
            entity.setTravelerId(it.getTravelerId());
            entity.setDepartCityNo(it.getDepartCityNo());
            entity.setArriveCityNo(it.getArriveCityNo());
            entity.setStartDate(LocalDate.parse(it.getStartDate(), DATE_FMT));
            entity.setEndDate(LocalDate.parse(it.getEndDate(), DATE_FMT));
            entity.setDescription(it.getDescription() != null ? it.getDescription() : "");
            entity = itineraryRepository.save(entity);
            String clientKey = StringUtils.hasText(it.getId()) ? it.getId() : "new-" + idx;
            itineraryIdMap.put(clientKey, entity.getId());
            idx++;
        }

        for (SubsidyInfoItemDto sub : form.getSubsidies()) {
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
            subsidyRepository.save(entity);
        }

        int sort = 0;
        for (AllocationItemDto alloc : form.getAllocations()) {
            ReimburseAllocation entity = new ReimburseAllocation();
            entity.setDocId(docId);
            entity.setCostAttributionId(alloc.getCostAttributionId());
            entity.setProjectId(alloc.getProjectId() != null ? alloc.getProjectId() : "");
            entity.setRatio(alloc.getRatio() != null ? alloc.getRatio() : BigDecimal.ZERO);
            entity.setAmount(alloc.getAmount() != null ? alloc.getAmount() : BigDecimal.ZERO);
            entity.setSortOrder(sort++);
            allocationRepository.save(entity);
        }
    }

    private BigDecimal calcDocSubsidyTotal(Long docId) {
        return subsidyRepository.findByDocIdOrderByIdAsc(docId).stream()
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
        companyRepository.findAll().forEach(c -> map.put(c.getReimCompanyId(), c));
        return map;
    }

    private Map<String, ReimDepartment> indexDepartments() {
        Map<String, ReimDepartment> map = new HashMap<>();
        departmentRepository.findAll().forEach(d -> map.put(d.getReimDepartmentId(), d));
        return map;
    }

    private Map<String, Reimburser> indexReimbursers() {
        Map<String, Reimburser> map = new HashMap<>();
        reimburserRepository.findAll().forEach(r -> map.put(r.getReimburserId(), r));
        return map;
    }

    private Map<String, BusinessType> indexBusinessTypes() {
        Map<String, BusinessType> map = new HashMap<>();
        businessTypeRepository.findAll().forEach(b -> map.put(b.getBusinessTypeId(), b));
        return map;
    }

    private Map<String, City> indexCities() {
        Map<String, City> map = new HashMap<>();
        cityRepository.findAll().forEach(c -> map.put(c.getCityNo(), c));
        return map;
    }

    private Map<String, Project> indexProjects() {
        Map<String, Project> map = new HashMap<>();
        projectRepository.findAll().forEach(p -> map.put(p.getProjectId(), p));
        return map;
    }
}
