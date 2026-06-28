package org.sopt.buddys.global.security.oauth.kakao;

import lombok.extern.slf4j.Slf4j;
import org.sopt.buddys.domain.auth.code.AuthErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.sopt.buddys.global.security.oauth.dto.KakaoTokenResponse;
import org.sopt.buddys.global.security.oauth.dto.KakaoUserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class KakaoAuthClient {

  private final RestTemplate restTemplate;

  @Value("${spring.oauth.kakao.client-id}")
  private String restApiKey;

  @Value("${spring.oauth.kakao.client-secret:}")
  private String clientSecret;

  @Value("${spring.oauth.kakao.redirect-url}")
  private String redirectUrl;

  @Value("${spring.oauth.kakao.token-url}")
  private String tokenUrl;

  @Value("${spring.oauth.kakao.user-info-url}")
  private String userInfoUrl;

  public KakaoAuthClient(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public String getAccessToken(String code) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("grant_type", "authorization_code");
    params.add("client_id", restApiKey);
    params.add("redirect_uri", redirectUrl);
    params.add("code", code);
    if (StringUtils.hasText(clientSecret)) {
      params.add("client_secret", clientSecret);
    }

    try {
      ResponseEntity<KakaoTokenResponse> response = restTemplate.postForEntity(
          tokenUrl,
          new HttpEntity<>(params, headers),
          KakaoTokenResponse.class
      );

      KakaoTokenResponse body = response.getBody();
      if (body == null || !StringUtils.hasText(body.accessToken())) {
        throw new BaseException(AuthErrorCode.KAKAO_AUTH_FAILED, "카카오 토큰 응답이 비어 있습니다.");
      }

      return body.accessToken();
    } catch (RestClientResponseException e) {
      log.warn(
          "Kakao token request failed. status={}, body={}",
          e.getStatusCode(),
          e.getResponseBodyAsString()
      );
      throw new BaseException(AuthErrorCode.KAKAO_AUTH_FAILED, "카카오 토큰 발급에 실패했습니다.");
    }
  }

  public KakaoUserInfo getUserInfo(String accessToken) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);

    try {
      ResponseEntity<KakaoUserInfo> response = restTemplate.exchange(
          userInfoUrl,
          HttpMethod.GET,
          new HttpEntity<>(headers),
          KakaoUserInfo.class
      );

      KakaoUserInfo body = response.getBody();
      if (body == null || body.id() == null) {
        throw new BaseException(AuthErrorCode.KAKAO_AUTH_FAILED, "카카오 사용자 응답이 비어 있습니다.");
      }

      return body;
    } catch (RestClientResponseException e) {
      log.warn(
          "Kakao user info request failed. status={}, body={}",
          e.getStatusCode(),
          e.getResponseBodyAsString()
      );
      throw new BaseException(AuthErrorCode.KAKAO_AUTH_FAILED, "카카오 사용자 조회에 실패했습니다.");
    }
  }
}
