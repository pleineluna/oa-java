package com.wgq.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wgq.entity.SysUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Repository
public interface SysUserMapper extends BaseMapper<SysUser> {

	List<Long> getNavMenuIds(Long userId);

	List<SysUser> listByMenuId(Long menuId);

	SysUser getGuideUserByUserId(Long userId);

	SysUser getSecretary();

	IPage<SysUser> selectPageVo(Page<SysUser> page, @Param("username") String username);

	ArrayList<HashMap> searchAllUser();

    HashMap searchName(Long id);


}
