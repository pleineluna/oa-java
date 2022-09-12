package com.wgq.service;

import com.wgq.entity.dto.SysMenuDto;
import com.wgq.entity.SysMenu;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * Description:
 * 服务类接口
 */
public interface SysMenuService extends IService<SysMenu> {

	List<SysMenuDto> getCurrentUserNav();

	List<SysMenu> tree();

}
