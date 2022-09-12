package com.wgq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * BaseEntity，提供了一些基础字段供其他实体继承使用
 */
@Data
public class BaseEntity implements Serializable {

	@TableId(value = "id", type = IdType.AUTO)
	private Long id;

	private LocalDateTime created;
	private LocalDateTime updated;

	private Integer statu;
}
