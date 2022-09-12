package com.wgq.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Description:
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdmNoticeDto {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private LocalDateTime updated;

    private Integer statu;
    /**
     * 接收人
     */
    private Long receiverId;

    private String realname;

    @NotBlank(message = "通知内容不能为空")
    private String content;

    private String created;


    /**
     * 发布本条通知的用户的id 目前是给发布通知业务使用
     */
    private Long publisherId;

    /**
     * 主题是什么
     */
    private String subjectContext;

    /**
     * 附件的地址
     */
    private String fileAddress;

    @TableField(exist = false)
    private List files;

    /**
     * 通知类型：
     * type=1 ---请假模块通知【面向具体用户】
     * type=2 ---通知模块导员通知【面向班级】
     * type=3 ---通知模块书记通知【面向全院】
     * type=4 ---会议模块通知
     */
    private int type;

    /**
     * uuid作为本条通知唯一标识
     */
    private String uuid;
}
