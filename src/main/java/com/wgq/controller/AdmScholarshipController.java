package com.wgq.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wgq.common.lang.Result;
import com.wgq.controller.form.ApplyScholarshipForm;
import com.wgq.entity.AdmScholarship;
import com.wgq.entity.AdmScholarshipSysUser;
import com.wgq.entity.SysUser;
import com.wgq.entity.dto.AdmScholarshipSysUserDto;
import com.wgq.utils.Activiti7Util;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Description:
 */
@RestController
@RequestMapping("/adm/scholarship")
public class AdmScholarshipController extends BaseController {

    @Resource
    Activiti7Util activiti7Util;

    /**
     * “奖学金类别管理”模块  得到所有类别奖学金
     *
     * @param name
     * @return
     */
    @GetMapping("/scholarshipList")
    public Result scholarshipList(String name) {
        IPage iPage = admScholarshipService.scholarshipList(getPage(), name);
        return Result.succ(iPage);
    }

    /**
     * “奖学金申请”模块用   得到所有开放状态的（可申请）的奖学金类别
     *
     * @param name
     * @return
     */
    @GetMapping("/scholarshipListStatuIsOpen")
    public Result scholarshipListStatuIsOpen(String name) {
        List<AdmScholarship> admScholarshipList = admScholarshipService.scholarshipListStatuIsOpen(name);
        return Result.succ(admScholarshipList);
    }

    /**
     * 得到奖学金详情信息
     *
     * @param id
     * @return
     */
    @GetMapping("/scholarshipInfo")
    public Result scholarshipInfo(Long id) {
        AdmScholarship admScholarship = admScholarshipService.scholarshipInfo(id);
        return Result.succ(admScholarship);
    }

    /**
     * “奖学金类别管理”新建奖学金类别
     *
     * @param admScholarship
     * @return
     */
    @PostMapping("/create")
    @Transactional
    @PreAuthorize("hasAuthority('adm:scholarship:category:management:save')")
    public Result create(@RequestBody AdmScholarship admScholarship) {
        admScholarshipService.createScholarshipCategory(admScholarship);
        return Result.succ("新建奖学金类别成功！");
    }

    /**
     * “奖学金类别管理”更新奖学金类别
     *
     * @param admScholarship
     * @return
     */
    @PostMapping("/update")
    @Transactional
    @PreAuthorize("hasAuthority('adm:scholarship:category:management:update')")
    public Result update(@RequestBody AdmScholarship admScholarship) {
        boolean b = admScholarshipService.updateById(admScholarship);
        return Result.succ(b);
    }

    /**
     * “奖学金申请模块”申请奖学金
     *
     * @param form
     * @param principal
     * @return
     */
    @PostMapping("/apply")
    @Transactional
    @PreAuthorize("hasAuthority('adm:scholarship:apply:apply')")
    public Result apply(@RequestBody ApplyScholarshipForm form, Principal principal) {
        SysUser sysUser = sysUserService.getByUsername(principal.getName());
        admScholarshipService.apply(form.getApplyDesc(), form.getScholarshipId(), sysUser.getId());
        return Result.succ("");
    }

    /**
     * “奖学金类别管理”删除奖学金类别
     * * @param ids
     *
     * @return
     */
    @PostMapping("/deleteScholarshipByIds")
    @Transactional
    @PreAuthorize("hasAuthority('adm:scholarship:category:management:delete')")
    public Result deleteScholarshipByIds(@RequestBody Long[] ids) {
        boolean b = admScholarshipService.removeByIds(Arrays.asList(ids));
        return Result.succ(b);
    }

    /**
     * “奖学金审批”模块  得到所有当前系统登录用户待审批的任务
     *
     * @param principal
     * @param realname  奖学金申请人的姓名，是条件搜索。
     * @return
     */
    @GetMapping("/scholarshipListNeedToExamine")
    public Result scholarshipListNeedToExamine(Principal principal, String realname) {
        SysUser sysUser = sysUserService.getByUsername(principal.getName());
        //所有待办任务的businessKey
        List<Long> businessKeys = new ArrayList<>();
        //当前登录用户的所有代办任务。
        List<Map<String, Object>> myTaskList = activiti7Util.myTaskList(sysUser.getId().toString(), "scholarship");
        //如果传来了搜索关键词realname，就要对当前登录用户的所有代办任务myTaskList进行筛选再拿到businessKeys。
        if (!realname.equals("")) {
            for (Map<String, Object> map : myTaskList) {
                AdmScholarshipSysUser scholarshipSysUser = admScholarshipSysUserService.getById(Long.parseLong(map.get("businessKey").toString()));
                SysUser applyUser = sysUserService.getById(scholarshipSysUser.getUserId());
                if (scholarshipSysUser != null) {
                    if (applyUser.getRealname().contains(realname)) {
                        scholarshipSysUser.setTaskId(map.get("taskId").toString());
                        System.out.println("*businessKey:" + Long.parseLong(map.get("businessKey").toString()));
                        businessKeys.add(Long.parseLong(map.get("businessKey").toString()));
                    }
                }
            }
        } else {
            for (Map<String, Object> map : myTaskList) {
                AdmScholarshipSysUser scholarshipSysUser = admScholarshipSysUserService.getById(Long.parseLong(map.get("businessKey").toString()));
                if (scholarshipSysUser != null) {
                    scholarshipSysUser.setTaskId(map.get("taskId").toString());
                    System.out.println("*businessKey:" + Long.parseLong(map.get("businessKey").toString()));
                    businessKeys.add(Long.parseLong(map.get("businessKey").toString()));
                }
            }
        }
        System.out.println("*businessKeys:" + businessKeys);
        if (businessKeys.size() == 0) {
            businessKeys.add(null);
        }
        /**
         * 根据businessKeys【也就是adm_scholarship_sys_user的主键】查到所有的AdmScholarshipSysUser
         * 因为奖学金模块的特殊性，我查到的申请表（其实是奖学金和用户的关联表），里面包括了申请人id、申请奖学金类别id、申请原因、申请结果。我前台审批页面需要的信息有申请人信息，申请人申请的奖学金类别信息、奖学金和用户的关联表的申请原因、申请时间信息。审批过程中还要设置申请表的result结果字段。所以呢，我需要返回给前台的数据是以上这些。所以我根据查询当前登录用户的正在执行任务之scholarship得到所有task，根据task拿到businessKey也就是申请表的id，这样我就能拿到这次申请的申请人信息和申请人申请的奖学金类别信息，和申请表的申请原因、申请时间一起然后返回给前端。
         */
        IPage iPage = admScholarshipSysUserService.scholarshipListNeedToExamine(getPage(), businessKeys);
        return Result.succ(iPage);
    }

    /**
     * “奖学金审批”模块 “审批业务数据回填用”
     * 获取指定id的信息以及taskId
     */
    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long id, Principal principal) {
        SysUser sysUser = sysUserService.getByUsername(principal.getName());
        AdmScholarshipSysUserDto dto = admScholarshipSysUserService.scholarshipNeedToExamineById(id, sysUser.getId());
        return Result.succ(dto);
    }

    /**
     * “奖学金审批”模块 “审批”
     * 整合activiti后对应complete任务。
     * 如果某个流程实例结束，要对adm_scholarship_sys_user业务表进行状态的设置
     */
    @GetMapping("/audit/{taskId}")
    @Transactional
    public Result audit(@PathVariable(name = "taskId") String taskId, String result, String review, Principal principal) {
        SysUser sysUser = sysUserService.getByUsername(principal.getName());
        admScholarshipSysUserService.audit(taskId, sysUser.getId(), result, review);
        return Result.succ("审批成功");
    }


    /**
     * “我的奖学金”模块用   得到我申请的所有奖学金申请信息
     * @param name
     * @return
     */
    @GetMapping("/Myscholarship")
    public Result Myscholarship(String name, Principal principal) {
        SysUser sysUser = sysUserService.getByUsername(principal.getName());
        List<AdmScholarshipSysUserDto> myScholarshipList = admScholarshipSysUserService.Myscholarship(name, sysUser.getId());
        return Result.succ(myScholarshipList);
    }

    /**
     * “我的奖学金”模块用   根据adm_scholarship_sys_user的id得到我申请的某个奖学金申请信息
     * “奖学金管理”模块用   根据adm_scholarship_sys_user的id得到申请的某个奖学金申请信息
     */
    @GetMapping("/Myscholarshipinfo/{id}")
    public Result Myscholarshipinfo(@PathVariable("id") Long id) {
        AdmScholarshipSysUserDto dto = admScholarshipSysUserService.myScholarshipinfo(id);
        return Result.succ(dto);
    }

    /**
     * 动态获取奖学金类别给下拉框选项
     * @return
     */
    @GetMapping("/getScholarshipCategoryList")
    public Result getScholarshipCategoryList() {
        List<AdmScholarship> admScholarships = admScholarshipService.list();
        return Result.succ(admScholarships);
    }

    /**
     * @param realname 奖学金申请人的姓名，是条件搜索。
     * @param type     奖学金类别，是条件搜索
     * @return
     */
    @GetMapping("/getApplyScholarshipList")
    public Result getApplyScholarshipList(String realname, String type) {
        IPage iPage = admScholarshipSysUserService.getApplyScholarshipList(getPage(), realname, type);
        return Result.succ(iPage);
    }

    @PostMapping("/delete")
    @Transactional//这种写操作一定要开启事务
    @PreAuthorize("hasAuthority('adm:scholarship:application:management:delete')")
    public Result delete(@RequestBody Long[] ids) {
        //Arrays.asList() 数组转列表 因为mybatis-plus的removeByIds方法参数是list类型
        boolean b = admScholarshipSysUserService.removeByIds(Arrays.asList(ids));
        return Result.succ(b);
    }

}
