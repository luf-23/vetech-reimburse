package org.dep.reimburse.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dep.reimburse.entity.*;
import org.dep.reimburse.mapper.*;
import org.dep.reimburse.common.PageResult;
import org.dep.reimburse.dto.*;
import org.dep.reimburse.util.AllocationAmountUtil;
import org.dep.reimburse.util.ReimburseFormValidator;
import org.dep.reimburse.service.ReimburseService;
import org.dep.reimburse.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<ReimburseListItemVO> list(ReimburseListQueryDTO query) {
        int pageNum = Math.max(query.getPage(), 1);
        int size = query.getSize() <= 0 ? 10 : query.getSize();

        Page<ReimburseDoc> pageData = docMapper.selectPageByQuery(new Page<>(pageNum, size), query);

        List<ReimburseListItemVO> records = pageData.getRecords().stream()
                .map(this::toListItem)
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
    public ReimburseFormVO save(ReimburseFormDTO form) {
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

        BigDecimal subsidyTotal = calcFormSubsidyTotal(form);
        AllocationAmountUtil.distribute(subsidyTotal, form.getAllocations());

        ValidateResultVO validation = ReimburseFormValidator.validate(form, subsidyTotal);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.getMessage());
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
        ReimburseFormDTO copy = objectMapper.convertValue(source, ReimburseFormDTO.class);
        copy.setId(null);
        copy.setReimburseNo(null);
        copy.setStatus(0);
        copy.setTitle(source.getTitle() + "-副本");
        copy.setReason(source.getReason());
        copy.setReimburserId(source.getReimburserId());
        copy.setDepartmentId(source.getDepartmentId());
        copy.setCompanyId(source.getCompanyId());
        copy.setBusinessTypeId(source.getBusinessTypeId());
        copy.setSubmitDate(LocalDate.now().format(DATE_FMT));
        List<ReimburseFormDTO.ItineraryItem> copyItineraries = copy.getItineraries();
        List<ReimburseFormDTO.SubsidyInfoItem> copySubsidies = copy.getSubsidies();
        for (int i = 0; i < copyItineraries.size(); i++) {
            String tempId = "copy-it-" + i;
            copyItineraries.get(i).setId(tempId);
        }
        for (int i = 0; i < copySubsidies.size(); i++) {
            copySubsidies.get(i).setId(null);
            if (i < copyItineraries.size()) {
                copySubsidies.get(i).setItineraryId(copyItineraries.get(i).getId());
            }
        }
        for (ReimburseFormDTO.AllocationItem alloc : copy.getAllocations()) {
            alloc.setId(null);
        }
        ReimburseFormVO saved = save(copy);
        ReimburseDoc doc = docMapper.selectById(Long.parseLong(saved.getId()));
        return toListItem(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public ValidateResultVO validate(ReimburseValidateRequestDTO request) {
        return ReimburseFormValidator.validate(request, request.getSubsidyTotal());
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
                        .orderByAsc(ReimburseItinerary::getId));
    }

    private List<ReimburseSubsidy> listSubsidies(Long docId) {
        return subsidyMapper.selectList(
                Wrappers.<ReimburseSubsidy>lambdaQuery()
                        .eq(ReimburseSubsidy::getDocId, docId)
                        .orderByAsc(ReimburseSubsidy::getId));
    }

    private List<ReimburseAllocation> listAllocations(Long docId) {
        return allocationMapper.selectList(
                Wrappers.<ReimburseAllocation>lambdaQuery()
                        .eq(ReimburseAllocation::getDocId, docId)
                        .orderByAsc(ReimburseAllocation::getSortOrder)
                        .orderByAsc(ReimburseAllocation::getId));
    }

    private ReimburseListItemVO toListItem(ReimburseDoc doc) {
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
        if (doc.getCreateTime() != null) {
            vo.setCreateTime(doc.getCreateTime().format(DATE_FMT));
        }
        return vo;
    }

    private ReimburseFormVO toFormVO(ReimburseDoc doc) {
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
            ReimburseFormVO.ItineraryItem item = new ReimburseFormVO.ItineraryItem();
            item.setId(String.valueOf(it.getId()));
            item.setTravelerId(it.getTravelerId());
            item.setDepartCityNo(it.getDepartCityNo());
            item.setArriveCityNo(it.getArriveCityNo());
            item.setStartDate(it.getStartDate().format(DATE_FMT));
            item.setEndDate(it.getEndDate().format(DATE_FMT));
            item.setDescription(it.getDescription());
            vo.getItineraries().add(item);
        }

        Set<String> itineraryIds = vo.getItineraries().stream()
                .map(ReimburseFormVO.ItineraryItem::getId)
                .collect(java.util.stream.Collectors.toSet());

        for (ReimburseSubsidy sub : listSubsidies(doc.getId())) {
            if (sub.getItineraryId() == null) {
                continue;
            }
            String linkedItineraryId = String.valueOf(sub.getItineraryId());
            if (!itineraryIds.contains(linkedItineraryId)) {
                continue;
            }
            ReimburseFormVO.SubsidyInfoItem item = new ReimburseFormVO.SubsidyInfoItem();
            item.setId(String.valueOf(sub.getId()));
            item.setItineraryId(linkedItineraryId);
            item.setTravelerId(sub.getTravelerId());
            item.setStartDate(sub.getStartDate().format(DATE_FMT));
            item.setEndDate(sub.getEndDate().format(DATE_FMT));
            item.setDays(sub.getDays());
            item.setRoute(sub.getRoute());
            item.setSubsidyCityNo(sub.getSubsidyCityNo());
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
            ReimburseFormVO.AllocationItem item = new ReimburseFormVO.AllocationItem();
            item.setId(String.valueOf(alloc.getId()));
            item.setCostAttributionId(alloc.getCostAttributionId());
            item.setProjectId(alloc.getProjectId());
            item.setRatio(alloc.getRatio());
            item.setAmount(alloc.getAmount());
            vo.getAllocations().add(item);
        }

        return vo;
    }

    private void applyHeader(ReimburseDoc doc, ReimburseFormDTO form) {
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

    private void saveChildren(Long docId, ReimburseFormDTO form) {
        Map<String, Long> itineraryIdMap = new HashMap<>();
        int idx = 0;
        for (ReimburseFormDTO.ItineraryItem it : form.getItineraries()) {
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

        for (ReimburseFormDTO.SubsidyInfoItem sub : form.getSubsidies()) {
            if (!StringUtils.hasText(sub.getItineraryId())) {
                throw new IllegalArgumentException("补助信息必须关联补录行程");
            }
            Long mapped = itineraryIdMap.get(sub.getItineraryId());
            if (mapped == null) {
                try {
                    mapped = Long.parseLong(sub.getItineraryId());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("存在未关联补录行程的补助信息");
                }
                if (!itineraryIdMap.containsValue(mapped)) {
                    throw new IllegalArgumentException("存在未关联补录行程的补助信息");
                }
            }
            ReimburseSubsidy entity = new ReimburseSubsidy();
            entity.setDocId(docId);
            entity.setItineraryId(mapped);
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
        for (ReimburseFormDTO.AllocationItem alloc : form.getAllocations()) {
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

    private BigDecimal calcFormSubsidyTotal(ReimburseFormDTO form) {
        if (form.getSubsidies() == null || form.getSubsidies().isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return form.getSubsidies().stream()
                .map(s -> s.getSubsidyAmount() != null ? s.getSubsidyAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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

}
