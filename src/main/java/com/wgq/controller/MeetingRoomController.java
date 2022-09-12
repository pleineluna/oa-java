package com.wgq.controller;

import cn.hutool.json.JSONUtil;
import com.wgq.common.lang.Result;
import com.wgq.controller.form.*;
import com.wgq.entity.TbMeetingRoom;
import com.wgq.service.MeetingRoomService;
import com.wgq.utils.PageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * 会议管理Web接口
 */
@RestController
@RequestMapping("/meeting_room")
public class MeetingRoomController {
    @Autowired
    private MeetingRoomService meetingRoomService;

    /**
     * "查询所有会议室"
     * @return
     */
    @GetMapping("/searchAllMeetingRoom")
    public Result searchAllMeetingRoom() {
        ArrayList<HashMap> list = meetingRoomService.searchAllMeetingRoom();
        return Result.succ(list);
    }

    /**
     * "查询空闲会议室"
     * @param form
     * @return
     */
    @PostMapping("/searchFreeMeetingRoom")
    public Result searchFreeMeetingRoom(@Valid @RequestBody SearchFreeMeetingRoomForm form) {
        HashMap param = JSONUtil.parse(form).toBean(HashMap.class);
        ArrayList<String> list = meetingRoomService.searchFreeMeetingRoom(param);
        return Result.succ(list);
    }

    /**
     * 查询会议室分页数据
     * @param form
     * @return
     */
    @PostMapping("/searchMeetingRoomByPage")
    public Result searchMeetingRoomByPage(@Valid @RequestBody SearchMeetingRoomByPageForm form) {
        int page = form.getPage();
        int length = form.getLength();
        int start = (page - 1) * length;
        HashMap param = JSONUtil.parse(form).toBean(HashMap.class);
        param.put("start", start);
        PageUtil pageUtil = meetingRoomService.searchMeetingRoomByPage(param);
        return Result.succ(pageUtil);
    }

    /**
     * 添加会议室
     * @param form
     * @return
     */
    @PostMapping("/insert")
    @Transactional
    @PreAuthorize("hasAuthority('adm:meetingRoom:list:save')")
    public Result insert(@Valid @RequestBody InsertMeetingRoomForm form) {
        TbMeetingRoom meetingRoom = JSONUtil.parse(form).toBean(TbMeetingRoom.class);
        int rows = meetingRoomService.insert(meetingRoom);
        return Result.succ(rows);
    }

    /**
     * "根据ID查找会议室"
     * @param form
     * @return
     */
    @PostMapping("/searchById")
    public Result searchById(@Valid @RequestBody SearchMeetingRoomByIdForm form) {
        HashMap map = meetingRoomService.searchById(form.getId());
        return Result.succ(map);
    }

    /**
     * 修改会议室
     * @param form
     * @return
     */
    @PostMapping("/update")
    @Transactional
    @PreAuthorize("hasAuthority('adm:meetingRoom:list:update')")
    public Result update(@Valid @RequestBody UpdateMeetingRoomForm form) {
        TbMeetingRoom meetingRoom = JSONUtil.parse(form).toBean(TbMeetingRoom.class);
        int rows = meetingRoomService.update(meetingRoom);
        return Result.succ(rows);
    }

    /**
     * 删除会议室记录
     * @param form
     * @return
     */
    @PostMapping("/deleteMeetingRoomByIds")
    @Transactional
    @PreAuthorize("hasAuthority('adm:meetingRoom:list:delete')")
    public Result deleteMeetingRoomByIds(@Valid @RequestBody DeleteMeetingRoomByIdsForm form) {
        int rows = meetingRoomService.deleteMeetingRoomByIds(form.getIds());
        return Result.succ(rows);
    }
}
