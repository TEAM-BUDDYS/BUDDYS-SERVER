package org.sopt.buddys.domain.verification.entity;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import lombok.Getter;

@Getter
public enum SupportedExchangeDocumentType {

  PDF("application/pdf", ".pdf"),
  JPEG("image/jpeg", ".jpg"),
  PNG("image/png", ".png");

  private final String contentType;
  private final String extension;

  SupportedExchangeDocumentType(String contentType, String extension) {
    this.contentType = contentType;
    this.extension = extension;
  }

  public static Optional<SupportedExchangeDocumentType> fromContentType(String contentType) {
    if (contentType == null) {
      return Optional.empty();
    }

    String normalized = contentType.toLowerCase(Locale.ROOT);
    return Arrays.stream(values())
        .filter(type -> type.contentType.equals(normalized))
        .findFirst();
  }
}
