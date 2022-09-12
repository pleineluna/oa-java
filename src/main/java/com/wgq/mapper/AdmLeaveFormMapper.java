package com.wgq.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wgq.entity.dto.AdmLeaveFormInfoDto;
import com.wgq.entity.AdmLeaveForm;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 *  Mapper 接口
 */
public interface AdmLeaveFormMapper extends BaseMapper<AdmLeaveForm> {

    IPage<AdmLeaveFormInfoDto> selectPageVo(Page<AdmLeaveFormInfoDto> page, @Param("idList") List<Long> idList);

    AdmLeaveFormInfoDto getLeaveFormById(Long id);

    Page<AdmLeaveFormInfoDto> selectAllLeaveForm(Page<?> page, @Param("type") String type, @Param("realname") String realname);

    AdmLeaveFormInfoDto selectLeaveFormById(Long id);

}
