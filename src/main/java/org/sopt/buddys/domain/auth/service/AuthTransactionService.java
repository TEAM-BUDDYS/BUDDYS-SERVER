package org.sopt.buddys.domain.auth.service;

import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.auth.code.AuthErrorCode;
import org.sopt.buddys.domain.auth.dto.response.AuthTokens;
import org.sopt.buddys.domain.auth.entity.RefreshToken;
import org.sopt.buddys.domain.auth.repository.RefreshTokenRepository;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.domain.user.service.UserService;
import org.sopt.buddys.global.exception.BaseException;
import org.sopt.buddys.global.security.jwt.JwtProperties;
import org.sopt.buddys.global.security.jwt.JwtProvider;
import org.sopt.buddys.global.security.oauth.dto.GoogleUserInfo;
import org.sopt.buddys.global.security.oauth.dto.KakaoUserInfo;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthTransactionService {

  private static final String PROVIDER_UNIQUE_CONSTRAINT = "uk_user_provider";
  private static final String NICKNAME_UNIQUE_CONSTRAINT = "uk_user_nickname";

  private final UserRepository userRepository;
  private final SocialUserTransactionService socialUserTransactionService;
  private final UserService userService;
  private final JwtProvider jwtProvider;
  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtProperties jwtProperties;

  @Transactional
  public AuthTokens processKakaoLogin(String providerId, KakaoUserInfo kakaoUser) {
    return processSocialLogin(
        AuthProvider.KAKAO,
        providerId,
        () -> User.ofKakao(providerId, kakaoUser)
    );
  }

  @Transactional
  public AuthTokens processGoogleLogin(String providerId, GoogleUserInfo googleUser) {
    return processSocialLogin(
        AuthProvider.GOOGLE,
        providerId,
        () -> User.ofGoogle(providerId, googleUser)
    );
  }

  private AuthTokens processSocialLogin(
      AuthProvider provider,
      String providerId,
      Supplier<User> newUserSupplier
  ) {
    User user = userRepository.findByProviderAndProviderId(provider, providerId)
        .orElseGet(() -> saveNewUser(provider, providerId, newUserSupplier));

    String jwt = jwtProvider.generateToken(user.getId());
    String refreshToken = jwtProvider.generateRefreshToken(user.getId());

    refreshTokenRepository.deleteByUserId(user.getId());
    refreshTokenRepository.save(
        RefreshToken.of(user.getId(), refreshToken, jwtProperties.refreshTokenExpiration())
    );

    return new AuthTokens(user.getId(), jwt, refreshToken, userService.isOnboardingCompleted(user));
  }

  private User saveNewUser(
      AuthProvider provider,
      String providerId,
      Supplier<User> newUserSupplier
  ) {
    try {
      return socialUserTransactionService.create(newUserSupplier.get());
    } catch (DataIntegrityViolationException e) {
      if (isConstraintViolation(e, PROVIDER_UNIQUE_CONSTRAINT)) {
        return socialUserTransactionService.findByProviderAndProviderId(provider, providerId)
            .orElseThrow(() -> e);
      }
      if (isConstraintViolation(e, NICKNAME_UNIQUE_CONSTRAINT)) {
        throw new BaseException(AuthErrorCode.DUPLICATE_NICKNAME, "이미 사용 중인 닉네임입니다.");
      }
      throw e;
    }
  }

  private boolean isConstraintViolation(
      DataIntegrityViolationException exception,
      String constraintName
  ) {
    Throwable cause = exception;
    while (cause != null) {
      if (cause instanceof org.hibernate.exception.ConstraintViolationException constraintViolationException) {
        String actualConstraintName = constraintViolationException.getConstraintName();
        return actualConstraintName != null
            && (actualConstraintName.equals(constraintName)
                || actualConstraintName.endsWith("." + constraintName));
      }
      cause = cause.getCause();
    }
    return false;
  }
}
