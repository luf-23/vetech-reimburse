package org.dep.reimburse.service;

import org.dep.reimburse.vo.PageResult;
import org.dep.reimburse.vo.ReimburseFormVO;
import org.dep.reimburse.vo.ReimburseListItemVO;
import org.dep.reimburse.vo.ReimburseListQuery;
import org.dep.reimburse.vo.ReimburseValidateRequest;
import org.dep.reimburse.vo.ValidateResultVO;

public interface ReimburseService {

    PageResult<ReimburseListItemVO> list(ReimburseListQuery query);

    ReimburseFormVO getById(Long id);

    ReimburseFormVO save(ReimburseFormVO form);

    void delete(Long id);

    ReimburseListItemVO copy(Long id);

    ValidateResultVO validate(ReimburseValidateRequest request);
}
