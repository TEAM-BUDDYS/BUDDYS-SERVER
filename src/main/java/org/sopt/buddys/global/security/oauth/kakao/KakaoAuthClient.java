package org.sopt.buddys.global.security.oauth.kakao;

import lombok.extern.slf4j.Slf4j;
import org.sopt.buddys.domain.auth.code.AuthErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.sopt.buddys.global.security.oauth.dto.KakaoTokenResponse;
import org.sopt.buddys.global.security.oauth.dto.KakaoUserInfo;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class KakaoAuthClient {

  private final RestTemplate restTemplate;
  private final KakaoOAuthProperties properties;

  public KakaoAuthClient(RestTemplate restTemplate, KakaoOAuthProperties properties) {
    this.restTemplate = restTemplate;
    this.properties = properties;
  }

  public String getAccessToken(String code) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("grant_type", "authorization_code");
    params.add("client_id", properties.clientId());
    params.add("redirect_uri", properties.redirectUrl());
    params.add("code", code);
    if (StringUtils.hasText(properties.clientSecret())) {
      params.add("client_secret", properties.clientSecret());
    }

    try {
      ResponseEntity<KakaoTokenResponse> response = restTemplate.postForEntity(
          properties.tokenUrl(),
          new HttpEntity<>(params, headers),
          KakaoTokenResponse.class
      );

      KakaoTokenResponse body = response.getBody();
      if (body == null || !StringUtils.hasText(body.accessToken())) {
        throw new BaseException(AuthErrorCode.KAKAO_AUTH_FAILED);
      }

      return body.accessToken();
    } catch (RestClientResponseException e) {
      log.warn("Kakao token request failed. status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
      throw new BaseException(AuthErrorCode.KAKAO_AUTH_FAILED);
    } catch (ResourceAccessException e) {
      log.warn("Kakao token request timed out or network error", e);
      throw new BaseException(AuthErrorCode.KAKAO_AUTH_FAILED);
    }
  }

  public KakaoUserInfo getUserInfo(String accessToken) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);

    try {
      ResponseEntity<KakaoUserInfo> response = restTemplate.exchange(
          properties.userInfoUrl(),
          HttpMethod.GET,
          new HttpEntity<>(headers),
          KakaoUserInfo.class
      );

      KakaoUserInfo body = response.getBody();
      if (body == null || body.id() == null) {
        throw new BaseException(AuthErrorCode.KAKAO_AUTH_FAILED);
      }

      return body;
    } catch (RestClientResponseException e) {
      log.warn("Kakao user info request failed. status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
      throw new BaseException(AuthErrorCode.KAKAO_AUTH_FAILED);
    } catch (ResourceAccessException e) {
      log.warn("Kakao user info request timed out or network error", e);
      throw new BaseException(AuthErrorCode.KAKAO_AUTH_FAILED);
    }
  }
}
