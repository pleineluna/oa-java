package com.wgq.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wgq.entity.dto.TbMeetingDto;
import com.wgq.entity.TbMeeting;
import com.wgq.utils.PageUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public interface MeetingService {
    public PageUtil searchOfflineMeetingByPage(HashMap param);

    public int insert(TbMeeting meeting);

    public ArrayList<HashMap> searchOfflineMeetingInWeek(HashMap param);

    public HashMap searchMeetingInfo(short status, long id);

    public int deleteMeetingApplication(HashMap param);

    IPage<TbMeetingDto> searchTaskByPage(Page<TbMeetingDto> page, List<Long> idList);

    void audit(String taskId, Long operatorId, String result, String review);

    public PageUtil searchOnlineMeetingByPage(HashMap param);

    public Long searchRoomIdByUUID(String uuid);

    public ArrayList<HashMap> searchOnlineMeetingMembers(HashMap param);

    public boolean searchCanCheckinMeeting(HashMap param);

    public int updateMeetingPresent(HashMap param);
}
