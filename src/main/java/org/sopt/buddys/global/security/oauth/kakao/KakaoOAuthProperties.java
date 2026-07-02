package org.sopt.buddys.global.security.oauth.kakao;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.oauth.kakao")
public record KakaoOAuthProperties(
    String clientId,
    String clientSecret,
    String redirectUrl,
    String tokenUrl,
    String userInfoUrl
) {}
