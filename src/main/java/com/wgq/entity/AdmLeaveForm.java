package com.wgq.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdmLeaveForm extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 员工编号
     */
    private Long userId;

    /**
     * 请假类型 1-事假 2-病假 3-工伤假 4-婚假 5-产假 6-丧假
     */
    private Integer formType;

    /**
     *  开始时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     *  截止时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 请假事由
     */
    private String reason;

    /**
     * processing-正在审批 approved-审批已通过 refused-审批被驳回
     */
    private String state;


}
