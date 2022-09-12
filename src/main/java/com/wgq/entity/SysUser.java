package com.wgq.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysUser extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "用户名昵称不能为空，不能重复")
    private String username;

//    @NotBlank(message = "真实姓名不能为空")
    private String realname;

//    @NotBlank(message = "学工号不能为空")
    private String idNumber;


    private String password;

    private String avatar;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    private String city;

    private LocalDateTime lastLogin;

    private String title;

    @TableField(exist = false)
    private List<SysRole> sysRoles = new ArrayList<>();

    @NotNull(message = "必须声明是否为系统最高管理者isTopManagement")
    private Long isTopManagement;

    private Integer gender;

    private String nation;

    private String idNo;

    private String politicalOutlook;

    private String bankAccount;

    @Pattern(regexp = "^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\\d{8}$", message = "手机号码格式错误")
    private String phone;


}
