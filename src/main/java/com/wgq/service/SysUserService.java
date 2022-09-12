package com.wgq.service;

import com.wgq.entity.SysUser;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Description:
 * 服务类接口
 */
public interface SysUserService extends IService<SysUser> {

	SysUser getByUsername(String username);

	String getUserAuthorityInfo(Long userId);

	void clearUserAuthorityInfo(String username);

	void clearUserAuthorityInfoByRoleId(Long roleId);

	void clearUserAuthorityInfoByMenuId(Long menuId);

    ArrayList<HashMap> searchAllUser();

    Map getUserClassAndSubjectById(Long id);
}
