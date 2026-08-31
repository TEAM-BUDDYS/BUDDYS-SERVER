package org.sopt.buddys.domain.verification.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "verification.university-email")
public record UniversityVerificationProperties(
    @NotNull Duration codeExpiration,
    @Positive int maxAttempts
) {

  public UniversityVerificationProperties {
    if (codeExpiration != null && (codeExpiration.isZero() || codeExpiration.isNegative())) {
      throw new IllegalArgumentException("codeExpiration must be positive");
    }
  }
}
