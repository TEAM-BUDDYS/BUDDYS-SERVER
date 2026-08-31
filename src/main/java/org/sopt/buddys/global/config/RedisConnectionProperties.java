package org.sopt.buddys.global.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "buddys.redis")
public record RedisConnectionProperties(
    @NotNull Mode mode,
    @NotBlank String host,
    @Min(1) @Max(65535) int port,
    String username,
    String password,
    boolean sslEnabled,
    @NotNull Duration connectTimeout,
    @NotNull Duration commandTimeout
) {

  public enum Mode {
    STANDALONE,
    CLUSTER
  }
}
