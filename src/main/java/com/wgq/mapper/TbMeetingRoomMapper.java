package com.wgq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wgq.entity.TbMeetingRoom;

import java.util.ArrayList;
import java.util.HashMap;

public interface TbMeetingRoomMapper extends BaseMapper<TbMeetingRoom> {
    public ArrayList<HashMap> searchAllMeetingRoom();

    public HashMap searchById(int id);

    public ArrayList<String> searchFreeMeetingRoom(HashMap param);

    public ArrayList<HashMap> searchMeetingRoomByPage(HashMap param);

    public long searchMeetingRoomCount(HashMap param);

    public int insert1(TbMeetingRoom meetingRoom);

    public int update1(TbMeetingRoom meetingRoom);

    public boolean searchCanDelete(Integer[] ids);

    public int deleteMeetingRoomByIds(Integer[] ids);

}
