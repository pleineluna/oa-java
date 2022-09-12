package com.wgq.controller.form;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * "查询在线会议室房间ID"
 */
@Data
public class SearchRoomIdByUUIDForm {

    @NotBlank(message = "uuid不能为空")
    private String uuid;
}