package com.wgq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wgq.entity.dto.AdmLeaveFormInfoDto;
import com.wgq.common.exception.BusinessException;
import com.wgq.common.exception.BusinessExceptionEnum;
import com.wgq.entity.*;
import com.wgq.mapper.*;
import com.wgq.service.AdmLeaveFormService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wgq.service.SysRoleService;
import com.wgq.utils.Activiti7Util;
import com.wgq.utils.MailUtil;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Comment;
import org.activiti.engine.task.Task;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AdmLeaveFormServiceImpl服务实现类
 */
@Service
public class AdmLeaveFormServiceImpl extends ServiceImpl<AdmLeaveFormMapper, AdmLeaveForm> implements AdmLeaveFormService {
    @Resource
    AdmLeaveFormMapper admLeaveFormMapper;

    @Resource
    SysRoleService sysRoleService;

    @Resource
    SysUserMapper sysUserMapper;

    @Resource
    AdmClassMapper admClassMapper;

    @Resource
    AdmNoticeMapper admNoticeMapper;

    @Resource
    Activiti7Util activiti7Util;

    @Resource
    TaskService taskService;

    @Resource
    RuntimeService runtimeService;

    @Resource
    MailUtil mailUtil;

    //全局日期格式化
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 根据当前登录用户获取班级/职位信息以及申请人真实姓名realname
     */
    @Override
    public Map<String, String> baseInfo(SysUser sysUser) {
        Map<String, String> map = new HashMap<>();
        String realname = sysUser.getRealname();
        String position = getPosition(sysUser.getId());
        map.put("realname", realname);
        map.put("position", position);
        return map;
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
     * 创建请假单
     *
     * @param admLeaveForm 前端输入的请假单数据
     */
    @Override
    public void create(AdmLeaveForm admLeaveForm) {

        //本次【请假单】所对应的用户、用户的导员、书记、班级
        SysUser sysUser = sysUserMapper.selectById(admLeaveForm.getUserId());//必能查到
        SysUser sysGuiderUser = sysUserMapper.getGuideUserByUserId(admLeaveForm.getUserId());//没有的话返回null,比如导员的导员当然不存在。
        SysUser sysSecretaryUser = sysUserMapper.getSecretary();//原理同上
        AdmClass admClass = admClassMapper.selectOne(new QueryWrapper<AdmClass>()//原理同上，班级只针对学生。
                .inSql("id", "select class_id from sys_user_adm_class where user_id=" + admLeaveForm.getUserId()));


        //1.查出当前请假人员的角色code编码
        //目的：如果是superAdmin表单状态设置为approved（同意）,而admin、normal表单状态设置为processing（正在审批）
        List<String> roleCodeList = getRoleCodeList(admLeaveForm.getUserId());
        for (String roleCode : roleCodeList) {
            if (roleCode.equals("superAdmin")) {
                admLeaveForm.setState("approved");
            } else {
                admLeaveForm.setState("processing");
            }
        }
        admLeaveForm.setCreated(LocalDateTime.now());
        int insert = admLeaveFormMapper.insert(admLeaveForm);

        /*经过上述操作，【创建了admLeaveForm】，其实是用admLeaveForm接受前台参数而形成的，
         * 严谨一些后续还是newAdmLeaveForm=admLeaveForm，对newAdmLeaveForm操作的形式比较好。
         * 其中，如果是supperAdmin，也就是书记拥有的角色，admLeaveForm的状态自动为approved
         * 其他的则设置为processing。
         */

        //variables为流程变量。
        Map<String,Object> variables = new HashMap<>();
        //因为有些【当前登录用户sysUser】是查不出来他的导员、书记的，所以默认assignee设置为isGuider\isSecretary,只是表示我就是导员或者书记。
        variables.put("assignee0", (sysUser == null || sysUser.getId().equals("")) ? 0 : sysUser.getId());
        variables.put("assignee1", (null == sysGuiderUser || sysGuiderUser.getId().equals("")) ? "isGuider" : sysGuiderUser.getId());
        variables.put("assignee2", (sysSecretaryUser == null || sysSecretaryUser.getId().equals("")) ? "isSecretary" : sysSecretaryUser.getId());
        long diff = admLeaveForm.getEndTime().toInstant(ZoneOffset.of("+8")).toEpochMilli() - admLeaveForm.getStartTime().toInstant(ZoneOffset.of("+8")).toEpochMilli();
        float days = (diff / (1000 * 60 * 60) * 1f) / 24;
        System.out.println("*该请假申请的天数days变量值为：" + days);
        variables.put("days", days);
        //businessKey
        String businessKey = admLeaveForm.getId().toString();

        //2.根据申请人的身份创建对应的流程实例
        //2.1 学生用户---normal，生成导员审批任务
        if (!(roleCodeList.contains("superAdmin")) && !(roleCodeList.contains("admin"))) {
            variables.put("identity", 1);
            ProcessInstance processInstance = activiti7Util.createProcessInstance(variables, businessKey,"leave");
            System.out.println("*实例id=="+processInstance.getId());
            System.out.println("*业务id=="+processInstance.getBusinessKey());
            //得到个人任务列表。这里的个人应该是当前登录用户。sysUser是根据admLeaveForm.getUserId()查到的，admLeaveForm的id是Controller中从安全框架得到的当前登录用户
            List<Map<String, Object>> taskList = activiti7Util.myTaskList(sysUser.getId().toString(),"leave");
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
            String noticeContent = String.format("您的请假申请[%s~%s]已提交，请等待上级审批。"
                    , admLeaveForm.getStartTime().format(formatter), admLeaveForm.getEndTime().format(formatter));
            admNoticeMapper.insert(new AdmNotice(admLeaveForm.getUserId(), "您的请假申请已提交", noticeContent, LocalDateTime.now(),1));
            mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                    sysUser.getEmail(),
                    sysUser.getEmail(),
                    "通知管理者",
                    "您的请假申请已提交",
                    "您的请假申请已提交",
                    noticeContent);

            //通知导员审批消息
            String noticeContentGuide = String.format("%s-%s提起请假申请[%s~%s]，请尽快审批。"
                    , admClass.getClassName(), sysUser.getRealname(), admLeaveForm.getStartTime().format(formatter), admLeaveForm.getEndTime().format(formatter));
            admNoticeMapper.insert(new AdmNotice(sysGuiderUser.getId(), "您有新的请假审批任务", noticeContentGuide, LocalDateTime.now(),1));
            mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                    sysGuiderUser.getEmail(),
                    sysGuiderUser.getEmail(),
                    "通知管理者",
                    "您有新的请假审批任务",
                    "您有新的请假审批任务",
                    noticeContent);
        } else if (!(roleCodeList.contains("superAdmin")) && (roleCodeList.contains("admin"))) {
            //2.2 导员，生成书记审批任务
            variables.put("identity", 2);
            ProcessInstance processInstance = activiti7Util.createProcessInstance(variables, businessKey,"leave");
            System.out.println("*实例id=="+processInstance.getId());
            System.out.println("*业务id=="+processInstance.getBusinessKey());
            //得到个人任务列表。这里的个人应该是当前登录用户。sysUser是根据admLeaveForm.getUserId()查到的，admLeaveForm的id是Controller中从安全框架得到的当前登录用户
            List<Map<String, Object>> taskList = activiti7Util.myTaskList(sysUser.getId().toString(),"leave");
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
            /*注意，taskList是当前登录用户个人任务列表，他是纵向的，意思是这个用户在当前这个阶段的所有任务，而不是横向当前整个流程这个人的任务。
             * 并且，你只要不complete当前用户任务，下一个用户任务是不可能出现的，所以肯定不是横向的。
             * 所以要先complete才能产生下一个任务，也就是“导员审批”下的任务
             */

            //导员的请假单已提交消息
            String noticeContent = String.format("您的请假申请[%s~%s]已提交，请等待上级审批。"
                    , admLeaveForm.getStartTime().format(formatter), admLeaveForm.getEndTime().format(formatter));
            admNoticeMapper.insert(new AdmNotice(admLeaveForm.getUserId(), "您的请假申请已提交", noticeContent, LocalDateTime.now(), 1));
            mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                    sysUser.getEmail(),
                    sysUser.getEmail(),
                    "通知管理者",
                    "您的请假申请已提交",
                    "您的请假申请已提交",
                    noticeContent);

            //通知书记审批消息
            String noticeContentSecretary = String.format("辅导员-%s提起请假申请[%s~%s]，请尽快审批。"
                    , sysUser.getRealname(), admLeaveForm.getStartTime().format(formatter), admLeaveForm.getEndTime().format(formatter));
            admNoticeMapper.insert(new AdmNotice(sysSecretaryUser.getId(), "您有新的请假审批任务", noticeContentSecretary, LocalDateTime.now(), 1));
            mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                    sysSecretaryUser.getEmail(),
                    sysSecretaryUser.getEmail(),
                    "通知管理者",
                    "您有新的请假审批任务",
                    "您有新的请假审批任务",
                    noticeContent);

        } else if (roleCodeList.contains("superAdmin")) {
            //2.3 书记，生成书记审批任务，系统自动通过，状态已经在上述逻辑设置为approved
            variables.put("identity", 3);
            ProcessInstance processInstance = activiti7Util.createProcessInstance(variables, businessKey,"leave");
            //得到个人任务列表。这里的个人应该是当前登录用户。sysUser是根据admLeaveForm.getUserId()查到的，admLeaveForm的id是Controller中从安全框架得到的当前登录用户
            List<Map<String, Object>> taskList = activiti7Util.myTaskList(sysUser.getId().toString(),"leave");
            //遍历当前登录用户的所有任务
            for (Map<String, Object> map : taskList) {
                //如果当前任务的负责人是当前项目系统登录的用户并且流程实例id是本次创建的流程实例 打印出processDefinitionId【流程实例id】和taskid【act_ru_task的主键id】
                if (map.get("assignee").toString().equals(sysUser.getId().toString()) &&
                        map.get("processInstanceId").toString().equals(processInstance.getId())) {
                    activiti7Util.completeProcess("发起申请", map.get("taskId").toString(), sysUser.getId().toString());
                    System.out.println("请假申请任务已完成--assignee0！！！");
                    System.out.println("processInstanceId is {}" + map.get("processInstanceId").toString());
                    System.out.println("taskId is {}" + map.get("taskId").toString());

                    /**
                     * 此时，complete了当前登录用户本次创建的流程实例的任务。【再强调一下，完成这个任务，才会产生下一个任务。】
                     */
                }
            }

            //书记的请假单自动通过
            String noticeContent = String.format("您的请假申请[%s~%s]已自动批准通过。"
                    , admLeaveForm.getStartTime().format(formatter), admLeaveForm.getEndTime().format(formatter));
            admNoticeMapper.insert(new AdmNotice(sysUser.getId(), "您的请假申请已自动批准通过", noticeContent, LocalDateTime.now(), 1));
            mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                    sysUser.getEmail(),
                    sysUser.getEmail(),
                    "通知管理者",
                    "您的请假申请已自动批准通过",
                    "您的请假申请已自动批准通过",
                    noticeContent);
        }

    }

    /**
     * 获取指定经办人对应的请假单列表
     * @return IPage<AdmLeaveFormInfoDto>
     */
    @Override
    public IPage<AdmLeaveFormInfoDto> getLeaveFormList(Page<AdmLeaveFormInfoDto> page, List<Long> idList) {
        IPage<AdmLeaveFormInfoDto> admLeaveFormInfoDtoIPage = admLeaveFormMapper.selectPageVo(page, idList);
        List<AdmLeaveFormInfoDto> records = admLeaveFormInfoDtoIPage.getRecords();
        /**
         * 循环遍历设置班级或职务信息。
         */
        for (AdmLeaveFormInfoDto admLeaveFormInfoDto : records) {
            admLeaveFormInfoDto.setPosition(getPosition(admLeaveFormInfoDto.getUserId()));
        }
        admLeaveFormInfoDtoIPage.setRecords(records);
        return admLeaveFormInfoDtoIPage;
    }

    /**
     * 得到对应id的AdmLeaveFormInfoDto
     */
    @Override
    public AdmLeaveFormInfoDto getLeaveFormById(Long id,Long userId) {
        AdmLeaveFormInfoDto leaveFormById = admLeaveFormMapper.getLeaveFormById(id);
        List<Map<String, Object>> myTaskList = activiti7Util.myTaskList(userId.toString(),"leave");
        //获得当前登录用户的所有个人任务，然后根据传来的id也就是adm_leave_form的id与任务中的businessKey相等得到当前请假单所对应的请假流程实例中的当前assignee的任务的taskId
        //前端 “审批” 按钮用
        for (Map<String, Object> map : myTaskList) {
            if (Long.parseLong(map.get("businessKey").toString()) == id) {
                leaveFormById.setTaskId(map.get("taskId").toString());
            }
        }
        leaveFormById.setPosition(getPosition(leaveFormById.getUserId()));
        return leaveFormById;
    }


    /**
     * 审批请假单
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

        //admLeaveForm --- 本次task所属流程对应的 act_leave_form业务表的AdmLeaveForm对象
        AdmLeaveForm admLeaveForm = admLeaveFormMapper.selectById(businessKey);
        SysUser sysUser = sysUserMapper.selectById(admLeaveForm.getUserId());//表单提交人信息
        //position是表单提交人也就是请假者的职位信息---"学生班级"/"导员"/"书记"
        String position = getPosition(sysUser.getId());
        SysUser operator = sysUserMapper.selectById(operatorId);//任务经办人信息
        SysUser secretary = sysUserMapper.getSecretary();//拿到书记信息
        AdmClass classByUserId = admClassMapper.getClassByUserId(sysUser.getId());//表单提交人所在班级信息

        String strResult = null;
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
                admLeaveForm.setState("approved");
                admLeaveFormMapper.updateById(admLeaveForm); //更新到数据库中

                //经办人只能是导员或者书记。书记发起的请求是自动通过的，会在自动通过那里发送通知。
                //所以这里的请假单发起人只能是学生或者导员。
                String noticeContent = String.format("您的请假申请[%s~%s]%s-%s已%s，审批意见：%s，审批流程已结束。"
                        , admLeaveForm.getStartTime().format(formatter), admLeaveForm.getEndTime().format(formatter)
                        , operator.getTitle(), operator.getRealname(), strResult, review);//发给表单提交人
                admNoticeMapper.insert(new AdmNotice(admLeaveForm.getUserId(), "您的请假申请已"+strResult, noticeContent, LocalDateTime.now(),1));
                mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                        sysUser.getEmail(),
                        sysUser.getEmail(),
                        "通知管理者",
                        "您的请假申请已"+strResult,
                        "您的请假申请已"+strResult,
                        noticeContent);

                String noticeContentOperator = String.format("%s-%s提起请假申请[%s~%s]您已%s，审批意见：%s，审批流程已结束。",
                        sysUser.getTitle(), sysUser.getRealname()
                        , admLeaveForm.getStartTime().format(formatter), admLeaveForm.getEndTime().format(formatter),
                        strResult, review);//发给审批人的通知
                admNoticeMapper.insert(new AdmNotice(operator.getId(), "您有新的请假审批任务已完成", noticeContentOperator, LocalDateTime.now(), 1));
                mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                        operator.getEmail(),
                        operator.getEmail(),
                        "通知管理者",
                        "您有新的请假审批任务已完成",
                        "您有新的请假审批任务已完成",
                        noticeContent);
            } else {
                //任务complete，但是流程实例还没有结束，发送通知
                admLeaveForm.setState("processing");
                admLeaveFormMapper.updateById(admLeaveForm);
                if (position.equals("导员")) {
                    //当前经办人是导员。
                    //消息1: 通知表单提交人,导员已经审批通过,交由上级继续审批
                    String noticeContent1 = String.format("您的请假申请[%s~%s]%s-%s已批准，审批意见：%s ，请继续等待上级审批。",
                            admLeaveForm.getStartTime().format(formatter), admLeaveForm.getEndTime().format(formatter),
                            operator.getTitle(), operator.getRealname(), review);
                    admNoticeMapper.insert(new AdmNotice(sysUser.getId(), "您的请假申请有新进展", noticeContent1, LocalDateTime.now(), 1));
                    mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                            sysUser.getEmail(),
                            sysUser.getEmail(),
                            "通知管理者",
                            "您的请假申请有新进展",
                            "您的请假申请有新进展",
                            noticeContent1);
                    //消息2: 通知书记有新的审批任务
                    String noticeContent2 = String.format("%s-%s提起请假申请[%s~%s]，请尽快审批。",
                            sysUser.getTitle(), sysUser.getRealname(), admLeaveForm.getStartTime().format(formatter), admLeaveForm.getEndTime().format(formatter));
                    admNoticeMapper.insert(new AdmNotice(secretary.getId(), "您有新的请假审批任务", noticeContent2, LocalDateTime.now(), 1));
                    mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                            secretary.getEmail(),
                            secretary.getEmail(),
                            "通知管理者",
                            "您有新的请假审批任务",
                            "您有新的请假审批任务",
                            noticeContent2);

                    //消息3: 通知导员,学生的申请单你已批准,交由上级继续审批
                    String noticeContent3 = String.format("%s-%s提起请假申请[%s~%s]您已批准，审批意见：%s，申请转至上级领导继续审批。"
                            , classByUserId.getClassName(), sysUser.getRealname(), admLeaveForm.getStartTime().format(formatter), admLeaveForm.getEndTime().format(formatter), review);
                    admNoticeMapper.insert(new AdmNotice(operator.getId(), "您有新的请假审批任务已完成", noticeContent3, LocalDateTime.now(), 1));
                    mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                            operator.getEmail(),
                            operator.getEmail(),
                            "通知管理者",
                            "您有新的请假审批任务已完成",
                            "您有新的请假审批任务已完成",
                            noticeContent3);
                }
                //else if position == 书记，就会跳到经办人是书记的审批环节，所以这里不需要发送什么通知。
                // 意思是导员发起的任务经办人是书记。跳到经办人是书记的审批环节，书记审批就是最后一个环节，所以符合 if (isEndProcessInstance(processInstanceId))
            }
        } else {
            //说明审批结果为拒绝---refused   --- 拒绝代表着流程直接结束
            activiti7Util.endTask(review, taskId, operatorId.toString());
            admLeaveForm.setState("refused");
            admLeaveFormMapper.updateById(admLeaveForm);
            //该流程实例直接结束，发送通知
            //消息1: 通知申请人表单已被驳回
            String noticeContent1 = String.format("您的请假申请[%s~%s]%s-%s已驳回，审批意见：%s,审批流程已结束。",
                    admLeaveForm.getStartTime().format(formatter), admLeaveForm.getEndTime().format(formatter),
                    operator.getTitle(), operator.getRealname(), review);
            admNoticeMapper.insert(new AdmNotice(admLeaveForm.getUserId(), "您的请假申请已驳回", noticeContent1, LocalDateTime.now(),1));
            mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                    sysUser.getEmail(),
                    sysUser.getEmail(),
                    "通知管理者",
                    "您的请假申请已驳回",
                    "您的请假申请已驳回",
                    noticeContent1);
            if (position.equals("导员")) {
                //消息2: 通知经办人表单"您已驳回"  --- position.equals("导员"):表单提交人是导员，
                //所以这里的经办人是书记
                String noticeContent2 = String.format("导员：%s提起请假申请[%s~%s]您已驳回，审批意见：%s，审批流程已结束。",
                        sysUser.getRealname(), admLeaveForm.getStartTime().format(formatter), admLeaveForm.getEndTime().format(formatter)
                        , review);
                admNoticeMapper.insert(new AdmNotice(operator.getId(), "您有新的请假审批任务已完成", noticeContent2, LocalDateTime.now(), 1));
                mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                        operator.getEmail(),
                        operator.getEmail(),
                        "通知管理者",
                        "您有新的请假审批任务已完成",
                        "您有新的请假审批任务已完成",
                        noticeContent2);
            } else{
                //消息2: 通知经办人表单"您已驳回"  --- 因为书记自动通过自动发消息，所以这里的else只能是学生
                String noticeContent2 = String.format("%s-%s提起请假申请[%s~%s]您已驳回，审批意见：%s，审批流程已结束。",
                        classByUserId.getClassName(),sysUser.getRealname(), admLeaveForm.getStartTime().format(formatter), admLeaveForm.getEndTime().format(formatter)
                        , review);
                admNoticeMapper.insert(new AdmNotice(operator.getId(), "您有新的请假审批任务已完成", noticeContent2, LocalDateTime.now(), 1));
                mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                        operator.getEmail(),
                        operator.getEmail(),
                        "通知管理者",
                        "您有新的请假审批任务已完成",
                        "您有新的请假审批任务已完成",
                        noticeContent2);
            }


        }
    }

    /**
     *
     * @param id
     * @return AdmLeaveFormInfoDto
     */
    @Override
    public AdmLeaveFormInfoDto infoForManagementModel(Long id) {
        AdmLeaveFormInfoDto admLeaveFormInfoDto = admLeaveFormMapper.selectLeaveFormById(id);
        List<Comment> commentList = activiti7Util.findCommentByBusinessKey(id.toString());
        //这个是申请人的position
        String positionOfApplicaton = getPosition(admLeaveFormInfoDto.getUserId());
        if (positionOfApplicaton.equals("书记")) {
            admLeaveFormInfoDto.setIdentity(3);
        } else if (positionOfApplicaton.equals("导员")) {
            admLeaveFormInfoDto.setIdentity(2);
        } else {
            admLeaveFormInfoDto.setIdentity(1);
        }
        for (Comment comment : commentList) {
            /**
             * 如果此条批注的生产者【也就是谁给批注的】不是自身，那么说明不是自己申请时候的那条默认批注。我们也不需要那条申请都有的默认批注，所以我们需要过滤掉
             */
            if (Long.parseLong(comment.getUserId()) != admLeaveFormInfoDto.getUserId()) {
                String position = getPosition(Long.parseLong(comment.getUserId()));//这个是批注生产者的position
                if (position.equals("书记")){
                    //查出批注生产者的姓名
                HashMap searchName = sysUserMapper.searchName(Long.parseLong(comment.getUserId()));
                String message = String.format("%s-%s 审批意见：%s",
                        position, searchName.get("realname"), comment.getFullMessage());
                    admLeaveFormInfoDto.setCommentOfSecretary(message);
                    admLeaveFormInfoDto.setWhetherAuditSecretary("yes");
                } else if (position.equals("导员")) {
                    HashMap searchName = sysUserMapper.searchName(Long.parseLong(comment.getUserId()));
                    String message = String.format("%s-%s 审批意见：%s",
                            position, searchName.get("realname"), comment.getFullMessage());
                    admLeaveFormInfoDto.setCommentOfGuider(message);
                }
                }
        }
        return admLeaveFormInfoDto;
    }
}

