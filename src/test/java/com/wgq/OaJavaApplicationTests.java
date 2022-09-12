package com.wgq;


import org.thymeleaf.TemplateEngine;
import com.wgq.service.MailService;
import com.wgq.service.MeetingService;
import com.wgq.service.SysUserService;
import com.wgq.utils.Activiti7Util;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.runtime.shared.query.Pageable;
import org.activiti.api.task.runtime.TaskRuntime;
import org.activiti.engine.*;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Comment;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.thymeleaf.context.Context;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RunWith(SpringRunner.class)
@SpringBootTest
class OaJavaApplicationTests {

	@Resource
	private ProcessRuntime processRuntime;
	@Resource
	private TaskRuntime taskRuntime;
	@Resource
	private RuntimeService runtimeService;
	@Resource
	private TaskService taskService;
	@Resource
	SysUserService sysUserService;
	@Resource
	MeetingService meetingService;
	@Resource
	Activiti7Util activiti7Util;
	@Resource
	private MailService mailService;

	@Test
	public void testActBoot() {
		System.out.println(taskRuntime);
	}

	@Test
	public void test1(int m, int n) {
		
	}


	/**
	 * 查看流程定义
	 */
	@Test
	public void contextLoads() {
		Page<ProcessDefinition> processDefinitionPage =
				processRuntime.processDefinitions(Pageable.of(0, 10));
		System.out.println("可用的流程定义数量：" + processDefinitionPage.getTotalItems());
		for (org.activiti.api.process.model.ProcessDefinition pd : processDefinitionPage.getContent()) {
			System.out.println("流程定义：" + pd);
		}
	}

	/**
	 * 启动流程实例----------来创建act相关数据库表。注意设置nullCatalogMeansCurrent=true
	 * 相当于开始了一个流程实例，一个请假流程实例，并且设置好了流程变量，剩下的就是去complete任务就行了。
	 */
	@Test
	public void testStartProcess() {
//		ProcessInstance pi = processRuntime.start(ProcessPayloadBuilder.
//				start().
//				withProcessDefinitionKey("leave").
//				build());
//		System.out.println("流程实例ID：" + pi.getId());
		//设置流程变量---并没有与user关联，只是单纯的设置了1、2、3这几个值，后面会通过取当前登录用户的id与这几个值结合使用
		Map<String, Object> variables = new HashMap<>();
		variables.put("assignee0", 1);
		variables.put("assignee1", 2);
		variables.put("assignee2", 3);
		variables.put("days", 11);
		variables.put("identity", 1);
		ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("leave", variables);
		System.out.println("流程实例ID：" + processInstance.getId());

	}

	/**
	 * *查询任务，并完成自己的任务
	 **/
	@Test
	public void testTask() {
		/**
		 * 根据负责人id  查询任务
		 * TaskQuery---package org.activiti.engine.task;
		 */
		TaskQuery taskQuery = taskService.createTaskQuery().taskAssignee("3");
		List<Task> list = taskQuery.orderByTaskCreateTime().desc().list();
		List<Map<String, Object>> listmap = new ArrayList<Map<String, Object>>();
		for (Task task : list) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("taskid", task.getId());
			map.put("taskname", task.getName());
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
			listmap.add(map);
			System.out.println(map);
			taskService.complete(task.getId());
			System.out.println("任务：" + task.getId() + "已完成！");

		}

	}
	@Test
	public void activitiHistory() {
		List<Comment> commentByTaskId = activiti7Util.findCommentByBusinessKey("116");
		for (Comment comment : commentByTaskId) {
			System.out.println("*:::"+comment.getFullMessage());
		}
	}

	// 发件人要跟yml配置文件里填写的邮箱一致
	String mailFrom = "1154101064@qq.com";
	// 收件人
	String mailTo = "306683915@qq.com";
	// 抄送
	String cc = "306683915@qq.com";
	/**
	 * 1、测试普通邮件发送
	 */
	@Test
	public void testSendSimpleMail() {

		String result = "发送邮件成功";
		try {
			mailService.sendSimpleMail(mailFrom, "一个大帅哥", mailTo, cc, "TestMail", "Hello World !");
		} catch (Exception e) {
			result = "发送邮件失败！";
			System.out.println(result);
			System.out.println(e);
		}
		System.out.println(result);
	}

	/**
	 * 2、测试带附件的方法
	 */
	@Test
	public void testSendAttachment() {
		File imgFile = new File("src\\main\\java\\com\\ztt\\controller\\f1bdd00e8c.jpg");
		File txtFile = new File("src\\main\\java\\com\\ztt\\controller\\hello.txt");
		List<File> fileList = new ArrayList<>();
		fileList.add(imgFile);
		fileList.add(txtFile);

		// 发件人要跟yml配置文件里填写的邮箱一致

		String result = "发送邮件成功";
		try {
			mailService.sendMailWithAttachments(mailFrom, "一个大帅哥", mailTo, cc, "TestMail", "Hello World !", fileList);
		} catch (Exception e) {
			result = "发送邮件失败！";
			System.out.println(result);
			System.out.println(e);
		}
		System.out.println(result);
	}


	/**
	 * 3、正文带图片
	 * 这个方法实现了正文带图片的功能。不同于前两种的是，String content是html格式的文本，里面用cid标注静态资源（本文是src='cid:image01'），String[] imagePaths存储的是图片的路径，String[] imageId存储了每张图片的编号，这个编号是可以自己随便定义的，但是必须跟content里面使用的cid名称一致。
	 */
	@Test
	public void testSendMailWithImage() {
		// 图片路径
		String image01Path = "E:\\personal\\gittest\\学习项目库\\learning_project_library\\SpringBoot_mail\\src\\main\\java\\com\\ztt\\controller\\2ed0c0d5a2.jpg";
		String image02Path = "E:\\personal\\gittest\\学习项目库\\learning_project_library\\SpringBoot_mail\\src\\main\\java\\com\\ztt\\controller\\3bcd0b6866.jpg";
		String[] imageArr = new String[]{image01Path, image02Path};
		String[] imageIdArr = new String[]{"image01", "image02"};

		String result = "发送邮件成功";
		try {
			String contentHtml = "这是图片1:<div><img src='cid:image01'/></div>" +
					"这是图片2:<div><img src='cid:image02'/></div>";
			mailService.sendMailWithImage(mailFrom, "一个大帅哥", mailTo, cc, "TestMail", contentHtml, imageArr, imageIdArr);
		} catch (Exception e) {
			result = "发送邮件失败！";
			System.out.println(result);
			System.out.println(e);
		}
		System.out.println(result);
	}

	/**
	 * 4、使用ThymeLeaf
	 */
	// 注入TemplateEngine
	@Autowired
	TemplateEngine templateEngine;

	@Test
	public void testSendHtmlMailThymeLeaf() {

		// 注意导入的包是org.thymeleaf.context
		Context context = new Context();
		context.setVariable("username", "比尔盖茨");
		context.setVariable("age", "18");
		String content = templateEngine.process("mailTemplate01.html", context);

		mailService.sendHtmlMailThymeLeaf(mailFrom, "一个大帅哥", mailTo, cc, "TestMail", content);

		System.out.println("邮件发送成功");
	}

}

