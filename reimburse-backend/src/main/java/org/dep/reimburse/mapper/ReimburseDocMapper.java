package org.dep.reimburse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.dep.reimburse.dto.ReimburseListQueryDTO;
import org.dep.reimburse.entity.ReimburseDoc;

@Mapper
public interface ReimburseDocMapper extends BaseMapper<ReimburseDoc> {

    Page<ReimburseDoc> selectPageByQuery(Page<ReimburseDoc> page, @Param("query") ReimburseListQueryDTO query);
}
