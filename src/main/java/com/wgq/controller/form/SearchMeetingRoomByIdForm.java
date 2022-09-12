package com.wgq.controller.form;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * "根据ID查询会议室表单"
 */
@Data
public class SearchMeetingRoomByIdForm {

    @NotNull(message = "id不能为空")
    @Min(value = 1,message = "id不能小于1")
    private Integer id;
}
