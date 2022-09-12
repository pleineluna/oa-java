package com.wgq.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserAdmClass extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Integer userId;

    private Integer classId;


}
