package com.wgq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wgq.entity.dto.TbMeetingDto;
import com.wgq.entity.TbMeeting;
import org.apache.ibatis.annotations.Param;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public interface TbMeetingMapper extends BaseMapper<TbMeeting> {

    public boolean searchMeetingMembersInSameDept(String uuid);

    public HashMap searchMeetingById(HashMap param);

    public ArrayList<HashMap> searchOfflineMeetingByPage(HashMap param);

    public long searchOfflineMeetingCount(HashMap param);

    public int updateMeetingInstanceId(HashMap param);

    public int insert1(TbMeeting meeting);

    public ArrayList<HashMap> searchOfflineMeetingInWeek(HashMap param);

    public HashMap searchMeetingInfo(long id);

    public HashMap searchCurrentMeetingInfo(long id);

    public int deleteMeetingApplication(HashMap param);

    public ArrayList<HashMap> searchOnlineMeetingByPage(HashMap param);

    public long searchOnlineMeetingCount(HashMap param);

    public ArrayList<HashMap> searchOnlineMeetingMembers(HashMap param);

    public long searchCanCheckinMeeting(HashMap param);

    public int updateMeetingPresent(HashMap param);

    IPage<TbMeetingDto> selectPageVo(Page<TbMeetingDto> page, @Param("idList") List<Long> idList);

    TbMeetingDto getTbMeetingDtoById(Long id);

    TbMeeting searchById(Long id);

    int updateStatusById(@Param("id") Long id,@Param("status") int status);
}