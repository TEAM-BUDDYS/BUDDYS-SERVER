package org.sopt.buddys.global.security.oauth.google;

import lombok.extern.slf4j.Slf4j;
import org.sopt.buddys.domain.auth.code.AuthErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.sopt.buddys.global.security.oauth.dto.GoogleTokenResponse;
import org.sopt.buddys.global.security.oauth.dto.GoogleUserInfo;
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
public class GoogleAuthClient {

  private final RestTemplate restTemplate;
  private final GoogleOAuthProperties properties;

  public GoogleAuthClient(RestTemplate restTemplate, GoogleOAuthProperties properties) {
    this.restTemplate = restTemplate;
    this.properties = properties;
  }

  public String getAccessToken(String code, String redirectUri) {
    if (!properties.redirectUrls().contains(redirectUri)) {
      log.warn("Rejected google login: redirect_uri not in whitelist. redirectUri={}", redirectUri);
      throw new BaseException(AuthErrorCode.GOOGLE_REDIRECT_URI_NOT_ALLOWED);
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("grant_type", "authorization_code");
    params.add("client_id", properties.clientId());
    params.add("redirect_uri", redirectUri);
    params.add("code", code);
    if (StringUtils.hasText(properties.clientSecret())) {
      params.add("client_secret", properties.clientSecret());
    }

    try {
      ResponseEntity<GoogleTokenResponse> response = restTemplate.postForEntity(
          properties.tokenUrl(),
          new HttpEntity<>(params, headers),
          GoogleTokenResponse.class
      );

      GoogleTokenResponse body = response.getBody();
      if (body == null || !StringUtils.hasText(body.accessToken())) {
        throw new BaseException(AuthErrorCode.GOOGLE_AUTH_FAILED);
      }

      return body.accessToken();
    } catch (RestClientResponseException e) {
      log.warn("Google token request failed. status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
      throw new BaseException(AuthErrorCode.GOOGLE_AUTH_FAILED);
    } catch (ResourceAccessException e) {
      log.warn("Google token request timed out or network error", e);
      throw new BaseException(AuthErrorCode.GOOGLE_AUTH_FAILED);
    }
  }

  public GoogleUserInfo getUserInfo(String accessToken) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);

    try {
      ResponseEntity<GoogleUserInfo> response = restTemplate.exchange(
          properties.userInfoUrl(),
          HttpMethod.GET,
          new HttpEntity<>(headers),
          GoogleUserInfo.class
      );

      GoogleUserInfo body = response.getBody();
      if (body == null || !StringUtils.hasText(body.sub())) {
        throw new BaseException(AuthErrorCode.GOOGLE_AUTH_FAILED);
      }

      return body;
    } catch (RestClientResponseException e) {
      log.warn("Google user info request failed. status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
      throw new BaseException(AuthErrorCode.GOOGLE_AUTH_FAILED);
    } catch (ResourceAccessException e) {
      log.warn("Google user info request timed out or network error", e);
      throw new BaseException(AuthErrorCode.GOOGLE_AUTH_FAILED);
    }
  }
}
