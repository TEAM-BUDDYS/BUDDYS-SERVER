package org.sopt.buddys.global.aws.s3;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@Getter
public class S3Properties {
  @Value("${cloud.aws.s3.bucket}")
  private String bucket;

  @Value("${cloud.aws.s3.region}")
  private String region;

  @Bean
  public AwsCredentialsProvider awsCredentialsProvider() {
    return DefaultCredentialsProvider.create();
  }

  @Bean
  public S3Presigner s3Presigner(AwsCredentialsProvider awsCredentialsProvider) {
    return S3Presigner.builder()
        .region(Region.of(region))
        .credentialsProvider(awsCredentialsProvider)
        .build();
  }

  @Bean
  public S3Client s3Client(AwsCredentialsProvider awsCredentialsProvider) {
    return S3Client.builder()
        .region(Region.of(region))
        .credentialsProvider(awsCredentialsProvider)
        .build();
  }

  @Bean
  public S3Utilities s3Utilities() {
    return S3Utilities.builder()
        .region(Region.of(region))
        .build();
  }
}
