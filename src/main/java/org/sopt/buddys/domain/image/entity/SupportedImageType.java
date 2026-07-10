package org.sopt.buddys.domain.image.entity;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import lombok.Getter;

@Getter
public enum SupportedImageType {
  JPEG("image/jpeg", ".jpg"),
  PNG("image/png", ".png"),
  WEBP("image/webp", ".webp");

  private final String contentType;
  private final String extension;

  SupportedImageType(String contentType, String extension) {
    this.contentType = contentType;
    this.extension = extension;
  }

  public static Optional<SupportedImageType> fromContentType(String contentType) {
    String normalized = contentType.toLowerCase(Locale.ROOT);
    return Arrays.stream(values())
        .filter(type -> type.getContentType().equals(normalized))
        .findFirst();
  }
}
