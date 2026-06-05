package org.dep.reimburse.service;

import org.dep.reimburse.common.PageResult;
import org.dep.reimburse.dto.ReimburseFormDTO;
import org.dep.reimburse.dto.ReimburseListQueryDTO;
import org.dep.reimburse.dto.ReimburseValidateRequestDTO;
import org.dep.reimburse.vo.ReimburseFormVO;
import org.dep.reimburse.vo.ReimburseListItemVO;
import org.dep.reimburse.vo.ValidateResultVO;

public interface ReimburseService {

    PageResult<ReimburseListItemVO> list(ReimburseListQueryDTO query);

    ReimburseFormVO getById(Long id);

    ReimburseFormVO create(ReimburseFormDTO form);

    ReimburseFormVO update(Long id, ReimburseFormDTO form);

    void delete(Long id);

    ReimburseListItemVO copy(Long id);

    ValidateResultVO validate(ReimburseValidateRequestDTO request);
}
