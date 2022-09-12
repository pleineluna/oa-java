package com.wgq.security;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.wgq.common.exception.CaptchaException;
import com.wgq.common.lang.Const;
import com.wgq.utils.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Description:
 * 验证码过滤器，用来对验证码进行校验
 */
@Component
public class CaptchaFilter extends OncePerRequestFilter {

	@Autowired
	RedisUtil redisUtil;

	@Autowired
	LoginFailureHandler loginFailureHandler;

	@Override
	protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {

		String url = httpServletRequest.getRequestURI();

		if ("/login".equals(url) && httpServletRequest.getMethod().equals("POST")) {

			try{
				// 校验验证码
				validate(httpServletRequest);
			} catch (CaptchaException e) {
				//进入到catch也就是验证码验证失败，所以调用认证失败处理器的认证失败方法。
				// 交给认证失败处理器
				loginFailureHandler.onAuthenticationFailure(httpServletRequest, httpServletResponse, e);
			}
		}
		//没问题就放行过滤链
		filterChain.doFilter(httpServletRequest, httpServletResponse);
	}

	// 校验验证码逻辑
	private void validate(HttpServletRequest httpServletRequest) {
		/** 因为前台的"/login"是被我们用qs.stringify(this.loginForm)转化成Form表单的提交形式了。
		 *  为什么要转化？因为securityConfig中接收"/login"页面的数据就要求是Form表单形式，然后才能进行后续过滤器步骤。 */
		String code = httpServletRequest.getParameter("code"); //用户写在前端表单中验证码输入框输入的值
		String key = httpServletRequest.getParameter("token"); //这个token就是“key”、也就是redis哈希表key中每一项的item。

		if (StringUtils.isBlank(code) || StringUtils.isBlank(key)) {
			throw new CaptchaException("验证码错误");  //抛出的异常会被全局异常处理器GlobalExceptionHandler 捕获并处理
		}

		if (!code.equals(redisUtil.hget(Const.CAPTCHA_KEY, key))) {
			throw new CaptchaException("验证码错误");
		}

		// 一次性使用，防止别人暴力破解验证码
		redisUtil.hdel(Const.CAPTCHA_KEY, key);
	}
}
