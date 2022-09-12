package com.wgq.common.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * Description:
 * 验证码异常类 CaptchaException ; extends AuthenticationException(异常的父类)
 */
public class CaptchaException extends AuthenticationException {

	public CaptchaException(String msg) {
		super(msg);
	}
}
