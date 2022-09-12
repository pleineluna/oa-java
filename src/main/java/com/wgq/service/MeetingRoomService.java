package com.wgq.service;


import com.wgq.entity.TbMeetingRoom;
import com.wgq.utils.PageUtil;

import java.util.ArrayList;
import java.util.HashMap;

public interface MeetingRoomService {
    public ArrayList<HashMap> searchAllMeetingRoom();

    public HashMap searchById(int id);

    public ArrayList<String> searchFreeMeetingRoom(HashMap param);

    public PageUtil searchMeetingRoomByPage(HashMap param);

    public int insert(TbMeetingRoom meetingRoom);

    public int update(TbMeetingRoom meetingRoom);

    public int deleteMeetingRoomByIds(Integer[] ids);


}
