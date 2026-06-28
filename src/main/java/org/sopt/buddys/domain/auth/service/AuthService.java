package org.sopt.buddys.domain.auth.service;

import jakarta.transaction.Transactional;
import org.sopt.buddys.domain.auth.dto.response.LoginResponse;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.global.security.jwt.JwtProvider;
import org.sopt.buddys.global.security.oauth.dto.KakaoUserInfo;
import org.sopt.buddys.global.security.oauth.kakao.KakaoAuthClient;
import org.springframework.stereotype.Service;

@Service
@Transactional
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

    User user = userRepository.findByProviderAndProviderId(AuthProvider.KAKAO, providerId)
        .orElseGet(() -> userRepository.save(User.ofKakao(providerId, kakaoUser)));

    String jwt = jwtProvider.generateToken(user.getId());
    return new LoginResponse(jwt);
  }

}
