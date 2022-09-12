package com.wgq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wgq.common.exception.BusinessException;
import com.wgq.common.exception.BusinessExceptionEnum;
import com.wgq.entity.*;
import com.wgq.mapper.*;
import com.wgq.service.AdmScholarshipService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wgq.utils.Activiti7Util;
import com.wgq.utils.MailUtil;
import org.activiti.engine.runtime.ProcessInstance;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdmScholarshipServiceImpl extends ServiceImpl<AdmScholarshipMapper, AdmScholarship> implements AdmScholarshipService {
    @Resource
    AdmScholarshipMapper admScholarshipMapper;

    @Resource
    SysUserMapper sysUserMapper;

    @Resource
    AdmScholarshipSysUserMapper admScholarshipSysUserMapper;

    @Resource
    Activiti7Util activiti7Util;

    @Resource
    AdmNoticeMapper admNoticeMapper;

    @Resource
    AdmClassMapper admClassMapper;

    @Resource
    MailUtil mailUtil;

    @Override
    public IPage<AdmScholarship> scholarshipList(Page<AdmScholarship> page, String name) {
        IPage<AdmScholarship> admScholarshipIPage = admScholarshipMapper.searchByKeyWord(page, name);
        return admScholarshipIPage;
    }

    @Override
    public List<AdmScholarship> scholarshipListStatuIsOpen(String name) {
        List<AdmScholarship> admScholarshipList = admScholarshipMapper.searchByKeyWordStatuIsOpen(name);
        return admScholarshipList;
    }

    @Override
    public AdmScholarship scholarshipInfo(Long id) {
        AdmScholarship admScholarship = admScholarshipMapper.searchById(id);
        return admScholarship;
    }

    @Override
    public void createScholarshipCategory(AdmScholarship admScholarship) {
        AdmScholarship admScholarshipNew = new AdmScholarship();
        BeanUtils.copyProperties(admScholarship, admScholarshipNew);
        admScholarshipNew.setCreated(LocalDateTime.now());
        admScholarshipNew.setUpdated(LocalDateTime.now());
        int result = admScholarshipMapper.insert(admScholarshipNew);
        if (result != 1) {
            throw new BusinessException(BusinessExceptionEnum.FAILED_TO_CREATE_SCHOLARSHIP_CATEGORY);
        }
    }

    @Override
    public void apply(String applyDesc, Long scholarshipId, Long userId) {
        //先检查是否重复申请此奖学金
        AdmScholarshipSysUser isExistAdmScholarshipSysUser = admScholarshipSysUserMapper.selectOne(new QueryWrapper<AdmScholarshipSysUser>().eq("scholarship_id", scholarshipId).eq("user_id", userId));
        if (isExistAdmScholarshipSysUser!=null) {
            throw new BusinessException(BusinessExceptionEnum.REPEAT_APPLY_SCHOLARSHIP);
        }
        //本次【奖学金申请】所对应的用户、用户的导员、书记、班级、要申请的奖学金类别
        SysUser sysUser = sysUserMapper.selectById(userId);//必能查到
        SysUser sysGuiderUser = sysUserMapper.getGuideUserByUserId(userId);//因为只有学生能申请奖学金，所以比能查到导员
        SysUser sysSecretaryUser = sysUserMapper.getSecretary();//原理同上
        AdmClass admClass = admClassMapper.selectOne(new QueryWrapper<AdmClass>()//原理同上，班级只针对学生。
                .inSql("id", "select class_id from sys_user_adm_class where user_id=" + sysUser.getId()));

        AdmScholarship admScholarship = admScholarshipMapper.searchById(scholarshipId);

        /**
         * 此处不同于会议申请和请假申请的逻辑，因为我们不需要创建奖学金记录，会议申请和请假申请会分别创建奖学金记录和会议记录。
         * 因为奖学金申请是你只能申请提供的奖学金类别，不能随意申请。
         * 我们只需要在adm_scholarship_sys_user表添加关系即可。
         */

        AdmScholarshipSysUser admScholarshipSysUser = new AdmScholarshipSysUser();
        admScholarshipSysUser.setApplyDesc(applyDesc);
        admScholarshipSysUser.setScholarshipId(scholarshipId);
        admScholarshipSysUser.setUserId(userId);
        admScholarshipSysUser.setCreated(LocalDateTime.now());
        admScholarshipSysUser.setUpdated(LocalDateTime.now());
        //只有学生可以申请奖学金。状态申请后是“processing”等待处理
        admScholarshipSysUser.setStatus("processing");
        int insert = admScholarshipSysUserMapper.insert(admScholarshipSysUser);


        //variables为流程变量。
        Map<String,Object> variables = new HashMap<>();
        variables.put("assignee0", (sysUser == null || sysUser.getId().equals("")) ? 0 : sysUser.getId());
        variables.put("assignee1", (null == sysGuiderUser || sysGuiderUser.getId().equals("")) ? "isGuider" : sysGuiderUser.getId());
        variables.put("assignee2", (sysSecretaryUser == null || sysSecretaryUser.getId().equals("")) ? "isSecretary" : sysSecretaryUser.getId());

        //businessKey
        String businessKey = admScholarshipSysUser.getId().toString();

        ProcessInstance processInstance = activiti7Util.createProcessInstance(variables, businessKey,"scholarship");
        System.out.println("*实例id==" + processInstance.getId());
        System.out.println("*业务id(businessKey)==" + processInstance.getBusinessKey());
        //得到个人任务列表。这里的个人应该是当前登录用户。
        List<Map<String, Object>> taskList = activiti7Util.myTaskList(sysUser.getId().toString(),"scholarship");
        //遍历当前登录用户的所有任务
        for (Map<String, Object> map : taskList) {
            //如果当前任务的负责人是当前项目系统登录的用户并且流程实例id是本次创建的流程实例 打印出processInstanceId【流程实例id】和taskid【act_ru_task的主键id】
            if (map.get("assignee").toString().equals(sysUser.getId().toString()) &&
                    map.get("processInstanceId").toString().equals(processInstance.getId())) {
                activiti7Util.completeProcess("发起申请", map.get("taskId").toString(), sysUser.getId().toString());
                System.out.println("请假申请任务已完成---assignee0！！！");
                System.out.println("processInstanceId is {}" + map.get("processInstanceId").toString());
                System.out.println("taskId is {}" + map.get("taskId").toString());

                /**
                 * 此时，complete了当前登录用户本次创建的流程实例的任务。【再强调一下，完成这个任务，才会产生下一个任务。】
                 */
            }
        }

        //请假单已提交消息
        String noticeContent = String.format("您的奖学金申请[%s-%s]已提交，请等待上级审批。"
                , admScholarship.getName(), admScholarship.getLevel());
        admNoticeMapper.insert(new AdmNotice(sysUser.getId(), "您的奖学金申请已提交", noticeContent, LocalDateTime.now(),1));
        mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                sysUser.getEmail(),
                sysUser.getEmail(),
                "通知管理者",
                "您的奖学金申请已提交",
                "您的奖学金申请已提交",
                noticeContent);

        //通知导员审批消息
        String noticeContentGuide = String.format("%s-%s提起奖学金申请[%s~%s]，请尽快审批。"
                , admClass.getClassName(), sysUser.getRealname(), admScholarship.getName(), admScholarship.getLevel());
        admNoticeMapper.insert(new AdmNotice(sysGuiderUser.getId(), "您有新的奖学金审批任务", noticeContentGuide, LocalDateTime.now(),1));
        mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                sysGuiderUser.getEmail(),
                sysGuiderUser.getEmail(),
                "通知管理者",
                "您有新的奖学金审批任务",
                "您有新的奖学金审批任务",
                noticeContent);
    }

    /**
     * 查询指定员工的Meeting系统消息
     * @param receiverId 接收人的id
     * @return List<AdmNotice>
     */
    @Override
    public List<AdmNotice> getScholarshipNoticeList(Long receiverId) {
        return admNoticeMapper.getScholarshipNoticeListByReceiverId(receiverId);
    }


}
