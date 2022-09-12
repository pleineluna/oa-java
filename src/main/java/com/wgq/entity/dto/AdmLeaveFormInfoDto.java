package com.wgq.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Description:
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdmLeaveFormInfoDto {

//    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * activiti中的taskId
     */
    private String taskId;

    private String className;

    private String created;

    private String position;

    /**
     * 申请人姓名
     */
    private String realname;

    private String updated;

    private Integer statu;

    /**
     * 员工编号
     */
    private Long userId;

    /**
     * 请假类型 1-事假 2-病假 3-工伤假 4-婚假 5-产假 6-丧假
     */
    private String formType;

    /**
     *  开始时间
     */
    private String startTime;

    /**
     *  截止时间
     */
    private String endTime;

    /**
     * 请假事由
     */
    private String reason;

    /**
     * processing-正在审批 approved-审批已通过 refused-审批被驳回
     */
    private String state;

    /**
     * 审批过程中导员的批注
     */
    private String commentOfGuider;

    /**
     * 审批过程中书记的批注
     */
    private String commentOfSecretary;

    /**
     * 身份编码
     * 1：学生
     * 2：导员
     * 3：书记
     */
    private Integer identity;

    /**
     * 书记是否审批
     */
    private String whetherAuditSecretary = "no";
}
