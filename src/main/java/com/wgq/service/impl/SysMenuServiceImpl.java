package com.wgq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wgq.entity.dto.SysMenuDto;
import com.wgq.entity.SysMenu;
import com.wgq.entity.SysUser;
import com.wgq.mapper.SysMenuMapper;
import com.wgq.mapper.SysUserMapper;
import com.wgq.service.SysMenuService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wgq.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Description:
 * 服务实现类SysMenuServiceImpl; extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService
 * 继承 extends ServiceImpl<SysMenuMapper, SysMenu> ---com.baomidou.mybatisplus.extension.service.impl;
 * 实现implements SysMenuService
 */
@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

	@Autowired
	SysUserService sysUserService;

	@Autowired
	SysUserMapper sysUserMapper;

	@Override
	public List<SysMenuDto> getCurrentUserNav() {
		String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		SysUser sysUser = sysUserService.getByUsername(username);

		List<Long> menuIds = sysUserMapper.getNavMenuIds(sysUser.getId());
		List<SysMenu> menus = this.listByIds(menuIds);
		//排序，实现nav导航栏按照orderNumber排序
		Collections.sort(menus);
		System.out.println(menus);
		// 转树状结构
		List<SysMenu> menuTree = buildTreeMenu(menus);
		System.out.println("CurrentUserNav:"+convert(menuTree));
		// 实体转DTO
		return convert(menuTree);
	}

	@Override
	public List<SysMenu> tree() {
		// 获取所有菜单信息
		//.list()里面什么也不写就是没有条件也就是查询所有。
		List<SysMenu> sysMenus = this.list(new QueryWrapper<SysMenu>().orderByAsc("orderNum"));
		// 转成树状结构
		return buildTreeMenu(sysMenus);
	}

	private List<SysMenuDto> convert(List<SysMenu> menuTree) {
		List<SysMenuDto> menuDtos = new ArrayList<>();

		menuTree.forEach(m -> {
			SysMenuDto dto = new SysMenuDto();

			dto.setId(m.getId());
			dto.setName(m.getPerms());
			dto.setTitle(m.getName());
			dto.setIcon(m.getIcon());
			dto.setComponent(m.getComponent());
			dto.setPath(m.getPath());

			if (m.getChildren().size() > 0) {

				// 子节点调用当前方法进行再次转换
				dto.setChildren(convert(m.getChildren()));
			}

			menuDtos.add(dto);
		});

		return menuDtos;
	}

	private List<SysMenu> buildTreeMenu(List<SysMenu> menus) {

		List<SysMenu> finalMenus = new ArrayList<>();

		for (SysMenu menu : menus) {
			// 先各自寻找到各自的孩子
			for (SysMenu e : menus) {
				if (menu.getId() == e.getParentId()) {
					menu.getChildren().add(e);
				}
			}
			// 提取出父节点
			if (menu.getParentId() == 0L) {
				finalMenus.add(menu);
			}
		}

		return finalMenus;
	}
}
