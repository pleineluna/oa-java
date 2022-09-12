package com.wgq.controller.form;

import lombok.Data;

import javax.validation.constraints.NotEmpty;

/**
 * "删除会议室表单"
 */
@Data
public class DeleteMeetingRoomByIdsForm {

    @NotEmpty(message = "ids不能为空")
    // "会议室ID"
    private Integer[] ids;
}