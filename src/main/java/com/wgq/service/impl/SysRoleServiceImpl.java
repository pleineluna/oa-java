package com.wgq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wgq.entity.SysRole;
import com.wgq.mapper.SysRoleMapper;
import com.wgq.service.SysRoleService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Description:
 * 服务实现类SysRoleServiceImpl; extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService
 *
 */
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

	@Override
	public List<SysRole>  listRolesByUserId(Long userId) {

		List<SysRole> sysRoles = this.list(new QueryWrapper<SysRole>()
				.inSql("id", "select role_id from sys_user_role where user_id = " + userId));

		return sysRoles;
	}
}
