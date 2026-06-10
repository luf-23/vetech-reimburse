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
import org.dep.reimburse.service.ReimburseDocCacheService;
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
    private ReimburseDocCacheService docCacheService;
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
        // 对分页参数做兜底，避免前端传入 0 或负数导致分页异常
        int pageNum = Math.max(query.getPage(), 1);
        int size = query.getSize() <= 0 ? 10 : query.getSize();

        // 通过自定义 Mapper SQL 按条件分页查询主表数据
        Page<ReimburseDoc> pageData = docMapper.selectPageByQuery(new Page<>(pageNum, size), query);

        // 列表接口只返回列表需要的字段，避免把详情子表一起查出来
        List<ReimburseListItemVO> records = pageData.getRecords().stream()
                .map(this::toListItem)
                .toList();

        return new PageResult<>(records, pageData.getTotal(), pageNum, size);
    }

    @Override
    @Transactional(readOnly = true)
    public ReimburseFormVO getById(Long id) {
        // 详情查询先走缓存，未命中时再从数据库加载主表
        ReimburseDoc doc = docCacheService.loadById(id);
        if (doc == null) {
            throw new NoSuchElementException("报销单不存在");
        }
        // 子表数据实时查询，组装成前端表单需要的 VO
        return toFormVO(doc);
    }

    @Override
    @Transactional
    public ReimburseFormVO create(ReimburseFormDTO form) {
        // 新增时忽略前端传入的 id，由数据库自增主键生成真实 id
        form.setId(null);
        ReimburseDoc doc = new ReimburseDoc();
        // 初始化单据号、单据类型、草稿状态和创建日期
        doc.setReimburseNo(generateReimburseNo());
        doc.setDocType("差旅费用报销单");
        doc.setStatus(0);
        doc.setCreateTime(LocalDate.now());

        return persistDoc(doc, form, true);
    }

    @Override
    @Transactional
    public ReimburseFormVO update(Long id, ReimburseFormDTO form) {
        // 更新前先确认主表存在，便于给前端返回明确的业务错误
        ReimburseDoc doc = docMapper.selectById(id);
        if (doc == null) {
            throw new NoSuchElementException("报销单不存在");
        }
        // 子表采用“先删后建”的方式处理新增、删除和顺序变化，整个过程由事务保证原子性
        form.setId(String.valueOf(id));
        deleteChildren(id);

        return persistDoc(doc, form, false);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        // 删除前校验存在性，避免用户删除不存在单据时得到静默成功
        if (docMapper.selectById(id) == null) {
            throw new NoSuchElementException("报销单不存在");
        }
        // 主表删除后，数据库外键级联删除行程、补助和分摊子表
        docMapper.deleteById(id);
        // 删除后清理缓存，避免后续详情查询读到旧主表
        docCacheService.evict(id);
    }

    @Override
    @Transactional
    public ReimburseListItemVO copy(Long id) {
        // 复制时读取完整表单，再转成 DTO 复用新增保存流程
        ReimburseFormVO source = getFormByIdFromDb(id);
        ReimburseFormDTO copy = objectMapper.convertValue(source, ReimburseFormDTO.class);
        // 清空主表唯一字段，并重置为草稿状态
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
        // 给复制出来的行程设置临时 id，便于补助信息在入库前继续关联对应行程
        for (int i = 0; i < copyItineraries.size(); i++) {
            String tempId = "copy-it-" + i;
            copyItineraries.get(i).setId(tempId);
        }
        // 清空补助真实 id，并把补助重新绑定到复制后的临时行程 id
        for (int i = 0; i < copySubsidies.size(); i++) {
            copySubsidies.get(i).setId(null);
            if (i < copyItineraries.size()) {
                copySubsidies.get(i).setItineraryId(copyItineraries.get(i).getId());
            }
        }
        // 分摊明细也需要清空 id，保存时由数据库重新生成
        for (ReimburseFormDTO.AllocationItem alloc : copy.getAllocations()) {
            alloc.setId(null);
        }
        ReimburseFormVO saved = create(copy);
        ReimburseDoc doc = getDocByIdFromDb(Long.parseLong(saved.getId()));
        return toListItem(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public ValidateResultVO validate(ReimburseValidateRequestDTO request) {
        return ReimburseFormValidator.validate(request, request.getSubsidyTotal());
    }

    private void deleteChildren(Long docId) {
        // 更新时先按 doc_id 清理旧子表，后续再按当前表单完整重建
        itineraryMapper.delete(Wrappers.<ReimburseItinerary>lambdaQuery().eq(ReimburseItinerary::getDocId, docId));
        subsidyMapper.delete(Wrappers.<ReimburseSubsidy>lambdaQuery().eq(ReimburseSubsidy::getDocId, docId));
        allocationMapper.delete(Wrappers.<ReimburseAllocation>lambdaQuery().eq(ReimburseAllocation::getDocId, docId));
    }

    private ReimburseFormVO persistDoc(ReimburseDoc doc, ReimburseFormDTO form, boolean isNew) {
        // 以后端收到的补助明细重新计算总金额，避免直接信任前端合计
        BigDecimal subsidyTotal = calcFormSubsidyTotal(form);
        // 根据补助总额和分摊比例重新计算分摊金额，保证金额合计准确
        AllocationAmountUtil.distribute(subsidyTotal, form.getAllocations());

        // 入库前做后端业务校验，防止绕过前端校验直接调用接口
        ValidateResultVO validation = ReimburseFormValidator.validate(form, subsidyTotal);
        if (!validation.isValid()) {
            throw new IllegalArgumentException(validation.getMessage());
        }

        // 先保存主表，新增时需要拿到数据库生成的主键供子表关联
        applyHeader(doc, form);
        if (isNew) {
            docMapper.insert(doc);
        } else {
            docMapper.updateById(doc);
        }

        // 保存行程、补助和费用分摊三类子表
        saveChildren(doc.getId(), form);
        // 子表落库后再次汇总补助金额，并回写主表冗余字段，方便列表展示
        doc.setSubsidyAmount(calcDocSubsidyTotal(doc.getId()));
        docMapper.updateById(doc);
        // 保存成功后清理主表缓存，下次读取从数据库加载最新数据
        docCacheService.evict(doc.getId());

        return getFormByIdFromDb(doc.getId());
    }

    private ReimburseFormVO getFormByIdFromDb(Long id) {
        return toFormVO(getDocByIdFromDb(id));
    }

    private ReimburseDoc getDocByIdFromDb(Long id) {
        ReimburseDoc doc = docMapper.selectById(id);
        if (doc == null) {
            throw new NoSuchElementException("报销单不存在");
        }
        return doc;
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
        // 先组装主表字段
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

        // 再查询并组装行程子表
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

        // 记录当前单据下的有效行程 id，组装补助时用于过滤异常关联
        Set<String> itineraryIds = vo.getItineraries().stream()
                .map(ReimburseFormVO.ItineraryItem::getId)
                .collect(java.util.stream.Collectors.toSet());

        // 组装补助子表，补助必须关联本单据下已有的行程
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

        // 最后组装费用归属和分摊明细
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
        // 保存行程后记录“前端临时 id -> 数据库真实 id”的映射，供补助表关联使用
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

        // 保存补助时把前端传来的 itineraryId 转换成数据库真实行程 id
        for (ReimburseFormDTO.SubsidyInfoItem sub : form.getSubsidies()) {
            if (!StringUtils.hasText(sub.getItineraryId())) {
                throw new IllegalArgumentException("补助信息必须关联补录行程");
            }
            Long mapped = itineraryIdMap.get(sub.getItineraryId());
            if (mapped == null) {
                // 兼容前端传入真实数字 id 的场景，但必须属于本次保存的行程集合
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

        // 保存费用分摊，并记录 sort_order 以保证详情回显顺序稳定
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
