package org.sopt.buddys.domain.auth.service;

import lombok.RequiredArgsConstructor;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthTransactionService {

  private final UserRepository userRepository;
  private final JwtProvider jwtProvider;
  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtProperties jwtProperties;

  @Transactional
  public AuthTokens processKakaoLogin(String providerId, KakaoUserInfo kakaoUser) {
    User user = userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, providerId)
        .orElseGet(() -> saveNewKakaoUser(providerId, kakaoUser));

    String jwt = jwtProvider.generateToken(user.getId());
    String refreshToken = jwtProvider.generateRefreshToken(user.getId());

    refreshTokenRepository.deleteByUserId(user.getId());
    refreshTokenRepository.save(
        RefreshToken.of(user.getId(), refreshToken, jwtProperties.refreshTokenExpiration())
    );

    return new AuthTokens(jwt, refreshToken);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void deleteRefreshToken(Long userId) {
    refreshTokenRepository.deleteByUserId(userId);
  }

  private User saveNewKakaoUser(String providerId, KakaoUserInfo kakaoUser) {
    try {
      return userRepository.save(User.ofKakao(providerId, kakaoUser));
    } catch (DataIntegrityViolationException e) {
      throw new BaseException(AuthErrorCode.DUPLICATE_NICKNAME, "이미 사용 중인 닉네임입니다.");
    }
  }
}
