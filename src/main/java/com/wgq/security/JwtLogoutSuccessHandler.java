package com.wgq.security;

import cn.hutool.json.JSONUtil;
import com.wgq.common.lang.Result;
import com.wgq.utils.JwtUtil;
import com.wgq.utils.RedisUtil;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Description:
 * JwtLogoutSuccessHandler; implements LogoutSuccessHandler
 * 登出处理器
 */
@Component
public class JwtLogoutSuccessHandler implements LogoutSuccessHandler {

	@Resource
    JwtUtil jwtUtil;

	@Resource
	RedisUtil redisUtil;

	@Override
	public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

		//手动退出，既然点了退出按钮，必然清空SecurityContext里的authentication
		if (authentication != null) {
			new SecurityContextLogoutHandler().logout(request, response, authentication);
		}

		response.setContentType("application/json;charset=UTF-8");
		ServletOutputStream outputStream = response.getOutputStream();

		//设为空字符串（使返回的响应的响应头header: Authorization为空字符串，目的是为了清空浏览器的token）
		response.setHeader(jwtUtil.getHeader(), "");


		Result result = Result.succ("");

		outputStream.write(JSONUtil.toJsonStr(result).getBytes("UTF-8"));

		outputStream.flush();
		outputStream.close();
	}
}
