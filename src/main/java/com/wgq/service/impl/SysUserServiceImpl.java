package com.wgq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wgq.entity.*;
import com.wgq.mapper.AdmClassMapper;
import com.wgq.mapper.AdmSpecializedSubjectMapper;
import com.wgq.mapper.SysUserMapper;
import com.wgq.service.SysMenuService;
import com.wgq.service.SysRoleService;
import com.wgq.service.SysUserService;
import com.wgq.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Description:
 * 服务实现类SysUserServiceImpl; extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

	@Autowired
	SysRoleService sysRoleService;

	@Autowired
	SysUserMapper sysUserMapper;

	@Autowired
	SysMenuService sysMenuService;

	@Autowired
	RedisUtil redisUtil;

	@Resource
	AdmClassMapper admClassMapper;

	@Resource
	AdmSpecializedSubjectMapper admSpecializedSubjectMapper;

	@Override
	public SysUser getByUsername(String username) {
		return getOne(new QueryWrapper<SysUser>().eq("username", username));
	}

	@Override
	public String getUserAuthorityInfo(Long userId) {

		SysUser sysUser = sysUserMapper.selectById(userId);
		//  ROLE_admin,ROLE_normal,sys:user:list,....
		String authority = "";

		if (redisUtil.hasKey("GrantedAuthority:" + sysUser.getUsername())) {
			// 优先从缓存获取（为了避免多次查库，因为在JwtAuthenticationFilter认证一次这个方法就会请求一次）
			authority = (String) redisUtil.get("GrantedAuthority:" + sysUser.getUsername());
		} else {
			// 获取角色编码
			List<SysRole> roles = sysRoleService.list(new QueryWrapper<SysRole>()
					.inSql("id", "select role_id from sys_user_role where user_id = " + userId));

			if (roles.size() > 0) {
				String roleCodes = roles.stream().map(r -> "ROLE_" + r.getCode()).collect(Collectors.joining(","));
				authority = roleCodes.concat(",");
			}


			// 获取菜单操作编码
			List<Long> menuIds = sysUserMapper.getNavMenuIds(userId);
			if (menuIds.size() > 0) {

				List<SysMenu> menus = sysMenuService.listByIds(menuIds);
				String menuPerms = menus.stream().map(m -> m.getPerms()).collect(Collectors.joining(","));

				authority = authority.concat(menuPerms);
			}

			redisUtil.set("GrantedAuthority:" + sysUser.getUsername(), authority, 60 * 60);
		}
		return authority;
	}

	/**
	 * 清除redis中缓存的权限 --------------当使用业务“ 分配权限 ”
	 * @param username
	 */
	@Override
	public void clearUserAuthorityInfo(String username) {
		redisUtil.del("GrantedAuthority:" + username);
	}
	/**
	 * 清除当前角色下的所有用户在redis中缓存的权限 ----------当使用业务编辑角色的唯一编码 （比如admin或normal）发生变化
	 * @param roleId
	 */
	@Override
	public void clearUserAuthorityInfoByRoleId(Long roleId) {

		//通过role_id查询到当前role下对应的用户
		List<SysUser> sysUsers = this.list(new QueryWrapper<SysUser>()
				.inSql("id", "select user_id from sys_user_role where role_id = " + roleId));

		sysUsers.forEach(u -> {
			this.clearUserAuthorityInfo(u.getUsername());
		});

	}
	/**
	 * 清除当前角色下的所有用户在redis中缓存的权限 --------------当使用业务编辑菜单的唯一编码时
	 * @param menuId
	 */
	@Override
	public void clearUserAuthorityInfoByMenuId(Long menuId) {
		//通过menu_id查询到当前菜单下对应的用户
		List<SysUser> sysUsers = sysUserMapper.listByMenuId(menuId);
		sysUsers.forEach(u -> {
			this.clearUserAuthorityInfo(u.getUsername());
		});
	}

	@Override
	public ArrayList<HashMap> searchAllUser() {
		ArrayList<HashMap> list = sysUserMapper.searchAllUser();
		return list;
	}

	@Override
	public Map getUserClassAndSubjectById(Long id) {
		AdmClass admClass = admClassMapper.getClassByUserId(id);
		AdmSpecializedSubject admSpecializedSubjectByUserId = admSpecializedSubjectMapper.getAdmSpecializedSubjectByUserId(id);
		Map classAndSubject = new HashMap();
		classAndSubject.put("className", admClass.getClassName());
		classAndSubject.put("specializedSubjectName", admSpecializedSubjectByUserId.getName());
		return classAndSubject;
	}
}
