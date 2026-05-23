package org.dep.reimburse.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dep.reimburse.dto.ReimburseFormDTO;
import org.dep.reimburse.vo.ReimburseFormVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReimburseConverter {

    @Autowired
    private ObjectMapper objectMapper;

    public ReimburseFormVO toVO(ReimburseFormDTO dto) {
        return objectMapper.convertValue(dto, ReimburseFormVO.class);
    }

    public ReimburseFormDTO toDTO(ReimburseFormVO vo) {
        return objectMapper.convertValue(vo, ReimburseFormDTO.class);
    }
}
