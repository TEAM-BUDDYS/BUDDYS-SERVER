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
  private static final String ATTEMPTS_KEY_SUFFIX = ":attempts";
  private static final String RESULT_MATCHED_PREFIX = "MATCHED:";
  private static final String RESULT_ATTEMPT_LIMIT_EXCEEDED = "ATTEMPT_LIMIT_EXCEEDED";
  private static final String RESULT_INVALID = "INVALID";
  private static final String VALUE_SEPARATOR = ":";
  private static final int VALUE_PARTS = 3;
  private static final Base64.Encoder VALUE_ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder VALUE_DECODER = Base64.getUrlDecoder();
  private static final DefaultRedisScript<Long> SAVE_WITH_TTL = new DefaultRedisScript<>(
          "redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2]); "
                  + "redis.call('DEL', KEYS[2]); return 1",
          Long.class
  );
  private static final DefaultRedisScript<String> VERIFY_CODE = new DefaultRedisScript<>(
          "local value = redis.call('GET', KEYS[1]); "
                  + "if not value then "
                  + "local attempts = tonumber(redis.call('GET', KEYS[2]) or '0'); "
                  + "if attempts >= tonumber(ARGV[2]) then return 'ATTEMPT_LIMIT_EXCEEDED' end; "
                  + "return 'INVALID' end; "
                  + "local first = string.find(value, ':', 1, true); "
                  + "local second = first and string.find(value, ':', first + 1, true); "
                  + "if not second then redis.call('DEL', KEYS[1], KEYS[2]); return 'INVALID' end; "
                  + "local storedDigest = string.sub(value, first + 1, second - 1); "
                  + "if storedDigest == ARGV[1] then return 'MATCHED:' .. value end; "
                  + "local attempts = redis.call('INCR', KEYS[2]); "
                  + "if attempts == 1 then "
                  + "local ttl = redis.call('PTTL', KEYS[1]); "
                  + "if ttl > 0 then redis.call('PEXPIRE', KEYS[2], ttl) end end; "
                  + "if attempts >= tonumber(ARGV[2]) then "
                  + "redis.call('DEL', KEYS[1]); return 'ATTEMPT_LIMIT_EXCEEDED' end; "
                  + "return 'INVALID'",
          String.class
  );
  private static final DefaultRedisScript<Long> DELETE_IF_VALUE_MATCHES = new DefaultRedisScript<>(
          "if redis.call('GET', KEYS[1]) == ARGV[1] "
                  + "then return redis.call('DEL', KEYS[1], KEYS[2]) else return 0 end",
          Long.class
  );

  private final StringRedisTemplate redisTemplate;

  @Override
  public void save(UniversityVerification verification, Duration ttl) {
    redisTemplate.execute(
            SAVE_WITH_TTL,
            List.of(key(verification.userId()), attemptsKey(verification.userId())),
            encode(verification),
            String.valueOf(ttl.toMillis())
    );
  }

  @Override
  public VerificationResult verifyCode(Long userId, String code, int maxAttempts) {
    if (userId == null || code == null || maxAttempts < 1) {
      return VerificationResult.invalid();
    }

    String result = redisTemplate.execute(
            VERIFY_CODE,
            List.of(key(userId), attemptsKey(userId)),
            codeDigest(code),
            String.valueOf(maxAttempts)
    );
    if (RESULT_ATTEMPT_LIMIT_EXCEEDED.equals(result)) {
      return VerificationResult.limitExceeded();
    }
    if (result == null || RESULT_INVALID.equals(result) || !result.startsWith(RESULT_MATCHED_PREFIX)) {
      return VerificationResult.invalid();
    }

    String storedValue = result.substring(RESULT_MATCHED_PREFIX.length());
    return decode(userId, code, storedValue)
            .map(VerificationResult::matched)
            .orElseGet(VerificationResult::invalid);
  }

  @Override
  public void deleteIfMatches(UniversityVerification verification) {
    redisTemplate.execute(
            DELETE_IF_VALUE_MATCHES,
            List.of(key(verification.userId()), attemptsKey(verification.userId())),
            encode(verification)
    );
  }

  private Optional<UniversityVerification> decode(Long userId, String code, String storedValue) {
    String[] parts = storedValue.split(VALUE_SEPARATOR, VALUE_PARTS);
    if (parts.length != VALUE_PARTS || !codeMatches(parts[1], code)) {
      return Optional.empty();
    }

    try {
      Long universityId = Long.valueOf(parts[0]);
      String email = new String(VALUE_DECODER.decode(parts[2]), StandardCharsets.UTF_8);
      return Optional.of(new UniversityVerification(userId, universityId, email, code));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  private String encode(UniversityVerification verification) {
    String encodedEmail = VALUE_ENCODER.encodeToString(
            verification.email().getBytes(StandardCharsets.UTF_8)
    );
    return verification.universityId()
            + VALUE_SEPARATOR + codeDigest(verification.code())
            + VALUE_SEPARATOR + encodedEmail;
  }

  private boolean codeMatches(String storedDigest, String code) {
    byte[] stored = storedDigest.getBytes(StandardCharsets.US_ASCII);
    byte[] actual = codeDigest(code).getBytes(StandardCharsets.US_ASCII);
    return MessageDigest.isEqual(stored, actual);
  }

  private String codeDigest(String code) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return VALUE_ENCODER.encodeToString(digest.digest(code.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }

  private String key(Long userId) {
    return KEY_PREFIX + "{" + userId + "}";
  }

  private String attemptsKey(Long userId) {
    return key(userId) + ATTEMPTS_KEY_SUFFIX;
  }
}
