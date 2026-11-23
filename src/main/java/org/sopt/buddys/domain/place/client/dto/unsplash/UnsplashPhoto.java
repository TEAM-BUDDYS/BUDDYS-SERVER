package org.sopt.buddys.domain.place.client.dto.unsplash;

public record UnsplashPhoto(
    UnsplashUrls urls,
    UnsplashUser user
) {

  public record UnsplashUrls(String raw) {
  }

  public record UnsplashUser(String name, UnsplashUserLinks links) {
  }

  public record UnsplashUserLinks(String html) {
  }
}
