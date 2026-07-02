package org.sopt.buddys.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

  private final JwtProperties jwtProperties;
  private final SecretKey signingKey;
  private static final String TOKEN_TYPE_CLAIM = "type";
  private static final String ACCESS_TOKEN_TYPE = "access";
  private static final String REFRESH_TOKEN_TYPE = "refresh";

  public JwtProvider(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
    this.signingKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
  }

  public String generateToken(Long userId) {
    return buildToken(userId, ACCESS_TOKEN_TYPE, jwtProperties.accessTokenExpiration());
  }

  public String generateRefreshToken(Long userId) {
    return buildToken(userId, REFRESH_TOKEN_TYPE, jwtProperties.refreshTokenExpiration());
  }

  private String buildToken(Long userId, String type, long expiration) {
    if (userId == null) throw new IllegalArgumentException("userId must not be null");
    return Jwts.builder()
        .subject(userId.toString())
        .claim(TOKEN_TYPE_CLAIM, type)
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(signingKey)
        .compact();
  }

  public Long getUserId(String token) {
    return Long.parseLong(getClaims(token).getSubject());
  }

  public Optional<Long> extractUserId(String token) {
    try {
      Claims claims = getClaims(token);
      if (!ACCESS_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
        return Optional.empty();
      }
      return Optional.of(Long.parseLong(claims.getSubject()));
    } catch (JwtException | IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  public boolean validateRefreshToken(String token) {
    try {
      Claims claims = getClaims(token);
      return REFRESH_TOKEN_TYPE.equals(claims.get(TOKEN_TYPE_CLAIM, String.class));
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  public boolean validateToken(String token) {
    try {
      getClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  private Claims getClaims(String token) {
    return Jwts.parser()
        .verifyWith(signingKey)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

}
