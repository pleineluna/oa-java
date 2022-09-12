package com.wgq.config;

import com.wgq.security.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Description:
 * Spring Security核心配置类 SecurityConfig; extends WebSecurityConfigurerAdapter ;
 */
@Configuration
@EnableWebSecurity//EnableWebSecurity注解有两个作用,1: 加载了WebSecurityConfiguration配置类, 配置安全认证策略。2: 加载了AuthenticationConfiguration, 配置了认证信息。
@EnableGlobalMethodSecurity(prePostEnabled = true)//开启Spring方法级安全，可以使用@PreAuthorize("hasAnyRole('user')")等方法注解
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	LoginFailureHandler loginFailureHandler;

	@Autowired
	LoginSuccessHandler loginSuccessHandler;

	@Autowired
	CaptchaFilter captchaFilter;

	@Autowired
	JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

	@Autowired
	JwtAccessDeniedHandler jwtAccessDeniedHandler;

	@Autowired
	UserDetailServiceImpl userDetailService;

	@Autowired
	JwtLogoutSuccessHandler jwtLogoutSuccessHandler;

	/**
	 * 把我们的JwtAuthenticationFilter设置进去
	 * @return JwtAuthenticationFilter
	 * @throws Exception
	 */
	@Bean
	JwtAuthenticationFilter jwtAuthenticationFilter() throws Exception {
		//JwtAuthenticationFilter是一个过滤器类，通过他的有参构造方法给new的对象注入AuthenticationManager 认证管理器，
		//再把jwtAuthenticationFilter通过@Bean注入到spring工程里面，后面会通过.addFilter(jwtAuthenticationFilter())配置进安全框架中。
		//只要是认证就需要由AuthenticationManager 认证管理器来进行。
		return new JwtAuthenticationFilter(authenticationManager());
	}

	/**
	 * 加密方法
	 * @return
	 */
	@Bean
	BCryptPasswordEncoder bCryptPasswordEncoder() {
		return new BCryptPasswordEncoder();
	}

	/**
	 * 白名单
	 */
	private static final String[] URL_WHITELIST = {

			"/login",
			"/logout",
			"/captcha",
			"/favicon.ico",
			"/test/**",
			"/druid/**"
	};


	protected void configure(HttpSecurity http) throws Exception {
		http.cors().and().csrf().disable()
				// 登录配置
				.formLogin() //表示沿用form表单的提交形式
				.successHandler(loginSuccessHandler) //配置我们的认证成功处理器LoginSuccessHandler
				.failureHandler(loginFailureHandler)//配置我们的认证失败处理器LoginFailureHandler
				.and()
				.logout()
				.logoutSuccessHandler(jwtLogoutSuccessHandler)//配置我们的退出成功处理器JwtLogoutSuccessHandler

				// 禁用session  因为我们是前后端分离的项目，而不是动态Web项目。
				.and()
				.sessionManagement()
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS)

				// 配置拦截规则
				.and()
				.authorizeRequests()
				.antMatchers(URL_WHITELIST).permitAll() //放行白名单
				.anyRequest().authenticated() //剩下的其他任何请求都拦截认证

				// 异常处理器
				.and()
				.exceptionHandling()
				.authenticationEntryPoint(jwtAuthenticationEntryPoint)//配置我们的认证失败处理器JwtAuthenticationEntryPoint
				.accessDeniedHandler(jwtAccessDeniedHandler)//配置我们的权限不足处理器JwtAccessDeniedHandler

				// 配置自定义的过滤器
				.and()
				.addFilter(jwtAuthenticationFilter()) //配置我们的自定义认证过滤器JwtAuthenticationFilter
				//验证码过滤器放到UsernamePasswordAuthenticationFilter过滤器之前
				.addFilterBefore(captchaFilter, UsernamePasswordAuthenticationFilter.class)

		;

	}

	/**
	 * 配置UserDetailService
	 * @param auth
	 * @throws Exception
	 */
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(userDetailService);
	}
}
