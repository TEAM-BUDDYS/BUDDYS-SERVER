package org.sopt.buddys.domain.auth.service;

import org.sopt.buddys.domain.auth.code.AuthErrorCode;
import org.sopt.buddys.domain.auth.dto.response.AuthTokens;
import org.sopt.buddys.domain.auth.entity.RefreshToken;
import org.sopt.buddys.domain.auth.repository.RefreshTokenRepository;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.global.exception.BaseException;
import org.sopt.buddys.global.security.jwt.JwtProperties;
import org.sopt.buddys.global.security.jwt.JwtProvider;
import org.sopt.buddys.global.security.oauth.dto.KakaoUserInfo;
import org.sopt.buddys.global.security.oauth.kakao.KakaoAuthClient;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final KakaoAuthClient kakaoAuthClient;
  private final UserRepository userRepository;
  private final JwtProvider jwtProvider;
  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtProperties jwtProperties;

  public AuthService(KakaoAuthClient kakaoAuthClient, UserRepository userRepository,
      JwtProvider jwtProvider, RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties) {
    this.kakaoAuthClient = kakaoAuthClient;
    this.userRepository = userRepository;
    this.jwtProvider = jwtProvider;
    this.refreshTokenRepository = refreshTokenRepository;
    this.jwtProperties = jwtProperties;
  }

  @Transactional
  public AuthTokens kakaoLogin(String code) {
    String accessToken = kakaoAuthClient.getAccessToken(code);
    KakaoUserInfo kakaoUser = kakaoAuthClient.getUserInfo(accessToken);

    String providerId = String.valueOf(kakaoUser.id());
    return processKakaoLogin(providerId, kakaoUser);
  }

  @Transactional
  public AuthTokens processKakaoLogin(String providerId, KakaoUserInfo kakaoUser) {
    User user = userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, providerId)
        .orElseGet(() -> saveNewKakaoUser(providerId, kakaoUser));

    String jwt = jwtProvider.generateToken(user.getId());
    String refreshToken = jwtProvider.generateRefreshToken(user.getId());

    refreshTokenRepository.deleteByUserId(user.getId());
    refreshTokenRepository.save(RefreshToken.of(user.getId(), refreshToken, jwtProperties.refreshTokenExpiration()));

    return new AuthTokens(jwt, refreshToken);
  }

  @Transactional
  public AuthTokens reissue(String refreshToken) {
    if (!jwtProvider.validateToken(refreshToken)) {
      throw new BaseException(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
    }

    RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
        .orElseThrow(() -> new BaseException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND));

    Long userId = stored.getUserId();

    if (stored.isExpired()) {
      refreshTokenRepository.deleteByUserId(userId);
      throw new BaseException(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
    }

    String newAccessToken = jwtProvider.generateToken(userId);
    String newRefreshToken = jwtProvider.generateRefreshToken(userId);

    refreshTokenRepository.deleteByUserId(userId);
    refreshTokenRepository.save(RefreshToken.of(userId, newRefreshToken, jwtProperties.refreshTokenExpiration()));

    return new AuthTokens(newAccessToken, newRefreshToken);
  }

  private User saveNewKakaoUser(String providerId, KakaoUserInfo kakaoUser) {
    try {
      return userRepository.save(User.ofKakao(providerId, kakaoUser));
    } catch (DataIntegrityViolationException e) {
      throw new BaseException(AuthErrorCode.DUPLICATE_NICKNAME, "이미 사용 중인 닉네임입니다.");
    }
  }
}
