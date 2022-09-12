package com.wgq.entity.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * Description:
 * 修改个人信息时用来接收PersonalInfo.vue模块的数据
 */
@Data
public class ChangePersonalInfoDto {
    @NotBlank(message = "用户名昵称不能为空，不能重复")
    private String username;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    private String bankAccount;

    @Pattern(regexp = "^(13[0-9]|14[01456879]|15[0-35-9]|16[2567]|17[0-8]|18[0-9]|19[0-35-9])\\d{8}$", message = "手机号码格式错误")
    private String phone;
}
