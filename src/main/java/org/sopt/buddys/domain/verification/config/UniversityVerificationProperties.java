package org.sopt.buddys.domain.verification.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "verification.university-email")
public record UniversityVerificationProperties(
    @NotNull Duration tokenExpiration,
    @NotBlank String confirmUrl
) {

  public UniversityVerificationProperties {
    if (tokenExpiration != null && (tokenExpiration.isZero() || tokenExpiration.isNegative())) {
      throw new IllegalArgumentException("tokenExpiration must be positive");
    }
  }
}
