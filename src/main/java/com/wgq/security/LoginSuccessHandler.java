package com.wgq.security;

import cn.hutool.json.JSONUtil;
import com.wgq.common.lang.Result;
import com.wgq.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Description:
 * LoginSuccessHandler; implements AuthenticationSuccessHandler
 * 登录成功处理器【只针对登录请求】
 */
@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

	@Autowired
    JwtUtil jwtUtil;

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
		response.setContentType("application/json;charset=UTF-8");
		ServletOutputStream outputStream = response.getOutputStream();

		// 生成jwt
		String jwt = jwtUtil.generateToken(authentication.getName());
		//（后端是）把生成的 jwt 放到响应头里然后返回给前端， 把jwt放到所有请求头的过程是在前端完成的。
		response.setHeader(jwtUtil.getHeader(), jwt);

		Result result = Result.succ("");

		outputStream.write(JSONUtil.toJsonStr(result).getBytes("UTF-8"));

		outputStream.flush();
		outputStream.close();
	}

}
