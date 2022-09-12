package com.wgq.entity.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * Description:
 * PassDto; implements Serializable
 * 使用举例：public Result updatePass(@Validated @RequestBody PassDto passDto, Principal principal){};
 */
@Data
public class PassDto implements Serializable {

	@NotBlank(message = "新密码不能为空")
	private String password;

	@NotBlank(message = "旧密码不能为空")
	private String currentPass;
}
