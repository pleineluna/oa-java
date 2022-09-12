package com.wgq.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

/**
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdmClass extends BaseEntity {

    private static final long serialVersionUID = 1L;
    @NotBlank(message = "班级名不能为空")
    private String className;

    private Long guideId;


}
