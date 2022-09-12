package com.wgq.controller.form;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * "查询线上会议成员"
 */
@Data
public class SearchOnlineMeetingMembersForm {
    @NotNull(message = "meetingId不能为空")
    @Min(value = 1, message = "meetingId不能小于1")
    private Integer meetingId;
}
