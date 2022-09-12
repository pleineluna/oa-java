package com.wgq.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wgq.service.*;
import com.wgq.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.ServletRequestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 描述：BaseController
 * 可作为其他Controller的父类，提供了一些基础的注入、方法等。类似于BaseEntity。
 */
public class BaseController {

	@Resource
	HttpServletRequest req;

	@Autowired
	RedisUtil redisUtil;

	@Autowired
	SysUserService sysUserService;

	@Autowired
	SysRoleService sysRoleService;

	@Autowired
	SysMenuService sysMenuService;

	@Autowired
	SysUserRoleService sysUserRoleService;

	@Autowired
	SysRoleMenuService sysRoleMenuService;

	@Resource
	AdmLeaveFormService admLeaveFormService;

	@Resource
	AdmNoticeService admNoticeService;

	@Resource
	AdmScholarshipService admScholarshipService;

	@Resource
	AdmScholarshipSysUserService admScholarshipSysUserService;


	/**
	 * 根据前端传入的size、current参数创建满足前端条件的Page对象
	 * @return Page 是mybatisplus的分页对象
	 */
	public Page getPage() {
		//为了分页功能的正常，要进行default设置
		int current = ServletRequestUtils.getIntParameter(req, "current", 1);
		int size = ServletRequestUtils.getIntParameter(req, "size", 10);

		return new Page(current, size);
	}

}
