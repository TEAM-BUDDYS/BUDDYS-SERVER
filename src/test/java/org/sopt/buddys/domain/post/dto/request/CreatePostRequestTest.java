package org.sopt.buddys.domain.post.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CreatePostRequestTest {

  private static ValidatorFactory validatorFactory;
  private static Validator validator;

  @BeforeAll
  static void setUp() {
    validatorFactory = Validation.buildDefaultValidatorFactory();
    validator = validatorFactory.getValidator();
  }

  @AfterAll
  static void tearDown() {
    validatorFactory.close();
  }

  @DisplayName("게시글 제목은 40자까지 허용된다")
  @Test
  void title_40Characters_isValid() {
    assertThat(validator.validateValue(CreatePostRequest.class, "title", "가".repeat(40))).isEmpty();
  }

  @DisplayName("게시글 제목이 40자를 넘으면 검증에 실패한다")
  @Test
  void title_41Characters_isInvalid() {
    assertThat(validator.validateValue(CreatePostRequest.class, "title", "가".repeat(41))).isNotEmpty();
  }

  @DisplayName("게시글 제목 길이는 앞뒤 공백을 제외하고 센다")
  @Test
  void title_surroundingSpaces_areNotCounted() {
    assertThat(validator.validateValue(CreatePostRequest.class, "title", "  " + "가".repeat(40) + "  ")).isEmpty();
  }

  @DisplayName("이모지는 1자로 세어 40개까지 허용한다")
  @Test
  void title_emojiCountedAsOneCharacter() {
    assertThat(validator.validateValue(CreatePostRequest.class, "title", "😀".repeat(40))).isEmpty();
    assertThat(validator.validateValue(CreatePostRequest.class, "title", "😀".repeat(41))).isNotEmpty();
  }
}
