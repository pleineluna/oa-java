package com.wgq.security;

import cn.hutool.core.util.StrUtil;
import com.wgq.entity.SysUser;
import com.wgq.service.SysUserService;
import com.wgq.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Description:
 * JwtAuthenticationFilter; extends BasicAuthenticationFilter
 * 用于实现其他接口【非登录请求、非白名单请求】的自动登录的过滤器
 */
public class JwtAuthenticationFilter extends BasicAuthenticationFilter {

	@Autowired
    JwtUtil jwtUtil;

	@Autowired
	UserDetailServiceImpl userDetailService;

	@Autowired
	SysUserService sysUserService;

	public JwtAuthenticationFilter(AuthenticationManager authenticationManager) {
		super(authenticationManager);
	}

	/*重写过滤流程  没有匹配路径条件也就是对所有非登录非白名单请求进行过滤【看流程图】*/
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {

		String jwt = request.getHeader(jwtUtil.getHeader()); //jwtUtils类header属性的get方法！得到header:Authorization(配合注解@ConfigurationProperties(prefix = "markerhub.jwt"))
		if (StrUtil.isBlankOrUndefined(jwt)) {
		 /*为什么没有jwt还放行？意思是：不带jwt token就不走此JwtAuthenticationFilter过滤器验证，相当于没这个过滤器，所以就靠security的一系列过滤器了，
		 看看是不是在securityConfig的白名单里，如果不是白名单中的url，security框架就会抛出异常被拦截处理去登录页
		 ==============综上所述，作用是避免误杀白名单【登录页面也是包括在白名单的】。
		 */
			chain.doFilter(request, response);
			return;
		}
		//获取claim
		Claims claim = jwtUtil.getClaimByToken(jwt);
		if (claim == null) {
			throw new JwtException("token异常");    //JwtException 这个异常是jwt提供的。
		}
		if (jwtUtil.isTokenExpired(claim)) {
			throw new JwtException("token已过期");
		}
		//获取claim的主题，也就是用户名
		String username = claim.getSubject();
		// 获取用户的权限等信息
		SysUser sysUser = sysUserService.getByUsername(username);
		 /*生成认证对象，因为要实现接口的自动登录，所以不需要放入密码？？？ 这里好像因为不是登录，登录前面已经完成了，所以用不到密码。只需要在securityContext里放个认证就行了，
		 也就是UsernamePasswordAuthenticationToken，里面放用户名做标识和权限鉴权用就可以了。*/
		UsernamePasswordAuthenticationToken token
				= new UsernamePasswordAuthenticationToken(
						username,
				null,
						userDetailService.getUserAuthority(sysUser.getId()));

		//设置SecurityContextHolder的Authentication认证主体，框架会自动验证SecurityContext有没有Authentication，有的话说明已经认证了，就会放行。
		SecurityContextHolder.getContext().setAuthentication(token);

		chain.doFilter(request, response);
	}
}
