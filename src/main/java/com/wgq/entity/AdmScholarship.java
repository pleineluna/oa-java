package com.wgq.entity;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AdmScholarship extends BaseEntity{

    private static final long serialVersionUID = 1L;

    /**
     * 奖学金名称
     */
    private String name;

    /**
     * 评定学年
     */
    private String year;

    /**
     * 评定学期
     */
    private String semester;

    /**
     * 等级名称
     */
    private String level;

    /**
     * 奖金金额
     */
    private Integer amountOfMoney;

    /**
     * 是否固定金额
     */
    private Integer fixedMoney;

    /**
     * 申请开始时间，是该类别奖学金什么时间开放申请，并不是某学生申请这个奖学金的时间
     */
    private LocalDateTime startTime;

    /**
     * 申请结束时间，是该类别奖学金什么时间结束开放申请。
     */
    private LocalDateTime endTime;


}
