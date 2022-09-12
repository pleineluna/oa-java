package com.wgq.controller.form;

import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * "添加会议室表单"
 */
@Data
public class InsertMeetingRoomForm {

    @NotBlank(message = "name不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9\\u4e00-\\u9fa5]{2,20}$",message = "name内容不正确")
    private String name;

    @NotNull(message = "max不能为空")
    @Range(min = 1, max = 99999,message = "max必须在1~99999之间")
    private String max;

    @Length(max = 20,message = "desc不能超过20个字符")
    private String desc;

    private Boolean status;
}