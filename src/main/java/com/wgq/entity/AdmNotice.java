package com.wgq.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AdmNotice extends BaseEntity implements Comparable<AdmNotice>{

    private static final long serialVersionUID = 1L;

    /**
     * 接收人
     */
    private Long receiverId;

    /**
     * 发布本条通知的用户的id 目前是给发布通知业务使用
     */
    private Long publisherId;

    /**
     * 主题是什么
     */
    private String subjectContext;

    @NotBlank(message = "通知内容不能为空")
    private String content;

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

    public AdmNotice(Long receiverId, String subjectContext, String content, LocalDateTime created,int type) {
        this.receiverId = receiverId;
        this.subjectContext = subjectContext;
        this.content = content;
        this.setCreated(created);
        this.type = type;

    }

    @Override
    public int compareTo(AdmNotice o) {
        Long second1 = this.getCreated().toEpochSecond(ZoneOffset.of("+8"));//获取时间戳---秒为单位
        Long second2 = o.getCreated().toEpochSecond(ZoneOffset.of("+8"));
        Long diff0 = (second1 - second2);
        Integer diff = Integer.parseInt(diff0.toString());
        return diff;
    }
}
