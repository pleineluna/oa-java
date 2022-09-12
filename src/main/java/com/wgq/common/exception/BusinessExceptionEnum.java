package com.wgq.common.exception;

/**
 * Description:
 * 异常枚举类
 */
public enum BusinessExceptionEnum {
    SYSTEM_ERROR(20000, "系统异常"),
    TASK_NOT_FOUND(30000, "未找到待处理任务"),
    CANNOT_DELETE_MEETINGROOM(30001, "无法删除关联会议的会议室"),
    FAILED_TO_ADD_MEETING(30002, "会议添加失败"),
    LESS_THAN_20MIN_CANNOT_DELETE_MEETING(30003, "距离会议开始不足20分钟，不能删除会议"),
    ONLY_SELF_MEETING_CAN_BE_DELETED(30004, "只能删除自己创建的会议"),
    ONLE_MEETING_TO_BE_APPROVED_AND_NOTSTARTED_CAN_BE_DELETE(30005, "只能删除待审批和未开始的会议"),
    SYSROLE_CREATE_ERRPR(30006, "创建角色错误"),
    SYSUSER_CREATE_ERRPR(30007, "创建用户错误"),
    SYSMENU_CREATE_ERRPR(30008, "创建菜单错误"),
    FAILED_TO_DELETE_SYSROLE_TOPMANAGEMENT(30009, "系统最高管理员角色（code=TopManagement）不允许删除！"),
    FAILED_TO_DELETE_SYSUSER_TOPMANAGEMENT(30010, "系统最高管理者用户不允许删除！"),
    FAILED_TO_DISTRIBUTE_SYSUSER_TOPMANAGEMENT_ROLES(30011, "系统最高管理者用户不允许分配权限！"),
    FAILED_TO_DISTRIBUTE_SYSROLE_TOPMANAGEMENT_ROLES(30012, "系统最高管理员角色（code=TopManagement）拥有所有权限！不允许分配权限！"),
    FAILED_TO_ADD_SYSROLE_TOPMANAGEMENT(300013, "系统最高管理员角色（code=TopManagement）不允许创建"),
    FAILED_TO_ADD_SYSUSER_TOPMANAGEMENT(300014, "系统最高管理者用户不允许创建"),
    ONLY_DISTRIBUTE_ONE_ROLE(300015, "只能为用户分配唯一角色"),
    FAILED_TO_CREATE_SCHOLARSHIP_CATEGORY(300016, "新建奖学金类别失败"),
    REPEAT_APPLY_SCHOLARSHIP(300016, "您已申请过此奖学金，请勿重复申请！"),





    ;
    /**
     * 异常码
     */
    Integer code;
    /**
     * 异常信息
     */
    String msg;

    BusinessExceptionEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
