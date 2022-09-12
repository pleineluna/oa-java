package com.wgq.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.wgq.entity.SysUser;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdmScholarshipSysUserDto {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long scholarshipId;

    /**
     * 本次的申请奖学金类别信息   AdmScholarshipDto是包含申请人部分信息的AdmScholarship。这样是为了更好处理数据展现。
     */
    private AdmScholarshipDto admScholarshipDto;

    private Long userId;

    /**
     * 本次的申请人信息
     */
    private SysUser sysUser;

    /**
     * 申请描述
     */
    private String applyDesc;

    /**
     * 申请结果
     * "approved"\"refused"\"processing"
     */
    private String status;

    /**
     * activiti中的taskId
     * 虽然每个AdmScholarshipSysUser记录都会有这个字段，但是我感觉没必要持久化到表中。因为taskId会在activii的相关表记录。你也可以用新的DTO对象，这样就不用添加@TableField(exist = false)注解了
     */
    private String taskId;

    /**
     * 创建时间
     */
    private LocalDateTime created;
    /**
     * 更新时间
     */
    private LocalDateTime updated;







}
