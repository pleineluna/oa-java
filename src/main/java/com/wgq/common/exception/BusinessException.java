package com.wgq.common.exception;

/**
 * Description:
 * 所有的业务逻辑异常的类
 */
public class BusinessException extends RuntimeException {
    private String code; //异常编码 表示什么异常
    private String message; //异常的具体文本消息

    public BusinessException(String code, String msg) {
        super(code + ":" + msg);
        this.code = code;
        this.message = msg;
    }

    /**
     * 统一异常联合统一异常枚举使用
     * @param exceptionEnum
     */
    public BusinessException(BusinessExceptionEnum exceptionEnum) {
        this(exceptionEnum.getCode().toString(), exceptionEnum.getMsg());
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
