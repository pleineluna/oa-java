package com.wgq.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wgq.entity.AdmNotice;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wgq.entity.dto.AdmNoticeDto;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 *
 */
public interface AdmNoticeMapper extends BaseMapper<AdmNotice> {

    AdmNotice selectNoticeByUuid(@Param("uuid") String uuid);

    List<AdmNotice> getCollegeNotice(@Param("guiderId") Long guiderId);

    List<AdmNotice> getLeaveNoticeListByReceiverId(Long receiverId);

    List<AdmNotice> getMeetingNoticeListByReceiverId(Long receiverId);

    Page<AdmNoticeDto> selectNoticeByRealname(Page<?> page, @Param("realname") String realname, @Param("type") String type);

    AdmNoticeDto selectAdmNoticeDtoById(Long id);

    List<AdmNotice> getScholarshipNoticeListByReceiverId(Long receiverId);
}
