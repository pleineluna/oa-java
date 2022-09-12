package com.wgq.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * <p>
 * 
 * </p>
 *
 * @author 作者:kissshot.wang@foxmail.com
 * @since 2021-12-11
 */
@Data
public class AdmScholarshipSysUser{

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private Long scholarshipId;

    private Long userId;

    /**
     * 创建时间
     */
    private LocalDateTime created;
    /**
     * 更新时间
     */
    private LocalDateTime updated;

    /**
     * 申请描述
     */
    private String applyDesc;

    /**
     * 申请状态
     * "approved"\"refused"\"processing"
     */
    private String status;

    /**
     * activiti中的taskId
     * 虽然每个AdmScholarshipSysUser记录都会有这个字段，但是我感觉没必要持久化到表中。因为taskId会在activii的相关表记录。你也可以用新的DTO对象，这样就不用添加@TableField(exist = false)注解了
     */
    @TableField(exist = false)
    private String taskId;

}
