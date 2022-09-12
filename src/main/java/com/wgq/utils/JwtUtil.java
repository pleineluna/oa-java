package com.wgq.utils;

import io.jsonwebtoken.*;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 *描述：JwtUtils
 * Jwt token操作工具类
 */
@Data
@Component
//springboot使用@ConfigurationProperties(prefix="")加载yml或prototype里的配置信息，需要对应的set get方法。
@ConfigurationProperties(prefix = "markerhub.jwt")
public class JwtUtil {

	private long expire;
	private String secret;
	private String header;

	// 生成jwt
	public String generateToken(String username) {

		Date nowDate = new Date();
		Date expireDate = new Date(nowDate.getTime() + 1000 * expire);

		return Jwts.builder()
				.setHeaderParam("typ", "JWT") //声明类型是jwt
				.setSubject(username)
				.setIssuedAt(nowDate)
				.setExpiration(expireDate)
				.signWith(SignatureAlgorithm.HS512, secret)
				.compact();
	}

	// 解析jwt
	public Claims getClaimByToken(String jwt) {
		try {
			return Jwts.parser()
					.setSigningKey(secret)
					.parseClaimsJws(jwt)
					.getBody();
		} catch (Exception e) {
			return null;
		}
	}

	// jwt是否过期
	public boolean isTokenExpired(Claims claims) {
		//最终return布尔值。然后其他地方进行校验是否过期决定是否抛出相关异常给前端
		return claims.getExpiration().before(new Date());
	}

}
