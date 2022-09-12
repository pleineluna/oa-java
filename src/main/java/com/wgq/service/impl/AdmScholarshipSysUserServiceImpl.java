package com.wgq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wgq.common.exception.BusinessException;
import com.wgq.common.exception.BusinessExceptionEnum;
import com.wgq.entity.*;
import com.wgq.entity.dto.AdmScholarshipDto;
import com.wgq.entity.dto.AdmScholarshipSysUserDto;
import com.wgq.mapper.*;
import com.wgq.service.AdmScholarshipSysUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wgq.service.SysRoleService;
import com.wgq.utils.Activiti7Util;
import com.wgq.utils.MailUtil;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 作者:kissshot.wang@foxmail.com
 * @since 2021-12-11
 */
@Service
public class AdmScholarshipSysUserServiceImpl extends ServiceImpl<AdmScholarshipSysUserMapper, AdmScholarshipSysUser> implements AdmScholarshipSysUserService {
    @Resource
    AdmScholarshipSysUserMapper admScholarshipSysUserMapper;
    @Resource
    AdmScholarshipMapper admScholarshipMapper;
    @Resource
    SysUserMapper sysUserMapper;
    @Resource
    AdmClassMapper admClassMapper;
    @Resource
    AdmSpecializedSubjectMapper admSpecializedSubjectMapper;
    @Resource
    Activiti7Util activiti7Util;
    @Resource
    MailUtil mailUtil;
    @Resource
    TaskService taskService;
    @Resource
    RuntimeService runtimeService;
    @Resource
    SysRoleService sysRoleService;
    @Resource
    AdmNoticeMapper admNoticeMapper;

    @Override
    public IPage<AdmScholarshipSysUserDto> scholarshipListNeedToExamine(Page page, List<Long> idList) {
        IPage iPage = admScholarshipSysUserMapper.selectPageVo(page, idList);
        List<AdmScholarshipSysUserDto> records = iPage.getRecords();
        for (AdmScholarshipSysUserDto admScholarshipSysUser : records) {
            //AdmScholarshipSysUser中对应的SysUser
            SysUser theUser = sysUserMapper.selectById(admScholarshipSysUser.getUserId());
            //AdmScholarshipSysUser中对应的AdmScholarship（Dto）
            AdmScholarshipDto theAdmScholarshipDto = admScholarshipMapper.searchAdmScholarshipDtoById(admScholarshipSysUser.getScholarshipId());
            //AdmScholarshipSysUser中对应SysUser的对应班级
            AdmClass theClass = admClassMapper.getClassByUserId(theUser.getId());
            //AdmScholarshipSysUser中对应SysUser的对应专业
            AdmSpecializedSubject theAdmSpecializedSubjectByUserId = admSpecializedSubjectMapper.getAdmSpecializedSubjectByUserId(theUser.getId());
            //一系列赋值设置
            theAdmScholarshipDto.setRealname(theUser.getRealname());
            theAdmScholarshipDto.setIdNumber(theUser.getIdNumber());
            theAdmScholarshipDto.setUserClassName(theClass.getClassName());
            theAdmScholarshipDto.setUserSpecializedSubjectName(theAdmSpecializedSubjectByUserId.getName());
            theAdmScholarshipDto.setUsefulId(admScholarshipSysUser.getId());
            admScholarshipSysUser.setSysUser(theUser);
            admScholarshipSysUser.setAdmScholarshipDto(theAdmScholarshipDto);
        }
        iPage.setRecords(records);
        return iPage;
    }

    @Override
    public AdmScholarshipSysUserDto scholarshipNeedToExamineById(Long id,Long loginUserId) {
        AdmScholarshipSysUserDto admScholarshipSysUserDto = admScholarshipSysUserMapper.selectAdmScholarshipSysUserDtoById(id);
        //AdmScholarshipSysUser中对应的SysUser
        SysUser theUser = sysUserMapper.selectById(admScholarshipSysUserDto.getUserId());
        //AdmScholarshipSysUser中对应的AdmScholarship（Dto）
        AdmScholarshipDto theAdmScholarshipDto = admScholarshipMapper.searchAdmScholarshipDtoById(admScholarshipSysUserDto.getScholarshipId());
        //AdmScholarshipSysUser中对应SysUser的对应班级
        AdmClass theClass = admClassMapper.getClassByUserId(theUser.getId());
        //AdmScholarshipSysUser中对应SysUser的对应专业
        AdmSpecializedSubject theAdmSpecializedSubjectByUserId = admSpecializedSubjectMapper.getAdmSpecializedSubjectByUserId(theUser.getId());
        List<Map<String, Object>> myTaskList = activiti7Util.myTaskList(loginUserId.toString(),"scholarship");
        //获得当前登录用户的所有个人任务，然后根据传来的id也就是adm_scholarship_sys_user的id与任务中的businessKey相等得到当前奖学金申请单所对应的奖学金申请流程实例中的当前assignee的任务的taskId
        //前端 “审批” 按钮用
        for (Map<String, Object> map : myTaskList) {
            if (Long.parseLong(map.get("businessKey").toString()) == id) {
                admScholarshipSysUserDto.setTaskId(map.get("taskId").toString());
            }
        }
        //一系列赋值设置
        theAdmScholarshipDto.setRealname(theUser.getRealname());
        theAdmScholarshipDto.setIdNumber(theUser.getIdNumber());
        theAdmScholarshipDto.setUserClassName(theClass.getClassName());
        theAdmScholarshipDto.setUserSpecializedSubjectName(theAdmSpecializedSubjectByUserId.getName());

        admScholarshipSysUserDto.setSysUser(theUser);
        admScholarshipSysUserDto.setAdmScholarshipDto(theAdmScholarshipDto);
        return admScholarshipSysUserDto;
    }

    /**
     * 查询某个id下的用户的权限编码 ---复用的思想
     *
     * @param id
     * @return roleCodeList
     */
    public List<String> getRoleCodeList(Long id) {
        List<SysRole> roleList = sysRoleService.list(new QueryWrapper<SysRole>()
                .inSql("id", "select role_id from sys_user_role where user_id =" + id));

        List<String> roleCodeList = new ArrayList<>();
        for (SysRole role : roleList) {
            roleCodeList.add(role.getCode());
        }
        return roleCodeList;
    }

    /**
     * 根据请假申请人的权限编码得到 班级/职位信息
     */
    public String getPosition(Long id) {
        List<String> roleCodeList = getRoleCodeList(id);
        String position = "";
        if (!(roleCodeList.contains("superAdmin")) && !(roleCodeList.contains("admin"))) {
            //这个判断条件说明是学生，学生的话是获取班级信息
            AdmClass classByUserId = admClassMapper.getClassByUserId(id);
            position = classByUserId.getClassName()+"学生";
        } else if (!(roleCodeList.contains("superAdmin")) && (roleCodeList.contains("admin"))) {
            //这个判断条件说明是导员
            position = "导员";
        } else {
            position = "书记";
        }
        return position;
    }
    /**
     * 审批奖学金申请单
     * @param taskId 当前任务在activiti中的taskId
     * @param operatorId 经办人id，经办人肯定是当前系统登录用户。Controller层传来的也是安全框架中的登录用户
     * @param result 审批结果  固定"approved"或"refused"
     * @param review 审批意见
     */
    @Override
    public void audit(String taskId, Long operatorId, String result, String review) {
        //任务Id 查询任务对象  --- 从act_ru_task表中查询 ---所以要在completeProcess()前查询
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new BusinessException(BusinessExceptionEnum.TASK_NOT_FOUND);
        }
        //获取任务对象的流程实例Id
        String processInstanceId = task.getProcessInstanceId();

        //拿到当前任务的流程实例
        ProcessInstance processInstance = runtimeService
                .createProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();

        //拿到businessKey
        String businessKey = processInstance.getBusinessKey();

        //adm_scholarship_sys_user --- 本次task所属流程对应的 adm_scholarship_sys_user业务表的AdmScholarshipSysUser对象
        AdmScholarshipSysUser admScholarshipSysUser = admScholarshipSysUserMapper.selectById(businessKey);
        SysUser sysUser = sysUserMapper.selectById(admScholarshipSysUser.getUserId());//表单提交人（申请人）信息
        AdmScholarship admScholarship = admScholarshipMapper.selectById(admScholarshipSysUser.getScholarshipId());//本次申请对应的奖学金类别信息
        //position是表单提交人也就是申请者的职位信息---"学生班级"，因为只能是学生申请奖学金，所以这里必定是学生。
        String position = getPosition(sysUser.getId());
        SysUser operator = sysUserMapper.selectById(operatorId);//任务经办人信息
        SysUser secretary = sysUserMapper.getSecretary();//拿到书记信息
        AdmClass classByUserId = admClassMapper.getClassByUserId(sysUser.getId());//表单提交人所在班级信息


        String strResult = "";
        if (result.equals("approved")) {
            strResult = "批准";
        } else {
            strResult = "驳回";
        }

        if (result.equals("approved")) {
            //审批结果为同意---approved
            activiti7Util.completeProcess(review, taskId, operatorId.toString());
            if (activiti7Util.isEndProcessInstance(processInstanceId)) {
                //true---说明流程已结束，设置请假单业务表的结果字段为approved，然后发送通知
                admScholarshipSysUser.setStatus("approved");
                admScholarshipSysUserMapper.updateById(admScholarshipSysUser); //更新到数据库中

                //经办人只能是导员或者书记。
                //这里的奖学金申请单发起人只能是学生
                String noticeContent = String.format("您的[%s]奖学金申请%s-%s已%s，审批意见：%s，审批流程已结束。"
                        ,admScholarship.getName(), operator.getTitle(), operator.getRealname(), strResult, review);//发给表单提交人
                admNoticeMapper.insert(new AdmNotice(admScholarshipSysUser.getUserId(), "您的奖学金申请已"+strResult, noticeContent, LocalDateTime.now(),5));
                mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                        sysUser.getEmail(),
                        sysUser.getEmail(),
                        "通知管理者",
                        "您的奖学金申请已"+strResult,
                        "您的奖学金申请已"+strResult,
                        noticeContent);

                String noticeContentOperator = String.format("%s-%s提起[%s]奖学金申请您已%s，审批意见：%s，审批流程已结束。",
                        sysUser.getTitle(), sysUser.getRealname(), admScholarship.getName()
                        , strResult, review);//发给审批人的通知
                admNoticeMapper.insert(new AdmNotice(operator.getId(), "您有新的奖学金审批任务已完成", noticeContentOperator, LocalDateTime.now(), 5));
                mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                        operator.getEmail(),
                        operator.getEmail(),
                        "通知管理者",
                        "您有新的奖学金审批任务已完成",
                        "您有新的奖学金审批任务已完成",
                        noticeContent);
            } else {
                //任务complete，但是流程实例还没有结束，发送通知
                admScholarshipSysUser.setStatus("processing");
                admScholarshipSysUserMapper.updateById(admScholarshipSysUser);
                    //当前经办人是导员。
                    //消息1: 通知表单提交人,导员已经审批通过,交由上级继续审批
                    String noticeContent1 = String.format("您的[%s]奖学金申请%s-%s已批准，审批意见：%s ，请继续等待上级审批。",
                            admScholarship.getName(),
                            operator.getTitle(), operator.getRealname(), review);
                    admNoticeMapper.insert(new AdmNotice(sysUser.getId(), "您的奖学金申请有新进展", noticeContent1, LocalDateTime.now(), 5));
                    mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                            sysUser.getEmail(),
                            sysUser.getEmail(),
                            "通知管理者",
                            "您的奖学金申请有新进展",
                            "您的奖学金申请有新进展",
                            noticeContent1);
                    //消息2: 通知书记有新的审批任务
                    String noticeContent2 = String.format("%s-%s提起[%s]奖学金申请，请尽快审批。",
                            sysUser.getTitle(), sysUser.getRealname(), admScholarship.getName());
                    admNoticeMapper.insert(new AdmNotice(secretary.getId(), "您有新的奖学金审批任务", noticeContent2, LocalDateTime.now(), 5));
                    mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                            secretary.getEmail(),
                            secretary.getEmail(),
                            "通知管理者",
                            "您有新的奖学金审批任务",
                            "您有新的奖学金审批任务",
                            noticeContent2);

                    //消息3: 通知导员,学生的申请单你已批准,交由上级继续审批
                    String noticeContent3 = String.format("%s-%s提起[%s]奖学金申请您已批准，审批意见：%s，申请转至上级领导继续审批。"
                            , classByUserId.getClassName(), sysUser.getRealname(), admScholarship.getName(), review);
                    admNoticeMapper.insert(new AdmNotice(operator.getId(), "您有新的奖学金审批任务已完成", noticeContent3, LocalDateTime.now(), 5));
                    mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                            operator.getEmail(),
                            operator.getEmail(),
                            "通知管理者",
                            "您有新的奖学金审批任务已完成",
                            "您有新的奖学金审批任务已完成",
                            noticeContent3);
                //else if position == 书记，就会跳到经办人是书记的审批环节，所以这里不需要发送什么通知。
                // 意思是导员发起的任务经办人是书记。跳到经办人是书记的审批环节，书记审批就是最后一个环节，所以符合 if (isEndProcessInstance(processInstanceId))
            }
        } else {
            //说明审批结果为拒绝---refused   --- 拒绝代表着流程直接结束
            activiti7Util.endTask(review, taskId, operatorId.toString());
            admScholarshipSysUser.setStatus("refused");
            admScholarshipSysUserMapper.updateById(admScholarshipSysUser);
            //该流程实例直接结束，发送通知
            //消息1: 通知申请人表单已被驳回
            String noticeContent1 = String.format("您的[%s]奖学金申请%s-%s已驳回，审批意见：%s,审批流程已结束。",
                    admScholarship.getName(),
                    operator.getTitle(), operator.getRealname(), review);
            admNoticeMapper.insert(new AdmNotice(admScholarshipSysUser.getUserId(), "您的奖学金申请已驳回", noticeContent1, LocalDateTime.now(),5));
            mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                    sysUser.getEmail(),
                    sysUser.getEmail(),
                    "通知管理者",
                    "您的奖学金申请已驳回",
                    "您的奖学金申请已驳回",
                    noticeContent1);

                //消息2: 通知经办人表单"您已驳回"  --- 因为书记自动通过自动发消息，所以这里的else只能是学生
                String noticeContent2 = String.format("%s-%s提起[%s]奖学金申请您已驳回，审批意见：%s，审批流程已结束。",
                        classByUserId.getClassName(),sysUser.getRealname(), admScholarship.getName()
                        , review);
                admNoticeMapper.insert(new AdmNotice(operator.getId(), "您有新的奖学金审批任务已完成", noticeContent2, LocalDateTime.now(), 5));
                mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                        operator.getEmail(),
                        operator.getEmail(),
                        "通知管理者",
                        "您有新的奖学金审批任务已完成",
                        "您有新的奖学金审批任务已完成",
                        noticeContent2);


        }

    }

    /**
     * “我的奖学金”模块用   得到我申请的所有奖学金申请信息
     * @param name 奖学金的关键字
     * @param userId 用户id，当前登录用户的id。这个肯定是学生，只有学生能申请奖学金，查看我的奖学金
     * @return
     */
    @Override
    public List<AdmScholarshipSysUserDto> Myscholarship(String name, Long userId) {
        List<AdmScholarshipSysUserDto> admScholarshipSysUserDtoList = admScholarshipSysUserMapper.searchMyByKeyWord(name, userId);
        for (AdmScholarshipSysUserDto admScholarshipSysUser : admScholarshipSysUserDtoList) {
            //AdmScholarshipSysUser中对应的SysUser
            SysUser theUser = sysUserMapper.selectById(admScholarshipSysUser.getUserId());
            //AdmScholarshipSysUser中对应的AdmScholarshipDto
            AdmScholarshipDto theAdmScholarshipDto = admScholarshipMapper.searchAdmScholarshipDtoById(admScholarshipSysUser.getScholarshipId());
            //AdmScholarshipSysUser中对应SysUser的对应班级
            AdmClass theClass = admClassMapper.getClassByUserId(theUser.getId());
            //AdmScholarshipSysUser中对应SysUser的对应专业
            AdmSpecializedSubject theAdmSpecializedSubjectByUserId = admSpecializedSubjectMapper.getAdmSpecializedSubjectByUserId(theUser.getId());
            //一系列赋值设置
            theAdmScholarshipDto.setRealname(theUser.getRealname());
            theAdmScholarshipDto.setIdNumber(theUser.getIdNumber());
            theAdmScholarshipDto.setUserClassName(theClass.getClassName());
            theAdmScholarshipDto.setUserSpecializedSubjectName(theAdmSpecializedSubjectByUserId.getName());
            theAdmScholarshipDto.setUsefulId(admScholarshipSysUser.getId());
            admScholarshipSysUser.setSysUser(theUser);
            admScholarshipSysUser.setAdmScholarshipDto(theAdmScholarshipDto);
        }
        return admScholarshipSysUserDtoList;
    }

    @Override
    public AdmScholarshipSysUserDto myScholarshipinfo(Long id) {
        AdmScholarshipSysUserDto admScholarshipSysUserDto = admScholarshipSysUserMapper.selectAdmScholarshipSysUserDtoById(id);
        //AdmScholarshipSysUser中对应的SysUser
        SysUser theUser = sysUserMapper.selectById(admScholarshipSysUserDto.getUserId());
        //AdmScholarshipSysUser中对应的AdmScholarshipDto
        AdmScholarshipDto theAdmScholarshipDto = admScholarshipMapper.searchAdmScholarshipDtoById(admScholarshipSysUserDto.getScholarshipId());
        //AdmScholarshipSysUser中对应SysUser的对应班级
        AdmClass theClass = admClassMapper.getClassByUserId(theUser.getId());
        //AdmScholarshipSysUser中对应SysUser的对应专业
        AdmSpecializedSubject theAdmSpecializedSubjectByUserId = admSpecializedSubjectMapper.getAdmSpecializedSubjectByUserId(theUser.getId());
        //一系列赋值设置
        theAdmScholarshipDto.setRealname(theUser.getRealname());
        theAdmScholarshipDto.setIdNumber(theUser.getIdNumber());
        theAdmScholarshipDto.setUserClassName(theClass.getClassName());
        theAdmScholarshipDto.setUserSpecializedSubjectName(theAdmSpecializedSubjectByUserId.getName());

        admScholarshipSysUserDto.setSysUser(theUser);
        admScholarshipSysUserDto.setAdmScholarshipDto(theAdmScholarshipDto);
        return admScholarshipSysUserDto;
    }

    @Override
    public IPage<AdmScholarshipSysUserDto> getApplyScholarshipList(Page page, String realname, String type) {
        IPage iPage = admScholarshipSysUserMapper.selectApplyScholarshipList(page, realname, type);
        List<AdmScholarshipSysUserDto> records = iPage.getRecords();
        for (AdmScholarshipSysUserDto admScholarshipSysUser : records) {
            //AdmScholarshipSysUser中对应的SysUser
            SysUser theUser = sysUserMapper.selectById(admScholarshipSysUser.getUserId());
            //AdmScholarshipSysUser中对应的AdmScholarship（Dto）
            AdmScholarshipDto theAdmScholarshipDto = admScholarshipMapper.searchAdmScholarshipDtoById(admScholarshipSysUser.getScholarshipId());
            //AdmScholarshipSysUser中对应SysUser的对应班级
            AdmClass theClass = admClassMapper.getClassByUserId(theUser.getId());
            //AdmScholarshipSysUser中对应SysUser的对应专业
            AdmSpecializedSubject theAdmSpecializedSubjectByUserId = admSpecializedSubjectMapper.getAdmSpecializedSubjectByUserId(theUser.getId());
            //一系列赋值设置
            theAdmScholarshipDto.setRealname(theUser.getRealname());
            theAdmScholarshipDto.setIdNumber(theUser.getIdNumber());
            theAdmScholarshipDto.setUserClassName(theClass.getClassName());
            theAdmScholarshipDto.setUserSpecializedSubjectName(theAdmSpecializedSubjectByUserId.getName());
            theAdmScholarshipDto.setUsefulId(admScholarshipSysUser.getId());
            admScholarshipSysUser.setSysUser(theUser);
            admScholarshipSysUser.setAdmScholarshipDto(theAdmScholarshipDto);
        }
        iPage.setRecords(records);
        return iPage;
    }


}
