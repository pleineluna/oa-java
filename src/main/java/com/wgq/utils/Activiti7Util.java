package com.wgq.utils;

import com.wgq.entity.AdmLeaveForm;
import com.wgq.entity.SysUser;
import com.wgq.mapper.AdmLeaveFormMapper;
import com.wgq.mapper.SysUserMapper;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.impl.identity.Authentication;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Comment;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Description:
 * Activiti7的工具操作类
 */
@Component
public class Activiti7Util {
    @Resource
    AdmLeaveFormMapper admLeaveFormMapper;

    @Resource
    SysUserMapper sysUserMapper;

    @Resource
    private RuntimeService runtimeService;

    @Resource
    private TaskService taskService;

    @Resource
    HistoryService historyService;

    /**
     * 启动流程实例
     * @param variables 流程变量
     * @param businessKey ~
     * @return ProcessInstance
     */
    public ProcessInstance createProcessInstance(Map<String, Object> variables, String businessKey,String ProcessInstanceKey) {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(ProcessInstanceKey, businessKey, variables);
        System.out.println("*本次创建的processInstanceId为：" + processInstance.getId());
        return processInstance;
    }

    /**
     * 查看个人任务列表
     * @param userId  用户的id
     * @return List<Map<String, Object>>类型任务列表
     */
    public List<Map<String, Object>> myTaskList(String userId,String ProcessInstanceKey) {
        /**
         * 根据负责人id  查询任务
         * TaskQuery---package org.activiti.engine.task;
         */
        //流程定义Key，因为不止会议流程，还有请假流程，所以不能再查询当前用户的所有ru_task，需要指定查询那个流程定义的ru_task
        String processDefinitionKey = ProcessInstanceKey;
        TaskQuery taskQuery = taskService
                .createTaskQuery()
                .taskAssignee(userId)
                .processDefinitionKey(processDefinitionKey);
        List<Task> list = taskQuery.orderByTaskCreateTime().desc().list();
        List<Map<String, Object>> listmap = new ArrayList<Map<String, Object>>();
        for (Task task : list) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("taskId", task.getId());
            map.put("taskName", task.getName());
            map.put("description", task.getDescription());
            map.put("priority", task.getPriority());
            map.put("owner", task.getOwner());
            map.put("assignee", task.getAssignee());
            map.put("delegationState", task.getDelegationState());
            map.put("processInstanceId", task.getProcessInstanceId());
            map.put("executionId", task.getExecutionId());
            map.put("processDefinitionId", task.getProcessDefinitionId());
            map.put("createTime", task.getCreateTime());
            map.put("taskDefinitionKey", task.getTaskDefinitionKey());
            map.put("dueDate", task.getDueDate());
            map.put("category", task.getCategory());
            map.put("parentTaskId", task.getParentTaskId());
            map.put("tenantId", task.getTenantId());
            //task.getAssignee()可以得到某个任务的负责人。
            SysUser sysUser = sysUserMapper.selectById(Long.valueOf(task.getAssignee()));
            map.put("assigneeUser", sysUser.getUsername());
            //拿到当前流程实例
            ProcessInstance processInstance = runtimeService
                    .createProcessInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId()).singleResult();
            //拿到businessKey
            String businessKey = processInstance.getBusinessKey();
            map.put("businessKey", businessKey);
            listmap.add(map);
        }
        return listmap;
    }

    /**
     * 查看个人任务信息
     */
    public List<Map<String, Object>> myTaskInfoList(String userId) {
        /**
         * 根据负责人id  查询任务
         */
        TaskQuery taskQuery = taskService.createTaskQuery().taskAssignee(userId);
        List<Task> list = taskQuery.orderByTaskCreateTime().desc().list();
        List<Map<String, Object>> listmap = new ArrayList<Map<String, Object>>();
        for (Task task : list) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("taskId", task.getId());
            map.put("assignee", task.getAssignee());
            map.put("processInstanceId", task.getProcessInstanceId());
            map.put("executionId", task.getExecutionId());
            map.put("processDefinitionId", task.getProcessDefinitionId());
            map.put("createTime", task.getCreateTime());
            ProcessInstance processInstance = runtimeService
                    .createProcessInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .singleResult();
            if (processInstance != null) {
                String businessKey = processInstance.getBusinessKey();
                if (!StringUtils.isBlank(businessKey)) {
                    AdmLeaveForm admLeaveForm = admLeaveFormMapper.selectById(businessKey);
                    SysUser sysUser = sysUserMapper.selectById(Long.valueOf(task.getAssignee()));
                    map.put("flowUserName", sysUser.getUsername());
                    map.put("flowType", "出差申请");
                    long diff = admLeaveForm.getEndTime().toInstant(ZoneOffset.of("+8")).toEpochMilli() - admLeaveForm.getStartTime().toInstant(ZoneOffset.of("+8")).toEpochMilli();
                    float days = (diff / (1000 * 60 * 60) * 1f) / 24;
                    map.put("flowcontent", "出差" + days + "天");
                }
            }
            listmap.add(map);
        }
        return listmap;
    }

    /**
     * 该流程实例是否结束
     * @param processInstanceId
     * @return  true:该流程已结束
     */
    public boolean isEndProcessInstance(String processInstanceId) {
        //根据流程实例id查询表act_ru_task中是否还有正在执行的任务信息，如果返回值为空，则该流程已完成。
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        //判断返回的结果，如果结果==null,流程执行完成。
        if (processInstance == null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * activiti拒接任务，直接结束该流程实例
     * @param remark 驳回意见
     * @param userId
     * @param taskId    当前任务ID
     */
    public void endTask(String remark,String taskId,String userId) {
        //  当前任务
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();

        //任务对象的流程实例Id
        String processInstanceId = task.getProcessInstanceId();

        //设置审批人的userId--为的是让你查询 act_hi_procinst 的 start_user_id 不为空----由于流程用户上下文对象是线程独立的，所以要在需要的位置设置，要保证设置和获取操作在同一个线程中
        // 以userId作为标识
        Authentication.setAuthenticatedUserId(userId);

        //添加记录  --- activiti的api，可以把本次记录添加到act_hi_comment表中。remake是对该记录的信息，对应字段MESSAGE_
        taskService.addComment(taskId, processInstanceId, remark);

        runtimeService.deleteProcessInstance(task.getProcessInstanceId(), "任务拒绝---本次流程实例结束！！！");
        System.out.println("任务拒绝---本次流程实例结束！！！");
    }
    /**
     * activiti拒接任务，直接结束该流程实例
     * @param remark 驳回意见
     * @param userId
     * @param
     */
    public void endTaskByInstanceId(String remark, String instanceId, String userId) {
        //设置审批人的userId--为的是让你查询 act_hi_procinst 的 start_user_id 不为空----由于流程用户上下文对象是线程独立的，所以要在需要的位置设置，要保证设置和获取操作在同一个线程中
        // 以userId作为标识
        Authentication.setAuthenticatedUserId(userId);

        runtimeService.deleteProcessInstance(instanceId, "任务拒绝---本次流程实例结束！！！");
        System.out.println("任务拒绝---本次流程实例结束！！！");
    }
    /**
     * 完成提交任务
     * @param remark 同意意见
     * @param userId
     * @param taskId
     */
    public void completeProcess(String remark, String taskId, String userId) {
        //任务Id 查询任务对象  --- 从act_ru_task表中查询
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            System.out.println("任务不存在！！！");
            return;
        }
        //任务对象  获取流程实例Id
        String processInstanceId = task.getProcessInstanceId();

        //设置审批人的userId--为的是让你查询 act_hi_procinst 的 start_user_id 不为空----由于流程用户上下文对象是线程独立的，所以要在需要的位置设置，要保证设置和获取操作在同一个线程中
        Authentication.setAuthenticatedUserId(userId);

        //添加记录  --- activiti的api，可以把本次记录添加到act_hi_comment表中。remake是对该记录的信息，对应字段MESSAGE_
        taskService.addComment(taskId, processInstanceId, remark);
        System.out.println("-----------完成任务操作 开始----------");
        System.out.println("任务Id=" + taskId);
        System.out.println("负责人id=" + userId);
        System.out.println("流程实例id=" + processInstanceId);
        //完成办理
        taskService.complete(taskId);
        System.out.println("-----------完成任务操作 结束----------");
    }

    /**
     * 查询历史记录 ---
     *
     * @param businessKey
     */
    public void searchHistory(String businessKey) {
        List<HistoricProcessInstance> list1 = historyService.createHistoricProcessInstanceQuery().processInstanceBusinessKey(businessKey).list();
        if (CollectionUtils.isEmpty(list1)) {
            return;
        }
        String processDefinitionId = list1.get(0).getProcessDefinitionId();
        // 历史相关Service
        List<HistoricActivityInstance> list = historyService
                .createHistoricActivityInstanceQuery()
                .processDefinitionId(processDefinitionId)
                .orderByHistoricActivityInstanceStartTime()
                .asc()
                .list();
        for (HistoricActivityInstance hiact : list) {
            if (StringUtils.isBlank(hiact.getAssignee())) {
                continue;
            }
            System.out.println("=========================查询历史记录=============================");
            System.out.println("活动ID:" + hiact.getId());
            System.out.println("流程实例ID:" + hiact.getProcessInstanceId());
            SysUser sysUser = sysUserMapper.selectById(Long.valueOf(hiact.getAssignee()));
            System.out.println("办理人ID：" + hiact.getAssignee());
            System.out.println("办理人名字：" + sysUser.getUsername());
            System.out.println("开始时间：" + hiact.getStartTime());
            System.out.println("结束时间：" + hiact.getEndTime());
            System.out.println("==================================================================");
        }
    }

    /**
     * 查看某个流程实例的所有过程的批注信息。比如，自身创建时候的批注信息（默认为固定值），导员的批注信息，书记的批注信息
     * 根据taskId查看message。也就是批注信息，在act_hi_commit中,用于保存流程审核的批注信息
     * 如何添加批注信息  taskServer.addComment(taskId:任务 id ,processInstanceId: 流程实例 id ,message : 批注信息);
     * 一般情况下,activiti中我们可以通过流程实例id来做关于流程实例的一些操作，有些操作也是可以通过businessKey的，但是有些处理activiti中默认只提供了流程实例id的方法，比如流程实例的挂起，删除，恢复，这几个方法中传入参数必须为processInstanceId，但是在和业务工程关联时，业务调用方更希望全部通过businessKey来操作流程实例，所以我们只要能通过businessKey得到流程实例id即可解决所有问题。
     * 需要注意的是，activiti中processRuntime .processInstance()只能取得还在运行中的流程实例，而不能获取已经完结的流程实例，而historyService .createHistoricProcessInstanceQuery()方法可以得到所有的流程实例（包括运行中和完结的）。而这个方法是可以通过businessKey作为条件来查询的，返回结果就是流程实例对象，所以通过businessKey就得到了流程实例对象，再以后业务方完全可以不用存流程实例id了，直接通过businessKey就可以代替流程实例id来做相关的操作了。
     * @return List<Comment> 某个流程实例的所有过程的批注信息Comment对象
     */
    public List<Comment> findCommentByBusinessKey(String businessKey) {
        List<HistoricTaskInstance> list = historyService.createHistoricTaskInstanceQuery()
                .processInstanceBusinessKey(businessKey).list();
        List<Comment> comments = new ArrayList<>();
        for (HistoricTaskInstance hisTask : list) {
            // 批注信息集合
            List<Comment> taskComments = taskService.getTaskComments(hisTask.getId());
            for (Comment comment : taskComments) {
                comments.add(comment);
            }
        }
        return comments;
    }
}
