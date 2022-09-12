package com.wgq.security;

import cn.hutool.json.JSONUtil;
import com.wgq.common.lang.Result;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Description:
 * LoginFailureHandler; implements AuthenticationFailureHandler
 * 登录失败处理器【只针对登录请求】
 */
@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {
	/**
	 * 重写登录失败调用的方法，void类型所以不能返回任何东西包括json。
	 * 所以我们才用流的形式进行传递
	 * @param request
	 * @param response
	 * @param exception
	 * @throws IOException
	 * @throws ServletException
	 */
	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {

		response.setContentType("application/json;charset=UTF-8");//设置响应的格式和编码
		ServletOutputStream outputStream = response.getOutputStream();//用到Web中的流
		Result result = Result.fail("用户名或密码错误");
		//把result统一响应对象转化为json形式然后编程字节码的形式writer到流中
		outputStream.write(JSONUtil.toJsonStr(result).getBytes("UTF-8"));
		//把这个流flush出去，也就是推出去
		outputStream.flush();
		outputStream.close();
	}
}
