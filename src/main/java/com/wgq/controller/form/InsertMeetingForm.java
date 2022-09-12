package com.wgq.controller.form;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * "添加会议表单"
 */
@Data
public class InsertMeetingForm implements Serializable {

    /**
     * 因为这是借鉴版的，所以没有继承BaseEntity，所以我忘记了添加id，所以无法联动到表中。然后导致我的businessKey不正确??
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "title不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9\\u4e00-\\u9fa5]{2,30}$", message = "title内容不正确")
    private String title;


    @NotBlank(message = "date不能为空")
    @Pattern(regexp = "^((((1[6-9]|[2-9]\\d)\\d{2})-(0?[13578]|1[02])-(0?[1-9]|[12]\\d|3[01]))|(((1[6-9]|[2-9]\\d)\\d{2})-(0?[13456789]|1[012])-(0?[1-9]|[12]\\d|30))|(((1[6-9]|[2-9]\\d)\\d{2})-0?2-(0?[1-9]|1\\d|2[0-8]))|(((1[6-9]|[2-9]\\d)(0[48]|[2468][048]|[13579][26])|((16|[2468][048]|[3579][26])00))-0?2-29-))$", message = "date内容不正确")
    private String date;

    @Pattern(regexp = "^[a-zA-Z0-9\\u4e00-\\u9fa5]{2,20}$", message = "place内容不正确")
    private String place;

//    @NotBlank(message = "start不能为空")
//    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):(00|30)$", message = "start内容不正确")
    private String start;

//    @NotBlank(message = "end不能为空")
//    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):(00|30)$", message = "end内容不正确")
    private String end;

    @NotNull(message = "type不能为空")
    @Range(min = 1, max = 2, message = "type内容不正确")
    private Byte type;

    @NotBlank(message = "members不能为空")
    private String members;

    @NotBlank(message = "desc不能为空")
    @Length(min = 1, max = 200)
    private String desc;
}
