package com.wgq.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wgq.entity.dto.AdmLeaveFormInfoDto;
import com.wgq.common.lang.Result;
import com.wgq.entity.AdmLeaveForm;
import com.wgq.entity.SysUser;
import com.wgq.mapper.AdmLeaveFormMapper;
import com.wgq.utils.Activiti7Util;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.security.Principal;
import java.util.*;

/**
 * 前端控制器
 */
@RestController
@RequestMapping("/adm/leave/form")
public class AdmLeaveFormController extends BaseController {

    @Resource
    AdmLeaveFormMapper admLeaveFormMapper;

    @Resource
    Activiti7Util activiti7Util;

    /**
     * 新增请假单
     *
     * @param admLeaveForm 请假单实体对象
     * @param principal
     * @return
     */
    @PostMapping("/create")
    @Transactional
    public Result create(@Validated @RequestBody AdmLeaveForm admLeaveForm
            , Principal principal) {
        SysUser sysUser = sysUserService.getByUsername(principal.getName());
        admLeaveForm.setUserId(sysUser.getId());
        admLeaveFormService.create(admLeaveForm);
        return Result.succ("请假申请成功");
    }

    /**
     * 根据当前登录用户获取班级/职位信息以及申请人真实姓名realname
     *
     * @return
     */
    @GetMapping("/baseInfo")
    public Result baseInfo(Principal principal) {
        SysUser sysUser = sysUserService.getByUsername(principal.getName());
        Map map = admLeaveFormService.baseInfo(sysUser);
        return Result.succ(map);
    }

    /**
     * 查询我的请假单
     */
    @GetMapping("/myList")
    public Result myList(Principal principal) {
        List<AdmLeaveForm> myLeaveList = admLeaveFormService.list(new QueryWrapper<AdmLeaveForm>()
                .eq("user_id", sysUserService.getByUsername(principal.getName()).getId()).orderByDesc("created"));
        return Result.succ(myLeaveList);
    }

    /**
     * 【查询需要审核的】请假单列表--经办人为当前登录用户
     * 用activiti就是查询当前登录用户的所有代办任务
     */
    @GetMapping("/list")
    public Result list(Principal principal, String realname) {
        SysUser sysUser = sysUserService.getByUsername(principal.getName());
        //所有待办任务的businessKey
        List<Long> businessKeys = new ArrayList<>();
        //当前登录用户的所有代办任务。
        List<Map<String, Object>> myTaskList = activiti7Util.myTaskList(sysUser.getId().toString(),"leave");

        System.out.println("*realname:" + realname);
        //如果传来了搜索关键词realname，就要对当前登录用户的所有代办任务myTaskList进行筛选再拿到businessKeys。
        if (!realname.equals("")) {
            for (Map<String, Object> map : myTaskList) {
                AdmLeaveFormInfoDto admLeaveFormInfoDto = admLeaveFormMapper.getLeaveFormById(Long.parseLong(map.get("businessKey").toString()));
                if (admLeaveFormInfoDto != null) {
                    if (admLeaveFormInfoDto.getRealname().contains(realname)) {
                        admLeaveFormInfoDto.setTaskId(map.get("taskId").toString());
                        System.out.println("*businessKey:" + Long.parseLong(map.get("businessKey").toString()));
                        businessKeys.add(Long.parseLong(map.get("businessKey").toString()));
                    }
                }
            }
        } else {
            for (Map<String, Object> map : myTaskList) {
                AdmLeaveFormInfoDto admLeaveFormInfoDto = admLeaveFormMapper.getLeaveFormById(Long.parseLong(map.get("businessKey").toString()));
                if (admLeaveFormInfoDto != null) {
                    admLeaveFormInfoDto.setTaskId(map.get("taskId").toString());
                    System.out.println("*businessKey:" + Long.parseLong(map.get("businessKey").toString()));
                    businessKeys.add(Long.parseLong(map.get("businessKey").toString()));
                }
            }
        }
        System.out.println("*businessKeys:" + businessKeys);
        if (businessKeys.size() == 0) {
            businessKeys.add(null);
        }
        //根据businessKeys【也就是adm_leave_form的主键】查到所有的AdmLeaveFormInfoDto
        IPage iPage = admLeaveFormService.getLeaveFormList(getPage(), businessKeys);
        return Result.succ(iPage);
    }

    /**
     * “审批”业务数据回填用
     * 得到对应id的请假单信息
     */
    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long id, Principal principal) {
        SysUser sysUser = sysUserService.getByUsername(principal.getName());
        AdmLeaveFormInfoDto leaveFormById = admLeaveFormService.getLeaveFormById(id, sysUser.getId());
        return Result.succ(leaveFormById);
    }

    /**
     * 审批
     * 整合activiti后对应complete任务。
     * 如果某个流程实例结束，要对adm_leave_form业务表进行状态的设置
     */
    @GetMapping("/audit/{taskId}")
    @PreAuthorize("hasAuthority('adm:examine:list:examine')")
    public Result audit(@PathVariable(name = "taskId") String taskId,
                        String result, String review, Principal principal) {
        SysUser sysUser = sysUserService.getByUsername(principal.getName());
        admLeaveFormService.audit(taskId, sysUser.getId(), result, review);
        return Result.succ("审批成功");
    }

    /**
     * 得到所有的请假单信息。与之前不同的是不单针对需要审批的，而是得到所有状态的请假单信息 leave_management.vue模块
     *
     * @param type
     * @param realname
     * @return
     */
    @GetMapping("/allList")
    public Result allList(String type, String realname) {
        Page<AdmLeaveFormInfoDto> page = admLeaveFormMapper
                .selectAllLeaveForm(getPage(), type, realname);
        return Result.succ(page);
    }

    /**
     * 得到某个id的请假单信息 leave_management.vue模块
     * @param id
     * @return
     */
    @GetMapping("/infoForManagementModel")
    public Result infoForManagementModel(Long id) {
        AdmLeaveFormInfoDto admLeaveFormInfoDto = admLeaveFormService.infoForManagementModel(id);
        return Result.succ(admLeaveFormInfoDto);
    }

    /**
     * 删除请假单信息 leave_management.vue模块
     * @param ids
     * @return
     */
    @PostMapping("/delete")
    @Transactional//这种写操作一定要开启事务
    @PreAuthorize("hasAuthority('adm:leave:management:delete')")
    public Result delete(@RequestBody Long[] ids) {
        //Arrays.asList() 数组转列表 因为mybatis-plus的removeByIds方法参数是list类型
        admLeaveFormService.removeByIds(Arrays.asList(ids));
        return Result.succ("删除成功");
    }

}
