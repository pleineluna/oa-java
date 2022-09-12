package com.wgq.controller;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.map.MapUtil;
import com.google.code.kaptcha.Producer;
import com.wgq.common.lang.Const;
import com.wgq.common.lang.Result;
import com.wgq.entity.SysUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import sun.misc.BASE64Encoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.Principal;

/**
 * Description:
 * AuthController校验控制器
 * 用于验证码生成、用户信息的获取
 */
@RestController
public class AuthController extends BaseController{

	@Autowired
	Producer producer;

	@GetMapping("/captcha")
	public Result captcha() throws IOException {

		String key = UUID.randomUUID().toString();
		String code = producer.createText();
		// 为了postman的测试（这样能够固定验证码的key和code），要不然key和code没法填
//		key = "aaaaa"; //模拟正确的验证码对应的key
//		code = "11111"; //模拟正确的验证码
		//把图片写进流里面
		BufferedImage image = producer.createImage(code);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ImageIO.write(image, "jpg", outputStream);

		BASE64Encoder encoder = new BASE64Encoder();
		String str = "data:image/jpeg;base64,"; //统一前缀

		String base64Img = str + encoder.encode(outputStream.toByteArray());

		redisUtil.hset(Const.CAPTCHA_KEY, key, code, 120);	//存到redis里面
		return Result.succ(
				MapUtil.builder()
						.put("token", key)
						.put("captchaImg", base64Img)
						.build()
		);
	}

	/**
	 * 获取用户信息接口
	 * @param principal
	 * @return
	 */
	@GetMapping("/sys/userInfo")
	public Result userInfo(Principal principal) {

		SysUser sysUser = sysUserService.getByUsername(principal.getName());

		return Result.succ(MapUtil.builder()
				.put("id", sysUser.getId())
				.put("username", sysUser.getUsername())
				.put("avatar", sysUser.getAvatar())
				.put("created", sysUser.getCreated())
				.put("gender", sysUser.getGender())
				.put("city", sysUser.getCity())
				.map()
		);
	}


}
