package com.wgq.entity.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Description: tb_meeting
 *
 */
@Data
public class TbMeetingDto implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * activiti中的taskId
     */
    private String taskId;

    /**
     * 本次会议申请人的realname
     */
    private String realname;
    private Long id;
    private String uuid;
    private String title;
    private Integer creatorId;
    private String date;
    private String place;
    private String start;
    private String end;
    private Short type;
    /**
     * 请假类型
     */
    private String meetingType;

    /**
     * 参会人，经过查库处理之后变成String的realname的字符串拼接
     */
    private String members;
    private String desc;
    private String instanceId;
    private String present;
    private String unpresent;
    private Short status;
    /**
     * 请假状态
     */
    private String statusType;

    /**
     * 审批结果
     */
    private String result;
    private String createTime;
}