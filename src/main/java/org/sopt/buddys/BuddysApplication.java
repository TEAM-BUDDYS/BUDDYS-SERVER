package org.sopt.buddys;

import org.sopt.buddys.domain.verification.config.UniversityVerificationProperties;
import org.sopt.buddys.global.config.RedisConnectionProperties;
import org.sopt.buddys.global.mail.MailProperties;
import org.sopt.buddys.global.security.jwt.JwtProperties;
import org.sopt.buddys.global.security.oauth.google.GoogleOAuthProperties;
import org.sopt.buddys.global.security.oauth.kakao.KakaoOAuthProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties({
    JwtProperties.class, KakaoOAuthProperties.class, GoogleOAuthProperties.class,
    MailProperties.class, UniversityVerificationProperties.class, RedisConnectionProperties.class
})
@SpringBootApplication
public class BuddysApplication {

	public static void main(String[] args) {
		SpringApplication.run(BuddysApplication.class, args);
	}

}
