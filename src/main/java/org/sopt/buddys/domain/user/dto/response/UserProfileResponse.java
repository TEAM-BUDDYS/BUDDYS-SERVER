package org.sopt.buddys.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.sopt.buddys.domain.post.entity.Post;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.entity.VerificationBadge;

public record UserProfileResponse(
    @Schema(description = "사용자 ID", example = "1")
    Long userId,

    @Schema(description = "닉네임", example = "버디")
    String nickname,

    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.png")
    String profileImageUrl,

    @Schema(description = "자기소개", example = "같이 여행해요!")
    String bio,

    @Schema(description = "프로필에 표시할 인증 뱃지", example = "SOCIAL_LOGIN")
    VerificationBadge verificationBadge,

    @Schema(description = "대표 취향 태그", example = "[\"문화생활\", \"액티비티\", \"활발한\"]")
    List<String> representativeTags,

    @Schema(description = "전체 취향 태그")
    List<TagGroupResponse> allTags,

    @Schema(description = "사용자가 작성한 게시글 목록")
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
      @Schema(description = "태그 타입", example = "ACTIVITY")
      TagType tagType,

      @Schema(description = "해당 타입의 전체 태그 이름 목록", example = "[\"액티비티\", \"맛집탐방\"]")
      List<String> tags
  ) {

    public TagGroupResponse {
      tags = List.copyOf(tags);
    }
  }

  public record PostResponse(
      @Schema(description = "게시글 ID", example = "1")
      Long postId,

      @Schema(description = "게시글 제목", example = "주말에 파리 근교 함께 가실 분!")
      String title,

      @Schema(description = "게시글 본문", example = "안녕하세요. 스쿠버다이빙 가실 분 구합니다.")
      String content,

      @Schema(description = "게시글 썸네일 이미지 URL", example = "https://example.com/post-thumbnail.png")
      String thumbnailImageUrl,

      @Schema(description = "게시글 일정 표시 텍스트", example = "26.09.06 ~ 26.09.19 (13일)")
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
