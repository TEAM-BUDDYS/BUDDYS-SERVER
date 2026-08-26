package org.sopt.buddys.global.aws.ses;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;

@Configuration
public class SesConfig {

  @Value("${cloud.aws.ses.region}")
  private String region;

  @Bean
  public SesV2Client sesV2Client() {
    return SesV2Client.builder()
        .region(Region.of(region))
        .build();
  }
}
