package org.sopt.buddys.global.security.oauth.google;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "spring.oauth.google")
public record GoogleOAuthProperties(
    String clientId,
    String clientSecret,
    @NotEmpty List<@NotBlank String> redirectUrls,
    String tokenUrl,
    String userInfoUrl
) {
  public GoogleOAuthProperties {
    if (redirectUrls != null) {
      redirectUrls = redirectUrls.stream().map(String::trim).toList();
    }
  }
}
