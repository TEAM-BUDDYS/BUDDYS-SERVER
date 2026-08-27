package org.sopt.buddys.domain.verification.repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.verification.entity.UniversityVerification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class RedisUniversityVerificationRepository implements UniversityVerificationRepository {

  private static final String KEY_PREFIX = "verification:university:user:";
  private static final String VALUE_SEPARATOR = ":";
  private static final int VALUE_PARTS = 3;
  private static final Base64.Encoder VALUE_ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder VALUE_DECODER = Base64.getUrlDecoder();
  private static final DefaultRedisScript<Long> DELETE_IF_VALUE_MATCHES = new DefaultRedisScript<>(
      "if redis.call('GET', KEYS[1]) == ARGV[1] "
          + "then return redis.call('DEL', KEYS[1]) else return 0 end",
      Long.class
  );

  private final StringRedisTemplate redisTemplate;

  @Override
  public void save(UniversityVerification verification, Duration ttl) {
    redisTemplate.opsForValue().set(key(verification.userId()), encode(verification), ttl);
  }

  @Override
  public Optional<UniversityVerification> findByToken(String token) {
    Optional<Long> userId = extractUserId(token);
    if (userId.isEmpty()) {
      return Optional.empty();
    }

    String storedValue = redisTemplate.opsForValue().get(key(userId.get()));
    if (storedValue == null) {
      return Optional.empty();
    }

    return decode(userId.get(), token, storedValue);
  }

  @Override
  public void deleteIfTokenMatches(UniversityVerification verification) {
    redisTemplate.execute(
        DELETE_IF_VALUE_MATCHES,
        List.of(key(verification.userId())),
        encode(verification)
    );
  }

  private Optional<UniversityVerification> decode(Long userId, String token, String storedValue) {
    String[] parts = storedValue.split(VALUE_SEPARATOR, VALUE_PARTS);
    if (parts.length != VALUE_PARTS || !tokenMatches(parts[1], token)) {
      return Optional.empty();
    }

    try {
      Long universityId = Long.valueOf(parts[0]);
      String email = new String(VALUE_DECODER.decode(parts[2]), StandardCharsets.UTF_8);
      return Optional.of(new UniversityVerification(userId, universityId, email, token));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  private Optional<Long> extractUserId(String token) {
    if (token == null) {
      return Optional.empty();
    }

    int separatorIndex = token.indexOf('.');
    if (separatorIndex <= 0 || separatorIndex == token.length() - 1) {
      return Optional.empty();
    }

    try {
      return Optional.of(Long.valueOf(token.substring(0, separatorIndex)));
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }

  private String encode(UniversityVerification verification) {
    String encodedEmail = VALUE_ENCODER.encodeToString(
        verification.email().getBytes(StandardCharsets.UTF_8)
    );
    return verification.universityId()
        + VALUE_SEPARATOR + tokenDigest(verification.token())
        + VALUE_SEPARATOR + encodedEmail;
  }

  private boolean tokenMatches(String storedDigest, String token) {
    byte[] stored = storedDigest.getBytes(StandardCharsets.US_ASCII);
    byte[] actual = tokenDigest(token).getBytes(StandardCharsets.US_ASCII);
    return MessageDigest.isEqual(stored, actual);
  }

  private String tokenDigest(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return VALUE_ENCODER.encodeToString(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }

  private String key(Long userId) {
    return KEY_PREFIX + userId;
  }
}
