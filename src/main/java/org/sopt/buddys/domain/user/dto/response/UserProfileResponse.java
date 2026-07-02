package org.sopt.buddys.domain.user.dto.response;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.sopt.buddys.domain.post.entity.Post;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.entity.VerificationBadge;

public record UserProfileResponse(
    Long userId,
    String nickname,
    String profileImageUrl,
    String bio,
    VerificationBadge verificationBadge,
    List<String> representativeTags,
    List<TagGroupResponse> allTags,
    List<PostResponse> posts
) {

  public static UserProfileResponse of(
      User user,
      List<String> representativeTags,
      List<TagGroupResponse> allTags,
      List<PostResponse> posts
  ) {
    return new UserProfileResponse(
        user.getId(),
        user.getNickname(),
        user.getProfileImageUrl(),
        user.getIntroduction(),
        getVerificationBadge(user),
        List.copyOf(representativeTags),
        List.copyOf(allTags),
        List.copyOf(posts)
    );
  }

  private static VerificationBadge getVerificationBadge(User user) {
    if (user.isExchangeVerified()) {
      return VerificationBadge.EXCHANGE_VERIFIED;
    }
    if (user.isUniversityVerified()) {
      return VerificationBadge.UNIVERSITY_VERIFIED;
    }
    return VerificationBadge.SOCIAL_LOGIN;
  }

  public record TagGroupResponse(
      TagType tagType,
      List<String> tags
  ) {

    public TagGroupResponse {
      tags = List.copyOf(tags);
    }
  }

  public record PostResponse(
      Long postId,
      String title,
      String content,
      String thumbnailImageUrl,
      String dateText
  ) {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yy.MM.dd");

    public static PostResponse from(Post post) {
      return from(post, null);
    }

    public static PostResponse from(Post post, String thumbnailImageUrl) {
      return new PostResponse(
          post.getId(),
          post.getTitle(),
          post.getContent(),
          thumbnailImageUrl,
          toDateText(post.getStartDate(), post.getEndDate())
      );
    }

    private static String toDateText(LocalDate startDate, LocalDate endDate) {
      if (startDate == null || endDate == null) {
        return null;
      }
      long days = ChronoUnit.DAYS.between(startDate, endDate);
      return "%s ~ %s (%d일)".formatted(
          startDate.format(DATE_FORMATTER),
          endDate.format(DATE_FORMATTER),
          days
      );
    }
  }
}
