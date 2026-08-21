package org.sopt.buddys.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.auth.code.AuthErrorCode;
import org.sopt.buddys.domain.auth.dto.response.AuthTokens;
import org.sopt.buddys.domain.auth.entity.RefreshToken;
import org.sopt.buddys.domain.auth.repository.RefreshTokenRepository;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.domain.user.service.UserService;
import org.sopt.buddys.global.exception.BaseException;
import org.sopt.buddys.global.security.jwt.JwtProperties;
import org.sopt.buddys.global.security.jwt.JwtProvider;
import org.sopt.buddys.global.security.oauth.dto.GoogleUserInfo;
import org.sopt.buddys.global.security.oauth.dto.KakaoUserInfo;
import org.sopt.buddys.global.security.oauth.google.GoogleAuthClient;
import org.sopt.buddys.global.security.oauth.kakao.KakaoAuthClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final KakaoAuthClient kakaoAuthClient;
  private final GoogleAuthClient googleAuthClient;
  private final AuthTransactionService authTransactionService;
  private final JwtProvider jwtProvider;
  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtProperties jwtProperties;
  private final UserRepository userRepository;
  private final UserService userService;

  public AuthTokens kakaoLogin(String code, String redirectUri) {
    String accessToken = kakaoAuthClient.getAccessToken(code, redirectUri);
    KakaoUserInfo kakaoUser = kakaoAuthClient.getUserInfo(accessToken);

    String providerId = String.valueOf(kakaoUser.id());
    return authTransactionService.processKakaoLogin(providerId, kakaoUser);
  }

  public AuthTokens googleLogin(String code, String redirectUri) {
    String accessToken = googleAuthClient.getAccessToken(code, redirectUri);
    GoogleUserInfo googleUser = googleAuthClient.getUserInfo(accessToken);

    String providerId = googleUser.sub();
    return authTransactionService.processGoogleLogin(providerId, googleUser);
  }

  @Transactional(noRollbackFor = BaseException.class)
  public AuthTokens reissue(String refreshToken) {
    if (!jwtProvider.validateRefreshToken(refreshToken)) {
      throw new BaseException(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
    }

    RefreshToken stored = refreshTokenRepository.findByTokenForUpdate(refreshToken)
        .orElseThrow(() -> new BaseException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND));

    Long userId = stored.getUserId();

    if (stored.isExpired()) {
      refreshTokenRepository.deleteByUserId(userId);
      throw new BaseException(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
    }

    User user = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));

    String newAccessToken = jwtProvider.generateToken(userId);
    String newRefreshToken = jwtProvider.generateRefreshToken(userId);

    refreshTokenRepository.deleteByUserId(userId);
    refreshTokenRepository.save(RefreshToken.of(userId, newRefreshToken, jwtProperties.refreshTokenExpiration()));

    return new AuthTokens(userId, newAccessToken, newRefreshToken, userService.isOnboardingCompleted(user));
  }
}
