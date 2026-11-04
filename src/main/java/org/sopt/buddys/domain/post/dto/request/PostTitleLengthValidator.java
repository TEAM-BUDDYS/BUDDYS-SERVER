package org.sopt.buddys.domain.post.dto.request;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.sopt.buddys.domain.post.entity.Post;

public class PostTitleLengthValidator implements ConstraintValidator<PostTitleLength, String> {

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return value == null || !Post.isTitleTooLong(value);
  }
}
