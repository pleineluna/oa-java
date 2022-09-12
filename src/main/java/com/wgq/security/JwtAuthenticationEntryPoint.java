package com.wgq.security;

import cn.hutool.json.JSONUtil;
import com.wgq.common.lang.Result;
import com.wgq.utils.JwtUtil;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Description:
 * AuthenticationEntryPoint 该类用来统一处理 AuthenticationException（认证失败） 异常
 * 自定义认证失败处理程序  为了使其返回统一响应api
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
	@Resource
	JwtUtil jwtUtil;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {

		response.setContentType("application/json;charset=UTF-8");
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		ServletOutputStream outputStream = response.getOutputStream();
		Result result = Result.fail("请先登录！");

		outputStream.write(JSONUtil.toJsonStr(result).getBytes("UTF-8"));

		outputStream.flush();
		outputStream.close();
	}
}
