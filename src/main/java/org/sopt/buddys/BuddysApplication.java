package org.sopt.buddys;

import org.sopt.buddys.global.security.jwt.JwtProperties;
import org.sopt.buddys.global.security.oauth.kakao.KakaoOAuthProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties({JwtProperties.class, KakaoOAuthProperties.class})
@SpringBootApplication
public class BuddysApplication {

	public static void main(String[] args) {
		SpringApplication.run(BuddysApplication.class, args);
	}

}
