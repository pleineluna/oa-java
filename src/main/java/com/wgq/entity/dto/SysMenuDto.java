package com.wgq.entity.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Description:
 * 因为前端接收时某个属性的变量名称和数据字段的名称不一样，所以我们创建dto对象，将属性的名称和前端配合起来。
 * {
 * 					name: 'SysUser',
 * 					title: '用户管理',
 * 					icon: 'el-icon-s-custom',
 * 					path: '/sys/users',
 * 					component: 'sys/User',
 * 					children: []
 *                                },
 */
@Data
public class SysMenuDto implements Serializable {

	private Long id;
	private String name;
	private String title;
	private String icon;
	private String path;
	private String component;
	private List<SysMenuDto> children = new ArrayList<>();

}
