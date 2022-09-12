package com.wgq.service.impl;

import com.wgq.common.exception.BusinessException;
import com.wgq.common.exception.BusinessExceptionEnum;
import com.wgq.entity.TbMeetingRoom;
import com.wgq.mapper.TbMeetingRoomMapper;
import com.wgq.service.MeetingRoomService;
import com.wgq.utils.PageUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;

@Service
public class MeetingRoomServiceImpl implements MeetingRoomService {
    @Resource
    private TbMeetingRoomMapper meetingRoomDao;

    @Override
    public ArrayList<HashMap> searchAllMeetingRoom() {
        ArrayList<HashMap> list = meetingRoomDao.searchAllMeetingRoom();
        return list;
    }

    @Override
    public HashMap searchById(int id) {
        HashMap map = meetingRoomDao.searchById(id);
        return map;
    }

    @Override
    public ArrayList<String> searchFreeMeetingRoom(HashMap param) {
        ArrayList<String> list = meetingRoomDao.searchFreeMeetingRoom(param);
        return list;
    }

    @Override
    public PageUtil searchMeetingRoomByPage(HashMap param) {
        ArrayList<HashMap> list = meetingRoomDao.searchMeetingRoomByPage(param);
        long count = meetingRoomDao.searchMeetingRoomCount(param);
        int start = (Integer) param.get("start");
        int length = (Integer) param.get("length");
        PageUtil pageUtil = new PageUtil(list, count, start, length);
        return pageUtil;
    }

    @Override
    public int insert(TbMeetingRoom meetingRoom) {
        int rows = meetingRoomDao.insert1(meetingRoom);
        return rows;
    }

    @Override
    public int update(TbMeetingRoom meetingRoom) {
        int rows = meetingRoomDao.update1(meetingRoom);
        return rows;
    }

    @Override
    public int deleteMeetingRoomByIds(Integer[] ids) {
        if (!meetingRoomDao.searchCanDelete(ids)) {
            throw new BusinessException(BusinessExceptionEnum.CANNOT_DELETE_MEETINGROOM);
        }
        int rows = meetingRoomDao.deleteMeetingRoomByIds(ids);
        return rows;
    }

}
