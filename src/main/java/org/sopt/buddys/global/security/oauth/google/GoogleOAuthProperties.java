package org.sopt.buddys.global.security.oauth.google;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "spring.oauth.google")
public record GoogleOAuthProperties(
    @NotBlank String clientId,
    @NotBlank String clientSecret,
    @NotEmpty List<@NotBlank String> redirectUrls,
    @NotBlank String tokenUrl,
    @NotBlank String userInfoUrl
) {
  public GoogleOAuthProperties {
    if (redirectUrls != null) {
      redirectUrls = redirectUrls.stream().map(String::trim).toList();
    }
  }
}
