package org.sopt.buddys.global.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NullPairValidatorTest {

  @DisplayName("둘 다 null이면 유효한 쌍이다")
  @Test
  void isValidPair_bothNull_returnsTrue() {
    assertThat(NullPairValidator.isValidPair(null, null)).isTrue();
  }

  @DisplayName("둘 다 non-null이면 유효한 쌍이다")
  @Test
  void isValidPair_bothNonNull_returnsTrue() {
    assertThat(NullPairValidator.isValidPair("a", 1)).isTrue();
  }

  @DisplayName("하나만 null이면 유효하지 않은 쌍이다")
  @Test
  void isValidPair_onlyOneNull_returnsFalse() {
    assertThat(NullPairValidator.isValidPair(null, 1)).isFalse();
    assertThat(NullPairValidator.isValidPair("a", null)).isFalse();
  }
}
