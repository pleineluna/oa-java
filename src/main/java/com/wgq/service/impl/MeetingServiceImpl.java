package com.wgq.service.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wgq.entity.dto.TbMeetingDto;
import com.wgq.common.exception.BusinessException;
import com.wgq.common.exception.BusinessExceptionEnum;
import com.wgq.entity.*;
import com.wgq.mapper.AdmClassMapper;
import com.wgq.mapper.AdmNoticeMapper;
import com.wgq.mapper.SysUserMapper;
import com.wgq.mapper.TbMeetingMapper;
import com.wgq.service.MeetingService;
import com.wgq.service.SysRoleService;
import com.wgq.utils.Activiti7Util;
import com.wgq.utils.MailUtil;
import com.wgq.utils.PageUtil;
import com.wgq.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class MeetingServiceImpl implements MeetingService {
    @Resource
    private TbMeetingMapper tbMeetingMapper;
    @Resource
    private RuntimeService runtimeService;
    @Resource
    private SysUserMapper sysUserMapper;
    @Resource
    private TaskService taskService;
    @Resource
    private SysRoleService sysRoleService;
    @Resource
    AdmNoticeMapper admNoticeMapper;
    @Resource
    AdmClassMapper admClassMapper;
    @Resource
    RedisUtil redisUtil;
    @Resource
    RedisTemplate redisTemplate;
    @Resource
    Activiti7Util activiti7Util;
    @Resource
    MailUtil mailUtil;


    @Override
    public PageUtil searchOfflineMeetingByPage(HashMap param) {
        ArrayList<HashMap> list = tbMeetingMapper.searchOfflineMeetingByPage(param);
        long count = tbMeetingMapper.searchOfflineMeetingCount(param);
        int start = MapUtil.getInt(param, "start");
        int length = MapUtil.getInt(param, "length");
        //把meeting字段转换为JSON数组对象格式
        for (HashMap map : list) {
            String meeting = (String) map.get("meeting");
            //如果meeting是有效字段，就转换成JSON数组对象格式
            if (meeting != null && meeting.length() > 0) {
                map.replace("meeting", JSONUtil.parseArray(meeting));
            }
        }
        PageUtil pageUtil = new PageUtil(list, count, start, length);
        return pageUtil;
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
     * 为了实现复用
     * @param
     * @return
     */
    public int apiOfUpdataMeetingInstanceId(String uuid, String instanceId) {
        HashMap param = new HashMap();
        param.put("uuid", uuid);
        param.put("instanceId", instanceId);
        return tbMeetingMapper.updateMeetingInstanceId(param);
    }

    @Override
    public int insert(TbMeeting meeting) {
        //本次【会议申请流程实例】所对应的用户、用户的导员、书记
        SysUser sysUser = sysUserMapper.selectById(meeting.getCreatorId());//必能查到
        SysUser sysGuiderUser = sysUserMapper.getGuideUserByUserId(meeting.getCreatorId().longValue());//没有的话返回null,比如导员的导员当然不存在。
        SysUser sysSecretaryUser = sysUserMapper.getSecretary();//原理同上
        AdmClass admClass = admClassMapper.selectOne(new QueryWrapper<AdmClass>()//原理同上，班级只针对学生。
                .inSql("id", "select class_id from sys_user_adm_class where user_id=" + meeting.getCreatorId()));


        //1.查出当前会议申请人员的角色code编码
        //目的：如果是superAdmin表单状态设置为approved（同意）,而admin、normal表单状态设置为processing（正在审批）
        List<String> roleCodeList = getRoleCodeList(meeting.getCreatorId().longValue());
        for (String roleCode : roleCodeList) {
            if (roleCode.equals("superAdmin")) {
                meeting.setStatus((short) 3); // 3 :审批通过
            } else {
                meeting.setStatus((short) 1); // 1 :申请中
            }
        }
        //生成新建会议的roomId，存到Redis中，key为会议Uuid的，value为会议的roomId。其中roomId在表中不存在，因为是放在redis中的。
        if (meeting.getType() == 1) {
            //meeting.getType() == 1 说明是线上会议，只有线上会议才用得到roomId
            Random random = new Random();
            StringBuilder roomId = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                roomId.append(random.nextInt(10));
            }
            redisUtil.set(meeting.getUuid(), roomId.toString());
        }
        int rows = tbMeetingMapper.insert1(meeting);
        Long id = null;
        if (rows == 1) {
             id = meeting.getId();
        }
        if (rows != 1) {
            throw new BusinessException(BusinessExceptionEnum.FAILED_TO_ADD_MEETING);
        }
        /**
         * 这里应该是会议申请流程实例的开始点，也就是个人申请。
         */

        //variables为流程变量。
        Map<String, Object> variables = new HashMap<>();
        //assignee0是会议申请，对应当前系统登录用户。Controller层：meeting.setCreatorId(Integer.parseInt(sysUser.getId().toString()));
        variables.put("assignee0", meeting.getCreatorId());
        variables.put("assignee1", (null == sysGuiderUser || sysGuiderUser.getId().equals("")) ? "isGuider" : sysGuiderUser.getId());
        variables.put("assignee2", (sysSecretaryUser == null || sysSecretaryUser.getId().equals("")) ? "isSecretary" : sysSecretaryUser.getId());

        //businessKey
        String businessKey = id.toString();
        System.out.println("*businessKey:"+businessKey);


        //2.根据申请人的身份创建对应的流程实例
        //2.1学生用户---normal，生成导员审批任务
        if (!(roleCodeList.contains("superAdmin")) && !(roleCodeList.contains("admin"))) {
            variables.put("identity", 1);//设置bpmn中的分支变量 identity=1 也就是申请人身份为学生
            ProcessInstance processInstance = activiti7Util.createProcessInstance(variables, businessKey,"meeting");
            apiOfUpdataMeetingInstanceId(meeting.getUuid(), processInstance.getId());
            System.out.println("*实例id==" + processInstance.getId());
            System.out.println("*业务id==" + processInstance.getBusinessKey());
            //得到个人任务列表。这里的个人应该是当前登录用户。sysUser是根据admLeaveForm.getUserId()查到的，admLeaveForm的id是Controller中从安全框架得到的当前登录用户
            List<Map<String, Object>> taskList = activiti7Util.myTaskList(sysUser.getId().toString(),"meeting");
            //遍历当前登录用户的所有任务
            for (Map<String, Object> map : taskList) {
                //如果当前任务的负责人是当前项目系统登录的用户并且流程实例id是本次创建的流程实例 打印出processInstanceId【流程实例id】和taskid【act_ru_task的主键id】
                if (map.get("assignee").toString().equals(sysUser.getId().toString()) &&
                        map.get("processInstanceId").toString().equals(processInstance.getId())) {
                    activiti7Util.completeProcess("同意", map.get("taskId").toString(), sysUser.getId().toString());
                    System.out.println("会议申请任务已完成---assignee0！！！");
                    System.out.println("processInstanceId is {}" + map.get("processInstanceId").toString());
                    System.out.println("taskId is {}" + map.get("taskId").toString());

                    /**
                     * 此时，complete了当前登录用户本次创建的流程实例的任务。【再强调一下，完成这个任务，才会产生下一个任务。】
                     */
                }
            }
            //会议申请单已提交消息
            String noticeContent = String.format("您的会议申请[%s~%s]已提交，请等待上级审批。"
                    , meeting.getStart(), meeting.getEnd());
            admNoticeMapper.insert(new AdmNotice(meeting.getCreatorId().longValue(), "您的会议申请已提交", noticeContent, LocalDateTime.now(),4));
            mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                    sysUser.getEmail(),
                    sysUser.getEmail(),
                    "通知管理者",
                    "您的会议申请已提交",
                    "您的会议申请已提交",
                    noticeContent);
            //通知导员审批消息
            String noticeContentGuide = String.format("%s-%s提起会议申请[%s~%s]，请尽快审批。"
                    , admClass.getClassName(), sysUser.getRealname(), meeting.getStart(), meeting.getEnd());
            admNoticeMapper.insert(new AdmNotice(sysGuiderUser.getId(), "您有新的会议审批任务", noticeContentGuide, LocalDateTime.now(), 4));
            mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                    sysGuiderUser.getEmail(),
                    sysGuiderUser.getEmail(),
                    "通知管理者",
                    "您有新的会议审批任务",
                    "您有新的会议审批任务",
                    noticeContent);
        }
        ////2.2 导员，生成书记审批任务
        else if (!(roleCodeList.contains("superAdmin")) && (roleCodeList.contains("admin"))) {
            variables.put("identity", 2);
            ProcessInstance processInstance = activiti7Util.createProcessInstance(variables, businessKey,"meeting");
            apiOfUpdataMeetingInstanceId(meeting.getUuid(), processInstance.getId());
            System.out.println("*实例id==" + processInstance.getId());
            System.out.println("*业务id==" + processInstance.getBusinessKey());
            //得到个人任务列表。这里的个人应该是当前登录用户。sysUser是根据admLeaveForm.getUserId()查到的，admLeaveForm的id是Controller中从安全框架得到的当前登录用户
            List<Map<String, Object>> taskList = activiti7Util.myTaskList(sysUser.getId().toString(),"meeting");
            //遍历当前登录用户的所有任务
            for (Map<String, Object> map : taskList) {
                //如果当前任务的负责人是当前项目系统登录的用户并且流程实例id是本次创建的流程实例 打印出processInstanceId【流程实例id】和taskid【act_ru_task的主键id】
                if (map.get("assignee").toString().equals(sysUser.getId().toString()) &&
                        map.get("processInstanceId").toString().equals(processInstance.getId())) {
                    activiti7Util.completeProcess("同意", map.get("taskId").toString(), sysUser.getId().toString());
                    System.out.println("会议申请任务已完成---assignee0！！！");
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

            //导员的会议申请单已提交消息
            String noticeContent = String.format("您的会议申请[%s~%s]已提交，请等待上级审批。"
                    , meeting.getStart(), meeting.getEnd());
            //longValue()是Long类的一个方法，用来得到Long类中的数值；也就是将包装类中的数据拆箱成基本数据类型。
            //Long.valueOf(参数)是将参数转换成long的包装类——Long；也就是把基本数据类型转换成包装类。
            admNoticeMapper.insert(new AdmNotice(meeting.getCreatorId().longValue(), "您的会议申请已提交", noticeContent, LocalDateTime.now(), 4));
            mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                    sysUser.getEmail(),
                    sysUser.getEmail(),
                    "通知管理者",
                    "您的会议申请已提交",
                    "您的会议申请已提交",
                    noticeContent);
            //通知书记审批消息
            String noticeContentSecretary = String.format("辅导员-%s提起会议申请[%s~%s]，请尽快审批。"
                    , sysUser.getRealname(), meeting.getStart(), meeting.getEnd());
            admNoticeMapper.insert(new AdmNotice(sysSecretaryUser.getId(), "您有新的会议审批任务", noticeContentSecretary, LocalDateTime.now(), 4));
            mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                    sysSecretaryUser.getEmail(),
                    sysSecretaryUser.getEmail(),
                    "通知管理者",
                    "您有新的会议审批任务",
                    "您有新的会议审批任务",
                    noticeContent);
        } else if (roleCodeList.contains("superAdmin")) {
            //2.3 书记，生成书记审批任务，系统自动通过，状态已经在上述逻辑设置为 3 :审批通过
            variables.put("identity", 3);
            ProcessInstance processInstance = activiti7Util.createProcessInstance(variables, businessKey,"meeting");
            apiOfUpdataMeetingInstanceId(meeting.getUuid(), processInstance.getId());
            //得到个人任务列表。这里的个人应该是当前登录用户。sysUser是根据admLeaveForm.getUserId()查到的，admLeaveForm的id是Controller中从安全框架得到的当前登录用户
            List<Map<String, Object>> taskList = activiti7Util.myTaskList(sysUser.getId().toString(),"meeting");
            //遍历当前登录用户的所有任务
            for (Map<String, Object> map : taskList) {
                //如果当前任务的负责人是当前项目系统登录的用户并且流程实例id是本次创建的流程实例 打印出processDefinitionId【流程实例id】和taskid【act_ru_task的主键id】
                if (map.get("assignee").toString().equals(sysUser.getId().toString()) &&
                        map.get("processInstanceId").toString().equals(processInstance.getId())) {
                    activiti7Util.completeProcess("同意", map.get("taskId").toString(), sysUser.getId().toString());
                    System.out.println("会议申请任务已完成--assignee0！！！");
                    System.out.println("processInstanceId is {}" + map.get("processInstanceId").toString());
                    System.out.println("taskId is {}" + map.get("taskId").toString());

                    /**
                     * 此时，complete了当前登录用户本次创建的流程实例的任务。【再强调一下，完成这个任务，才会产生下一个任务。】
                     */
                }
            }

            //书记的会议单自动通过
            String noticeContent = String.format("您的会议申请[%s~%s]已自动批准通过。"
                    , meeting.getStart(), meeting.getEnd());
            admNoticeMapper.insert(new AdmNotice(sysUser.getId(), "您的会议申请已自动批准通过", noticeContent, LocalDateTime.now(), 4));
            mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                    sysUser.getEmail(),
                    sysUser.getEmail(),
                    "通知管理者",
                    "您的会议申请已自动批准通过",
                    "您的会议申请已自动批准通过",
                    noticeContent);
        }
        return rows;
    }

    @Override
    public ArrayList<HashMap> searchOfflineMeetingInWeek(HashMap param) {
        ArrayList<HashMap> list = tbMeetingMapper.searchOfflineMeetingInWeek(param);
        return list;
    }

    @Override
    public HashMap searchMeetingInfo(short status, long id) {
        //判断正在进行中的会议
        HashMap map;
        if (status == 4 || status == 5) {
            map = tbMeetingMapper.searchCurrentMeetingInfo(id);
        } else {
            map = tbMeetingMapper.searchMeetingInfo(id);
        }
        return map;
    }

    @Override
    public int deleteMeetingApplication(HashMap param) {
        Long id = MapUtil.getLong(param, "id");
        String uuid = MapUtil.getStr(param, "uuid");
        String instanceId = MapUtil.getStr(param, "instanceId");
        //查询会议详情，一会儿要判断是否距离会议开始不足20分钟
        HashMap meeting = tbMeetingMapper.searchMeetingById(param);
        String date = MapUtil.getStr(meeting, "date");
        String start = MapUtil.getStr(meeting, "start");
        int status = MapUtil.getInt(meeting, "status");
        boolean isCreator = Boolean.parseBoolean(MapUtil.getStr(meeting, "isCreator"));
        DateTime dateTime = DateUtil.parse(date + " " + start);
        DateTime now = DateUtil.date();
        if (now.isAfterOrEquals(dateTime.offset(DateField.MINUTE, -20))) {
            throw new BusinessException(BusinessExceptionEnum.LESS_THAN_20MIN_CANNOT_DELETE_MEETING);
        }
        //只能申请人删除该会议
        if (!isCreator) {
            throw new BusinessException(BusinessExceptionEnum.ONLY_SELF_MEETING_CAN_BE_DELETED);
        }
        //待审批和未开始的会议可以删除
        if (status == 1 || status == 3) {
            int rows = tbMeetingMapper.deleteMeetingApplication(param);

//            if (rows == 1) {
//                String reason = MapUtil.getStr(param, "reason");
//                meetingWorkflowTask.deleteMeetingApplication(uuid, instanceId, reason);
//            }
            /**
             * 上述是借鉴版代码，转换成我自己的工作流。
             * 功能：删除会议；
             */
            if (rows == 1) {
                String reason = MapUtil.getStr(param, "reason");
                activiti7Util.endTaskByInstanceId(reason, instanceId, id.toString());//
            }
            return rows;
        } else {
            throw new BusinessException(BusinessExceptionEnum.ONLE_MEETING_TO_BE_APPROVED_AND_NOTSTARTED_CAN_BE_DELETE);
        }
    }

    /**
     * 查询当前登录用户的待审批会议；类似于查询当前登录用户的待审批会议
     * @param
     * @return
     */
    @Override
    public IPage<TbMeetingDto> searchTaskByPage(Page<TbMeetingDto> page, List<Long> idList) {
        IPage<TbMeetingDto> tbMeetingDtoIPage  =tbMeetingMapper.selectPageVo(page, idList);
        return tbMeetingDtoIPage;
    }
    /**
     * 根据会议申请人的权限编码得到 班级/职位信息
     */
    public String getPosition(Long id) {
        List<String> roleCodeList = getRoleCodeList(id);
        String position = "";
        if (!(roleCodeList.contains("superAdmin")) && !(roleCodeList.contains("admin"))) {
            //这个判断条件说明是学生，学生的话是获取班级信息
            AdmClass classByUserId = admClassMapper.getClassByUserId(id);
            position = classByUserId.getClassName();
        } else if (!(roleCodeList.contains("superAdmin")) && (roleCodeList.contains("admin"))) {
            //这个判断条件说明是导员
            position = "导员";
        } else {
            position = "书记";
        }
        return position;
    }
    /**
     * 审批会议单
     * @param taskId 当前任务在activiti中的taskId
     * @param operatorId 经办人id，经办人肯定是当前系统登录用户。Controller层传来的也是安全框架中的登录用户
     * @param result 审批结果  固定"approved"或"refused"
     * @param review 审批意见
     */
    @Override
    @Transactional
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

        //tbMeeting --- 本次task所属流程对应的业务表的tbMeeting对象
        TbMeeting tbMeeting = tbMeetingMapper.searchById(Long.parseLong(businessKey));
        System.out.println();
        SysUser sysUser = sysUserMapper.selectById(tbMeeting.getCreatorId());//表单提交人信息
        //position是表单提交人也就是会议者的职位信息---"学生班级"/"导员"/"书记"
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
                //true---说明流程已结束，设置会议单业务表的结果字段为approved，然后发送通知
                tbMeetingMapper.updateStatusById(tbMeeting.getId(), (short) 3); //更新到数据库中

                //经办人只能是导员或者书记。书记发起的请求是自动通过的，会在自动通过那里发送通知。所以这里的会议单发起人只能是学生或者导员。
                String noticeContent = String.format("您的会议申请[%s~%s]%s-%s已%s，审批意见：%s，审批流程已结束。"
                        , tbMeeting.getStart(), tbMeeting.getEnd()
                        , operator.getTitle(), operator.getRealname(), strResult, review);//发给表单提交人
                admNoticeMapper.insert(new AdmNotice(Long.parseLong(tbMeeting.getCreatorId().toString()), "您的会议申请已"+strResult, noticeContent, LocalDateTime.now(),4));
                mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                        sysUser.getEmail(),
                        sysUser.getEmail(),
                        "通知管理者",
                        "您的会议申请已"+strResult,
                        "您的会议申请已"+strResult,
                        noticeContent);
                String noticeContentOperator = String.format("%s-%s提起会议申请[%s~%s]您已%s，审批意见：%s，审批流程已结束。",
                        sysUser.getTitle(), sysUser.getRealname()
                        , tbMeeting.getStart(), tbMeeting.getEnd(),
                        strResult, review);//发给审批人的通知
                admNoticeMapper.insert(new AdmNotice(operator.getId(), "您有新的会议审批任务已完成", noticeContentOperator, LocalDateTime.now(), 4));
                mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                        operator.getEmail(),
                        operator.getEmail(),
                        "通知管理者",
                        "您有新的会议审批任务已完成",
                        "您有新的会议审批任务已完成",
                        noticeContent);
            } else {
                //任务complete，但是流程实例还没有结束，发送通知
                tbMeetingMapper.updateStatusById(tbMeeting.getId(), (short) 1);
                if (position.equals("导员")) {
                    //当前经办人是导员。
                    //消息1: 通知表单提交人,导员已经审批通过,交由上级继续审批
                    String noticeContent1 = String.format("您的会议申请[%s~%s]%s-%s已批准，审批意见：%s ，请继续等待上级审批。",
                            tbMeeting.getStart(), tbMeeting.getEnd(),
                            operator.getTitle(), operator.getRealname(), review);
                    admNoticeMapper.insert(new AdmNotice(sysUser.getId(), "您的会议申请有新进展", noticeContent1, LocalDateTime.now(),4));
                    mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                            sysUser.getEmail(),
                            sysUser.getEmail(),
                            "通知管理者",
                            "您的会议申请有新进展",
                            "您的会议申请有新进展",
                            noticeContent1);
                    //消息2: 通知书记有新的审批任务
                    String noticeContent2 = String.format("%s-%s提起会议申请[%s~%s]，请尽快审批。",
                            sysUser.getTitle(), sysUser.getRealname(), tbMeeting.getStart(), tbMeeting.getEnd());
                    admNoticeMapper.insert(new AdmNotice(secretary.getId(), "您有新的会议审批任务", noticeContent2, LocalDateTime.now(),4));
                    mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                            secretary.getEmail(),
                            secretary.getEmail(),
                            "通知管理者",
                            "您有新的会议审批任务",
                            "您有新的会议审批任务",
                            noticeContent2);


                    //消息3: 通知导员,学生的申请单你已批准,交由上级继续审批
                    String noticeContent3 = String.format("%s-%s提起会议申请[%s~%s]您已批准，审批意见：%s，申请转至上级领导继续审批。"
                            , classByUserId.getClassName(), sysUser.getRealname(), tbMeeting.getStart(), tbMeeting.getEnd(), review);
                    admNoticeMapper.insert(new AdmNotice(operator.getId(), "您有新的会议审批任务已完成", noticeContent3, LocalDateTime.now(), 4));
                    mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                            operator.getEmail(),
                            operator.getEmail(),
                            "通知管理者",
                            "您有新的会议审批任务已完成",
                            "您有新的会议审批任务已完成",
                            noticeContent3);
                }
                //else if position == 书记，就会跳到经办人是书记的审批环节，所以这里不需要发送什么通知。
                // 意思是导员发起的任务经办人是书记。跳到经办人是书记的审批环节，书记审批就是最后一个环节，所以符合 if (isEndProcessInstance(processInstanceId))
            }
        } else {
            //说明审批结果为拒绝---refused   --- 拒绝代表着流程直接结束
            activiti7Util.endTaskByInstanceId(review, processInstanceId, operatorId.toString());
            tbMeetingMapper.updateStatusById(tbMeeting.getId(), (short) 2);
            //该流程实例直接结束，发送通知
            //消息1: 通知申请人表单已被驳回
            String noticeContent1 = String.format("您的会议申请[%s~%s]%s-%s已驳回，审批意见：%s,审批流程已结束。",
                    tbMeeting.getStart(), tbMeeting.getEnd(),
                    operator.getTitle(), operator.getRealname(), review);
            admNoticeMapper.insert(new AdmNotice(tbMeeting.getCreatorId().longValue(), "您的会议申请已驳回", noticeContent1, LocalDateTime.now(),4));
            mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                    sysUser.getEmail(),
                    sysUser.getEmail(),
                    "通知管理者",
                    "您的会议申请已驳回",
                    "您的会议申请已驳回",
                    noticeContent1);
            if (position.equals("导员")) {
                //消息2: 通知经办人表单"您已驳回"  --- position.equals("导员"):表单提交人是导员
                String noticeContent2 = String.format("导员：%s提起会议申请[%s~%s]您已驳回，审批意见：%s，审批流程已结束。",
                        sysUser.getRealname(), tbMeeting.getStart(), tbMeeting.getEnd()
                        , review);
                admNoticeMapper.insert(new AdmNotice(operator.getId(), "您有新的会议审批任务已完成", noticeContent2, LocalDateTime.now(), 4));
                mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                        operator.getEmail(),
                        operator.getEmail(),
                        "通知管理者",
                        "您有新的会议审批任务已完成",
                        "您有新的会议审批任务已完成",
                        noticeContent2);
            } else{
                //消息2: 通知经办人表单"您已驳回"  --- 因为书记自动通过自动发消息，所以这里的else只能是学生
                String noticeContent2 = String.format("%s-%s提起会议申请[%s~%s]您已驳回，审批意见：%s，审批流程已结束。",
                        classByUserId.getClassName(),sysUser.getRealname(), tbMeeting.getStart(), tbMeeting.getEnd()
                        , review);
                admNoticeMapper.insert(new AdmNotice(operator.getId(), "您有新的会议审批任务已完成", noticeContent2, LocalDateTime.now(), 4));
                mailUtil.SendHtmlMailThymeLeaf("1154101064@qq.com",
                        operator.getEmail(),
                        operator.getEmail(),
                        "通知管理者",
                        "您有新的会议审批任务已完成",
                        "您有新的会议审批任务已完成",
                        noticeContent2);
            }


        }
    }
    @Override
    public PageUtil searchOnlineMeetingByPage(HashMap param) {
        ArrayList<HashMap> list = tbMeetingMapper.searchOnlineMeetingByPage(param);
        long count = tbMeetingMapper.searchOnlineMeetingCount(param);
        int start = (Integer) param.get("start");
        int length = (Integer) param.get("length");
        PageUtil pageUtil = new PageUtil(list, count, start, length);
        return pageUtil;
    }

    @Override
    public Long searchRoomIdByUUID(String uuid) {
        if (redisTemplate.hasKey(uuid)) {
            Object temp = redisTemplate.opsForValue().get(uuid);
            long roomId = Long.parseLong(temp.toString());
            return roomId;
        }
        return null;
    }

    @Override
    public ArrayList<HashMap> searchOnlineMeetingMembers(HashMap param) {
        ArrayList<HashMap> list = tbMeetingMapper.searchOnlineMeetingMembers(param);
        return list;
    }

    @Override
    public boolean searchCanCheckinMeeting(HashMap param) {
        long count = tbMeetingMapper.searchCanCheckinMeeting(param);
        return count == 1 ? true : false;
    }

    @Override
    public int updateMeetingPresent(HashMap param) {
        int rows = tbMeetingMapper.updateMeetingPresent(param);
        return rows;
    }


}
