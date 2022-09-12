package com.wgq.controller;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wgq.common.exception.BusinessException;
import com.wgq.common.exception.BusinessExceptionEnum;
import com.wgq.common.lang.Const;
import com.wgq.common.lang.Result;
import com.wgq.entity.SysRole;
import com.wgq.entity.SysRoleMenu;
import com.wgq.entity.SysUserRole;
import com.wgq.mapper.SysRoleMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 描述：SysRoleController前端控制器
 */
@RestController
@RequestMapping("/sys/role")
public class SysRoleController extends BaseController {

	@Resource
	SysRoleMapper sysRoleMapper;

	/**
	 * 根据 id 查询，可能没有对应的业务功能，但是其他地方需要用到这个逻辑，比如“编辑”、“分配权限”时 回填信息需要
	 *
	 * @param id
	 * @return
	 */
	@PreAuthorize("hasAuthority('sys:role:list')")
	@GetMapping("/info/{id}")
	public Result info(@PathVariable("id") Long id) {

		SysRole sysRole = sysRoleService.getById(id);

		// 获取角色Role相关联的菜单Menu的id
		List<SysRoleMenu> roleMenus = sysRoleMenuService.list(new QueryWrapper<SysRoleMenu>().eq("role_id", id));
		//通过流式处理得到所有menuId并转为List类型
		List<Long> menuIds = roleMenus.stream().map(p -> p.getMenuId()).collect(Collectors.toList());

		sysRole.setMenuIds(menuIds);
		return Result.succ(sysRole);
	}

	/**
	 * 根据 名称 查询，也就是对应前端搜索框业务功能
	 *
	 * @param name
	 * @return Result.succ(pageData);
	 */
	@PreAuthorize("hasAuthority('sys:role:list')")
	@GetMapping("/list")
	public Result list(String name) {
		IPage iPage = sysRoleMapper.selectPageVo(getPage(), name);
		return Result.succ(iPage);
	}

	@PostMapping("/save")
	@PreAuthorize("hasAuthority('sys:role:save')")
	@Transactional
	public Result save(@Validated @RequestBody SysRole sysRole) {
		/**
		 * 当然开发阶段是允许的！
		 * 系统最高管理员角色（code=TopManagement）不允许创建
		 */
		if (sysRole.getCode().equals("TopManagement")) {
			throw new BusinessException(BusinessExceptionEnum.FAILED_TO_ADD_SYSROLE_TOPMANAGEMENT);
		}

		sysRole.setCreated(LocalDateTime.now());
		sysRole.setStatu(Const.STATUS_ON);
		try {
			sysRoleService.save(sysRole);
		} catch (Exception e) {
			throw new BusinessException(BusinessExceptionEnum.SYSROLE_CREATE_ERRPR);
		}
		return Result.succ(sysRole);
	}

	@PostMapping("/update")
	@PreAuthorize("hasAuthority('sys:role:update')")
	@Transactional
	public Result update(@Validated @RequestBody SysRole sysRole) {

		sysRole.setUpdated(LocalDateTime.now());

		sysRoleService.updateById(sysRole);

		// 更新缓存
		sysUserService.clearUserAuthorityInfoByRoleId(sysRole.getId());

		return Result.succ(sysRole);
	}

	@PostMapping("/delete")
	@PreAuthorize("hasAuthority('sys:role:delete')")
	//这种写操作一定要开启事务
	@Transactional
	public Result delete(@RequestBody Long[] ids) {
		//查询要删除的role是否包含code="TopManagement"系统最高管理员，这个role是不允许被前端操作的。
		SysRole sysRoleTopManagement = sysRoleService.getOne(new QueryWrapper<SysRole>().eq("code", "TopManagement"));
		List<Long> idList = new ArrayList<Long>(Arrays.asList(ids));
//		List<Long> idList = Arrays.asList(ids);
		if (idList.contains(sysRoleTopManagement.getId())) {
			//如果要删除的role的id包含code="TopManagement"系统最高管理员的id，那么排除掉。
			throw new BusinessException(BusinessExceptionEnum.FAILED_TO_DELETE_SYSROLE_TOPMANAGEMENT);
		}
		//Arrays.asList() 数组转列表 因为mybatis-plus的removeByIds方法参数是list类型
		sysRoleService.removeByIds(idList);

		// 删除中间表
		sysUserRoleService.remove(new QueryWrapper<SysUserRole>().in("role_id", ids));
		sysRoleMenuService.remove(new QueryWrapper<SysRoleMenu>().in("role_id", ids));

		// 缓存同步删除
		Arrays.stream(ids).forEach(id -> {
			// 更新缓存
			sysUserService.clearUserAuthorityInfoByRoleId(id);
		});

		return Result.succ("恭喜你，操作成功");
	}

	/**
	 * “分配权限” 业务功能
	 *
	 * @param roleId  {roleId}是哪个角色要进行“分配权限” 操作
	 * @param menuIds 最终分配的权限id的数组【前端也统一传来数组】
	 * @return
	 */
	@PostMapping("/perm/{roleId}")
	@PreAuthorize("hasAuthority('sys:role:perm')")
	@Transactional
	public Result info(@PathVariable("roleId") Long roleId, @RequestBody Long[] menuIds) {
		SysRole sysRoleTopManagement = sysRoleService.getOne(new QueryWrapper<SysRole>().eq("code", "TopManagement"));
		if (sysRoleTopManagement.getId() == roleId) {
			/**
			 * 当然！！！测试阶段是允许的，只有最后项目上线后才不允许。
			 * 系统最高管理员角色（code=TopManagement）拥有所有权限！不允许分配权限！
			 */
			throw new BusinessException(BusinessExceptionEnum.FAILED_TO_DISTRIBUTE_SYSROLE_TOPMANAGEMENT_ROLES);
		}

		//根据传来的menuIds遍历转化为 SysRoleMenu 中间类存到List里
		List<SysRoleMenu> sysRoleMenus = new ArrayList<>();
		Arrays.stream(menuIds).forEach(menuId -> {
			SysRoleMenu roleMenu = new SysRoleMenu();
			roleMenu.setMenuId(menuId);
			roleMenu.setRoleId(roleId);

			sysRoleMenus.add(roleMenu);
		});

		// 批处理保存到数据库中，一定要先删除原来的记录，再保存新的
		sysRoleMenuService.remove(new QueryWrapper<SysRoleMenu>().eq("role_id", roleId));
		sysRoleMenuService.saveBatch(sysRoleMenus);

		// 删除缓存
		sysUserService.clearUserAuthorityInfoByRoleId(roleId);

		return Result.succ(menuIds);
	}

}
