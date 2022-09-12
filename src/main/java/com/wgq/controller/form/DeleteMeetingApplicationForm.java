package com.wgq.controller.form;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * "删除会议申请表单"
 */
@Data
public class DeleteMeetingApplicationForm {
    @NotNull(message = "id不能为空")
    @Min(value = 1)
    private Long id;

    @NotBlank(message = "uuid不能为空")
    private String uuid;

    @NotBlank(message = "instanceId不能为空")
    private String instanceId;

    @NotBlank(message = "原因不能为空")
    private String reason;

}