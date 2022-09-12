package com.wgq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * tb_meeting
 *
 * @author
 */
@Data
public class TbMeeting implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id",type = IdType.AUTO)
    private Long id;
    private String uuid;
    private String title;
    private Integer creatorId;
    private String date;
    private String place;
    private String start;
    private String end;
    @TableField("`type`")
    private Short type;
    private Object members;
    private String desc;
    private String instanceId;
    private String present;
    private String unpresent;
    private Short status;
    private Date createTime;
}