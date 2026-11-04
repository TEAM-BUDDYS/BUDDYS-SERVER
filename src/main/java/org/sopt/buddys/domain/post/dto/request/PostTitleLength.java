package org.sopt.buddys.domain.post.dto.request;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 게시글 제목 길이를 앞뒤 공백 제외, 이모지 1자 기준으로 검사한다. null은 @NotBlank가 담당한다.
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PostTitleLengthValidator.class)
public @interface PostTitleLength {

  String message() default "제목은 최대 40자까지 입력할 수 있습니다";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
