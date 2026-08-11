package org.sopt.buddys.global.security.oauth.kakao;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "spring.oauth.kakao")
public record KakaoOAuthProperties(
    String clientId,
    String clientSecret,
    @NotEmpty List<String> redirectUrls,
    String tokenUrl,
    String userInfoUrl
) {
  public KakaoOAuthProperties {
    if (redirectUrls != null) {
      redirectUrls = redirectUrls.stream().map(String::trim).toList();
    }
  }
}
