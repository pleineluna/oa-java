package com.wgq.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 
 * </p>
 *
 * @author 作者:kissshot.wang@foxmail.com
 * @since 2021-12-10
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdmSpecializedSubject extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 专业名称
     */
    private String name;


}
