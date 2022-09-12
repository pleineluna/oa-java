package com.wgq.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wgq.entity.dto.ChangePersonalInfoDto;
import com.wgq.entity.dto.PassDto;
import com.wgq.common.exception.BusinessException;
import com.wgq.common.exception.BusinessExceptionEnum;
import com.wgq.common.lang.Const;
import com.wgq.common.lang.Result;
import com.wgq.controller.form.SearchNameAndDeptForm;
import com.wgq.entity.SysRole;
import com.wgq.entity.SysUser;
import com.wgq.entity.SysUserRole;
import com.wgq.mapper.SysUserMapper;
import com.wgq.utils.FastDFSUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;

/**
 *描述：SysUserController前端控制器
 */
@RestController
@RequestMapping("/sys/user")
public class SysUserController extends BaseController {

	@Autowired
	BCryptPasswordEncoder passwordEncoder;

	@Resource
	SysUserMapper sysUserMapper;

	@Value("${fastdfs.nginx.host}")
	String nginxHost;

	@GetMapping("/sysUserInfo")
	public Result sysUserInfo(Principal principal) {
		SysUser sysUser = sysUserService.getByUsername(principal.getName());
		return Result.succ(sysUser);
	}

	@GetMapping("/sysUserClassAndSubject")
	public Result sysUserClassAndSubject(Principal principal) {
		SysUser sysUser = sysUserService.getByUsername(principal.getName());
		Map map = sysUserService.getUserClassAndSubjectById(sysUser.getId());
		return Result.succ(map);
	}

	@GetMapping("/info/{id}")
	@PreAuthorize("hasAuthority('sys:user:list')")
	public Result info(@PathVariable("id") Long id) {
		SysUser sysUser = sysUserService.getById(id);
		Assert.notNull(sysUser, "找不到该管理员");
		//查出用户对应的角色并设置到SysUser的对应属性里  给业务功能 “分配角色” 回填数据，“角色名称”展示 都需要知道用户拥有哪些角色
		List<SysRole> roles = sysRoleService.listRolesByUserId(id);
		sysUser.setSysRoles(roles);
		return Result.succ(sysUser);
	}

	@GetMapping("/list")
	@PreAuthorize("hasAuthority('sys:user:list')")
	public Result list(String username) {
		IPage iPage = sysUserMapper.selectPageVo(getPage(), username);
		List<SysUser> records = iPage.getRecords();

		records.forEach(u -> {

			u.setSysRoles(sysRoleService.listRolesByUserId(u.getId()));
		});
		iPage.setRecords(records);

		return Result.succ(iPage);
	}

	/**
	 * "查询所有用户[借鉴项目的特殊版]"
	 * @return
	 */
	@GetMapping("/searchAllUser")
	public Result searchAllUser() {
		ArrayList<HashMap> list = sysUserService.searchAllUser();
		System.out.println(list);
		return Result.succ(list);
	}

	/**
	 * "查找员工姓名和部门"
	 * @param form
	 * @return
	 */
	@PostMapping("/searchNameAndDept")
	public Result searchNameAndDept(@Valid @RequestBody SearchNameAndDeptForm form){
		HashMap map=sysUserMapper.searchName(form.getId().longValue());
		map.put("dept", "部门暂时测试用");
		return Result.succ(map);
	}

	@PostMapping("/save")
	@PreAuthorize("hasAuthority('sys:user:save')")
	@Transactional
	public Result save(@Validated @RequestBody SysUser sysUser) {
		sysUser.setCreated(LocalDateTime.now());

		// 默认密码
		String password = passwordEncoder.encode(Const.DEFULT_PASSWORD);
		sysUser.setPassword(password);

		// 默认头像
		sysUser.setAvatar(Const.DEFULT_AVATAR);
		try {
			sysUserService.save(sysUser);
		} catch (Exception e) {
			throw new BusinessException(BusinessExceptionEnum.SYSUSER_CREATE_ERRPR);
		}
		return Result.succ(sysUser);
	}

	@PostMapping("/update")
	@PreAuthorize("hasAuthority('sys:user:update')")
	@Transactional
	public Result update(@Validated @RequestBody SysUser sysUser) {

		sysUser.setUpdated(LocalDateTime.now());

		sysUserService.updateById(sysUser);
		return Result.succ(sysUser);
	}

	@Transactional
	@PostMapping("/delete")
	@PreAuthorize("hasAuthority('sys:user:delete')")
	public Result delete(@RequestBody Long[] ids) {
		SysUser sysUserTopManagement = sysUserService.getOne(new QueryWrapper<SysUser>().eq("is_top_management", "1"));
		List<Long> idList = new ArrayList<>(Arrays.asList(ids));
		if (idList.contains(sysUserTopManagement.getId())) {
			throw new BusinessException(BusinessExceptionEnum.FAILED_TO_DELETE_SYSUSER_TOPMANAGEMENT);
		}
		sysUserService.removeByIds(idList);
		sysUserRoleService.remove(new QueryWrapper<SysUserRole>().in("user_id", idList)); //删除关联表中的记录

		return Result.succ("恭喜你，操作成功");
	}

	/**
	 * 给用户”分配角色“ 业务功能
	 * @param userId  用户id
	 * @param roleIds 角色id数组
	 * @return
	 */
	@Transactional
	@PostMapping("/role/{userId}")
	@PreAuthorize("hasAuthority('sys:user:role')")
	public Result rolePerm(@PathVariable("userId") Long userId, @RequestBody Long[] roleIds) {
		SysRole sysRoleTopManagement = sysRoleService.getOne(new QueryWrapper<SysRole>().eq("code", "TopManagement"));
		List<Long> idList = new ArrayList<>(Arrays.asList(roleIds));
		if (idList.contains(sysRoleTopManagement.getId())) {
			/**
			 * 当然开发阶段可以。
			 * 系统最高管理者用户不允许分配权限！虽然前台也没有这个按钮，但是前端数据是不可靠的，可以随便修改传给后端，所以后端还是需要校验。
			 */
			throw new BusinessException(BusinessExceptionEnum.FAILED_TO_DISTRIBUTE_SYSUSER_TOPMANAGEMENT_ROLES);
		}
		/**
		 * 保证用户角色一对一关系。
		 * 这里也是，虽然前段根本不提供入口，但是前端数据是不可靠的，可以随便修改传给后端，所以后端还是需要校验。
		 */
		if (roleIds.length > 1) {
			throw new BusinessException(BusinessExceptionEnum.ONLY_DISTRIBUTE_ONE_ROLE);
		}
		//userRoles 中间表
		List<SysUserRole> userRoles = new ArrayList<>();

		//流式循环生成中间表对象
		Arrays.stream(roleIds).forEach(r -> {
			SysUserRole sysUserRole = new SysUserRole();
			sysUserRole.setRoleId(r);
			sysUserRole.setUserId(userId);

			userRoles.add(sysUserRole);
		});
		//删除之前的中间表记录 、 插入新的中间表记录
		sysUserRoleService.remove(new QueryWrapper<SysUserRole>().eq("user_id", userId));
		sysUserRoleService.saveBatch(userRoles);

		// 删除缓存
		SysUser sysUser = sysUserService.getById(userId);
		sysUserService.clearUserAuthorityInfo(sysUser.getUsername());

		return Result.succ("");
	}

	/**
	 * 重置密码为默认密码 ： Const.DEFULT_PASSWORD) 【888888】
	 * @param userId
	 * @return
	 */
	@PostMapping("/repass")
	@PreAuthorize("hasAuthority('sys:user:repass')")
	@Transactional
	public Result repass(@RequestBody Long userId) {

		SysUser sysUser = sysUserService.getById(userId);

		sysUser.setPassword(passwordEncoder.encode(Const.DEFULT_PASSWORD));
		sysUser.setUpdated(LocalDateTime.now());

		sysUserService.updateById(sysUser);
		return Result.succ("");
	}



	/**
	 * 修改密码
	 * @param passDto
	 * @param principal
	 * @return
	 */
	@PostMapping("/updatePass")
	@Transactional
	public Result updatePass(@Validated @RequestBody PassDto passDto, Principal principal) {

		SysUser sysUser = sysUserService.getByUsername(principal.getName());

		boolean matches = passwordEncoder.matches(passDto.getCurrentPass(), sysUser.getPassword());
		if (!matches) {
			return Result.fail("旧密码不正确");
		}

		sysUser.setPassword(passwordEncoder.encode(passDto.getPassword()));
		sysUser.setUpdated(LocalDateTime.now());

		sysUserService.updateById(sysUser);
		return Result.succ("");
	}

	/**
	 * PersonalInfo.vue模块 修改个人信息  对应前端的用户中心-个人资料
	 * @param changePersonalInfoDto
	 * @param principal
	 * @return
	 */
	@Transactional
	@PostMapping("/changePersonalInfo")
	public Result changePersonalInfo(@Validated @RequestBody ChangePersonalInfoDto changePersonalInfoDto,Principal principal) {
		SysUser sysUser = sysUserService.getByUsername(principal.getName());
		sysUser.setUsername(changePersonalInfoDto.getUsername());
		sysUser.setEmail(changePersonalInfoDto.getEmail());
		sysUser.setBankAccount(changePersonalInfoDto.getBankAccount());
		sysUser.setPhone(changePersonalInfoDto.getPhone());
		sysUserMapper.updateById(sysUser);
		return Result.succ("");
	}

	/**
	 *
	 * @param files
	 * @param principal
	 * @return
	 */
	@Transactional
	@PostMapping("/changeAvatar")
	public Result changeAvatar(@RequestParam("file") MultipartFile[] files, Principal principal) {
		SysUser sysUser = sysUserService.getByUsername(principal.getName());
		if (files != null && files.length > 0) {
			String urls = "";
			for (MultipartFile file : files) {
				String filePath = FastDFSUtil.upload(file);//上传到服务器，在服务器中的地址
				System.out.println(filePath);
				urls += (nginxHost + filePath);
			}
			sysUser.setAvatar(urls);
			sysUserService.updateById(sysUser);
			System.out.println(urls);
			System.out.println("更换头像成功");
			return Result.succ("更换头像成功");
		} else {
			System.out.println("更换头像成功失败");
			return Result.fail("更换头像成功失败");
		}
	}

}
