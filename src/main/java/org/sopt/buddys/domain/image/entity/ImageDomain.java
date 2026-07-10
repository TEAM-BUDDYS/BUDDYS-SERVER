package org.sopt.buddys.domain.image.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ImageDomain {
  POST("posts"),
  PROFILE("profiles");

  private final String folder;
}