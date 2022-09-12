package com.wgq.controller;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateRange;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wgq.entity.dto.TbMeetingDto;
import com.wgq.common.lang.Result;
import com.wgq.utils.Activiti7Util;
import com.wgq.utils.TrtcUtil;
import com.wgq.controller.form.*;
import com.wgq.entity.SysUser;
import com.wgq.entity.TbMeeting;
import com.wgq.mapper.SysUserMapper;
import com.wgq.mapper.TbMeetingMapper;
import com.wgq.service.MeetingService;
import com.wgq.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.TaskService;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/meeting")
@Slf4j
public class MeetingController extends BaseController {

    @Value("${tencent.trtc.appId}")
    private int appId;

    @Resource
    private TrtcUtil trtcUtil;

    @Resource
    private MeetingService meetingService;

    @Resource
    TbMeetingMapper tbMeetingMapper;

    @Resource
    SysUserMapper sysUserMapper;

    @Resource
    Activiti7Util activiti7Util;

    @Resource
    TaskService taskService;

    /**
     * 查询线下会议的分页数据
     * @param form
     * @param principal
     * @return
     */
    @PostMapping("/searchOfflineMeetingByPage")
    public Result searchOfflineMeetingByPage(@Valid @RequestBody SearchOfflineMeetingByPageForm form, Principal principal) {
        SysUser sysUser = sysUserService.getByUsername(principal.getName());
        int page = form.getPage();
        int length = form.getLength();
        int start = (page - 1) * length;
        HashMap param = new HashMap() {{
            put("date", form.getDate());
            put("mold", form.getMold());
            put("userId", sysUser.getId());
            put("start", start);
            put("length", length);
        }};
        PageUtil pageUtil = meetingService.searchOfflineMeetingByPage(param);
        return Result.succ(pageUtil);
    }

    /**
     * 创建会议
     * @param form
     * @param principal
     * @return
     */
    @PostMapping("/insert")
    @Transactional
    @PreAuthorize("hasAnyAuthority('adm:OfflineMeeting:list:apply','adm:OnlineMeeting:list:apply')")
    public Result insert(@Validated @RequestBody InsertMeetingForm form, Principal principal) {
        SysUser sysUser = sysUserService.getByUsername(principal.getName());
        DateTime start = DateUtil.parse(form.getDate() + " " + form.getStart());
        DateTime end = DateUtil.parse(form.getDate() + " " + form.getEnd());
        if (start.isAfterOrEquals(end)) {
            return Result.fail("结束时间必须大于开始时间");
        } else if (new DateTime().isAfterOrEquals(start)) {
            return Result.fail("会议开始时间不能早于当前时间");
        }
        TbMeeting meeting = JSONUtil.parse(form).toBean(TbMeeting.class);
        meeting.setUuid(UUID.randomUUID().toString(true));
        //CreatorId就是会议申请人的id
        meeting.setCreatorId(Integer.parseInt(sysUser.getId().toString()));
        meeting.setStatus((short) 1);
        int rows = meetingService.insert(meeting);
        return Result.succ(rows);
    }

    /**
     * 查询某个会议室一周的会议
     * @param form
     * @param principal
     * @return
     */
    @PostMapping("/searchOfflineMeetingInWeek")
    public Result searchOfflineMeetingInWeek(@Valid @RequestBody SearchOfflineMeetingInWeekForm form, Principal principal) {
        SysUser sysUser = sysUserService.getByUsername(principal.getName());
        Map map = new HashMap<>();
        String date = form.getDate();
        DateTime startDate, endDate;
        if (date != null && date.length() > 0) {
            startDate = DateUtil.parseDate(date);
            endDate = startDate.offsetNew(DateField.DAY_OF_WEEK, 6);
        } else {
            startDate = DateUtil.beginOfWeek(new Date());
            endDate = DateUtil.endOfWeek(new Date());
        }
        HashMap param = new HashMap() {{
            put("place", form.getName());
            put("startDate", startDate.toDateStr());
            put("endDate", endDate.toDateStr());
            put("mold", form.getMold());
            put("userId", sysUser.getId());
        }};
        ArrayList<HashMap> list = meetingService.searchOfflineMeetingInWeek(param);
        ArrayList days = new ArrayList();
        DateRange range = DateUtil.range(startDate, endDate, DateField.DAY_OF_WEEK);
        range.forEach(one -> {
            JSONObject json = new JSONObject();
            json.set("date", one.toString("MM/dd"));
            json.set("day", one.dayOfWeekEnum().toChinese("周"));
            days.add(json);
        });
        map.put("list", list);
        map.put("days", days);
        return Result.succ(map);
    }

    /**
     * 查询会议信息
     * @param form
     * @return
     */
    @PostMapping("/searchMeetingInfo")
    public Result searchMeetingInfo(@Valid @RequestBody SearchMeetingInfoForm form){
        HashMap map=meetingService.searchMeetingInfo(form.getStatus(),form.getId());
        return Result.succ(map);
    }

    /**
     * 删除会议申请
     * @param form
     * @param principal
     * @return
     */
    @PostMapping("/deleteMeetingApplication")
    @Transactional
    @PreAuthorize("hasAnyAuthority('adm:OnlineMeeting:list:delete')")
    public Result deleteMeetingApplication(@Valid @RequestBody DeleteMeetingApplicationForm form,Principal principal){
        HashMap param = JSONUtil.parse(form).toBean(HashMap.class);
        SysUser sysUser = sysUserService.getByUsername(principal.getName());
        param.put("creatorId",sysUser.getId());
        param.put("userId",sysUser.getId());
        int rows=meetingService.deleteMeetingApplication(param);
        return Result.succ(rows);
    }

    /**
     * 查询分页任务列表
     * @param principal
     * @param realname
     * @return
     */
    @PostMapping("/searchTaskByPage")
    public Result searchTaskByPage(Principal principal,String realname) {
        SysUser sysUser = sysUserService.getByUsername(principal.getName());
        List<Map<String, Object>> myTaskList = activiti7Util.myTaskList(sysUser.getId().toString(),"meeting");
        //所有待办任务的businessKey
        List<Long> businessKeys = new ArrayList<>();
        //拿到所有任务的businessKey
        //如果传来了搜索关键词realname，就要对当前登录用户的所有代办任务myTaskList进行筛选再拿到businessKeys。
        if (!realname.equals("")) {
            for (Map<String, Object> map : myTaskList) {
                TbMeetingDto tbMeetingDto = tbMeetingMapper.getTbMeetingDtoById(Long.parseLong(map.get("businessKey").toString()));
                if (tbMeetingDto!=null){
                    if (tbMeetingDto.getRealname().contains(realname)) {
                        tbMeetingDto.setTaskId(map.get("taskId").toString());
                        System.out.println("*businessKey:"+Long.parseLong(map.get("businessKey").toString()));
                        businessKeys.add(Long.parseLong(map.get("businessKey").toString()));
                    }
                }
            }
        }else {
            for (Map<String, Object> map : myTaskList) {
                TbMeetingDto tbMeetingDto = tbMeetingMapper.getTbMeetingDtoById(Long.parseLong(map.get("businessKey").toString()));
                if (tbMeetingDto != null) {
                    tbMeetingDto.setTaskId(map.get("taskId").toString());
                    System.out.println("*businessKey:" + Long.parseLong(map.get("businessKey").toString()));
                    businessKeys.add(Long.parseLong(map.get("businessKey").toString()));
                }
            }
        }
        System.out.println("*businessKeys:"+businessKeys);
        if (businessKeys.size()==0) {
            businessKeys.add(null);
        }

        //根据businessKeys【也就是adm_leave_form的主键】查到所有的AdmLeaveFormInfoDto
        IPage iPage = meetingService.searchTaskByPage(getPage(), businessKeys);
        List<TbMeetingDto> TbMeetingDtoList=iPage.getRecords();
        for (TbMeetingDto tbMeetingDto : TbMeetingDtoList) {
            tbMeetingDto.setCreateTime(tbMeetingDto.getCreateTime().toString().substring(0,10));
            switch (tbMeetingDto.getType()) {
                case 1:
                    tbMeetingDto.setMeetingType("在线会议");
                case 2:
                    tbMeetingDto.setMeetingType("线下会议");
            }
            switch (tbMeetingDto.getStatus()) {
                case 1:tbMeetingDto.setStatusType("申请中");
                case 2:tbMeetingDto.setStatusType("审批未通过");
                case 3:tbMeetingDto.setStatusType("审批通过");
                case 4:tbMeetingDto.setStatusType("会议进行中");
                case 5:tbMeetingDto.setStatusType("会议结束");
            }
        }
        iPage.setRecords(TbMeetingDtoList);
        return Result.succ(iPage);
    }

    /**
     * 查询某个任务详情
     * @param id
     * @param principal
     * @return
     */
    @PostMapping("/searchApprovalContent/{id}")
    public Result searchApprovalContent(@PathVariable("id") Long id, Principal principal) {
        TbMeetingDto tbMeetingDto = tbMeetingMapper.getTbMeetingDtoById(id);
        Task task = taskService.createTaskQuery().processInstanceId(tbMeetingDto.getInstanceId()).active().singleResult();
        String membersStr  = tbMeetingDto.getMembers();
        membersStr = membersStr.substring(1, membersStr.length() - 1);
        String[] membersSplits = membersStr.split(",");
        StringBuffer membersToShow = new StringBuffer();
        for (String memberId : membersSplits) {
            SysUser one = sysUserMapper.selectOne(new QueryWrapper<SysUser>().eq("id", memberId));
            membersToShow.append(one.getRealname()).append(" ");
        }
        tbMeetingDto.setMembers(membersToShow.toString());
        tbMeetingDto.setTaskId(task.getId());
        return Result.succ(tbMeetingDto);
    }

    /**
     * 审批
     * 整合activiti后对应complete任务。
     * 如果某个流程实例结束，要对业务表进行状态status的更新
     */
    @GetMapping("/audit/{taskId}")
    @Transactional
    @PreAuthorize("hasAuthority('adm:approval:list:approval')")
    public Result audit(@PathVariable(name = "taskId") String taskId, String result, String review, Principal principal) {
        SysUser sysUser = sysUserService.getByUsername(principal.getName());
        meetingService.audit(taskId, sysUser.getId(), result, review);
        return Result.succ("审批成功");
    }

    /**
     * 查询线上会议分页数据
     * @param form
     * @param principal
     * @return
     */
    @PostMapping("/searchOnlineMeetingByPage")
    public Result searchOnlineMeetingByPage(@Valid @RequestBody SearchOnlineMeetingByPageForm form, Principal principal) {
        SysUser sysUser = sysUserService.getByUsername(principal.getName());
        int page = form.getPage();
        int length = form.getLength();
        int start = (page - 1) * length;
        HashMap param = new HashMap() {{
            put("date", form.getDate());
            put("mold", form.getMold());
            put("userId", sysUser.getId());
            put("start", start);
            put("length", length);
        }};
        PageUtil pageUtil = meetingService.searchOnlineMeetingByPage(param);
        return Result.succ(pageUtil);
    }

    /**
     * 获取用户签名
     * @param principal
     * @return
     */
    @GetMapping("/searchMyUserSig")
    public Result searchMyUserSig(Principal principal) {
        SysUser sysUser = sysUserService.getByUsername(principal.getName());
        String userSig = trtcUtil.genUserSig(sysUser.getId().toString());
        Map param = new HashMap();
        param.put("userSig", userSig);
        param.put("userId", sysUser.getId().intValue());
        param.put("appId", appId);
        return Result.succ(param);
    }

    /**
     * 查询视频会议室RoomId
     * @param form
     * @return
     */
    @PostMapping("/searchRoomIdByUUID")
    public Result searchRoomIdByUUID(@Valid @RequestBody SearchRoomIdByUUIDForm form){
        Long roomId=meetingService.searchRoomIdByUUID(form.getUuid());
        System.out.println("*roomId:::"+roomId);
        return Result.succ(roomId);
    }

    /**
     * 查询线下会议成员
     * @param form
     * @param principal
     * @return
     */
    @PostMapping("/searchOnlineMeetingMembers")
    public Result searchOnlineMeetingMembers(@Valid @RequestBody SearchOnlineMeetingMembersForm form, Principal principal) {
        SysUser sysUser = sysUserService.getByUsername(principal.getName());
        HashMap param = JSONUtil.parse(form).toBean(HashMap.class);
        param.put("userId", sysUser.getId());
        ArrayList<HashMap> list = meetingService.searchOnlineMeetingMembers(param);
        return Result.succ(list);
    }

    /**
     * 执行会议签到
     * @param form
     * @param principal
     * @return
     */
    @PostMapping("/updateMeetingPresent")
    @Transactional
    public Result updateMeetingPresent(@Valid @RequestBody UpdateMeetingPresentForm form, Principal principal) {
        SysUser sysUser = sysUserService.getByUsername(principal.getName());
        HashMap param = new HashMap() {{
            put("meetingId", form.getMeetingId());
            put("userId", sysUser.getId());
        }};
        boolean bool = meetingService.searchCanCheckinMeeting(param);
        if (bool) {
            int rows = meetingService.updateMeetingPresent(param);
            return Result.succ(rows);
        }
        return Result.succ(0);
    }
}
