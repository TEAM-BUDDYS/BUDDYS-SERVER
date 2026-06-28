package org.sopt.buddys.domain.auth.service;

import org.sopt.buddys.domain.auth.code.AuthErrorCode;
import org.sopt.buddys.domain.auth.dto.response.LoginResponse;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.global.exception.BaseException;
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

  public AuthService(KakaoAuthClient kakaoAuthClient, UserRepository userRepository, JwtProvider jwtProvider) {
    this.kakaoAuthClient = kakaoAuthClient;
    this.userRepository = userRepository;
    this.jwtProvider = jwtProvider;
  }

  public LoginResponse kakaoLogin(String code) {
    String accessToken = kakaoAuthClient.getAccessToken(code);
    KakaoUserInfo kakaoUser = kakaoAuthClient.getUserInfo(accessToken);

    String providerId = String.valueOf(kakaoUser.id());
    User user = findOrCreateKakaoUser(providerId, kakaoUser);

    String jwt = jwtProvider.generateToken(user.getId());
    return new LoginResponse(jwt);
  }

  @Transactional
  protected User findOrCreateKakaoUser(String providerId, KakaoUserInfo kakaoUser) {
    return userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, providerId)
        .orElseGet(() -> saveNewKakaoUser(providerId, kakaoUser));
  }

  private User saveNewKakaoUser(String providerId, KakaoUserInfo kakaoUser) {
    try {
      return userRepository.save(User.ofKakao(providerId, kakaoUser));
    } catch (DataIntegrityViolationException e) {
      throw new BaseException(AuthErrorCode.DUPLICATE_NICKNAME, "이미 사용 중인 닉네임입니다.");
    }
  }
}
