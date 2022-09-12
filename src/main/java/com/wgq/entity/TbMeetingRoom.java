package com.wgq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class TbMeetingRoom {
    @TableId(value = "id",type = IdType.AUTO)
    private Integer id;
    private String name;
    private Short max;
    private String desc;
    private Byte status;
}
