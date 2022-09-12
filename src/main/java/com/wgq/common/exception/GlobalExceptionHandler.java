package com.wgq.common.exception;

import com.wgq.common.lang.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Description:
 * GlobalExceptionHandler全局异常处理器
 * 目的：将异常封装成Result对象返回给前端，使异常信息人性化。
 * !!!注意，有利就有弊，这样做的弊端是后端开发调试bug信息不会堆栈显示，只会按照你自己设置的显示，所以强烈建议开发时关闭全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	 //实体校验异常捕获
	@ResponseStatus(HttpStatus.BAD_REQUEST) //HttpStatus.BAD_REQUEST的常量
	@ExceptionHandler(value = MethodArgumentNotValidException.class)
	public Result handler(MethodArgumentNotValidException e) {
		//BindingResult用在实体类校验信息返回结果绑定。是package org.springframework.validation;的类
		BindingResult result = e.getBindingResult();
		//拿到第一个异常
		ObjectError objectError = result.getAllErrors().stream().findFirst().get();
		log.error("实体校验validation异常：----------------{}", objectError.getDefaultMessage());
		return Result.fail(objectError.getDefaultMessage());
	}

	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(value = IllegalArgumentException.class)
	public Result handler(IllegalArgumentException e) {
		log.error("IllegalArgumentException不合法的参数异常：----------------{}", e.getMessage());
		return Result.fail(e.getMessage());
	}


	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(value = RuntimeException.class)
	public Result handler(RuntimeException e) {
		log.error("运行时异常：----------------{}", e.getMessage());
		return Result.fail(e.getMessage());
	}


}
