package org.sopt.buddys.domain.recommendation.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.post.entity.Post;
import org.sopt.buddys.domain.post.entity.PostStatus;
import org.sopt.buddys.domain.post.repository.PostImageRepository;
import org.sopt.buddys.domain.post.repository.PostImageRepository.PostThumbnailProjection;
import org.sopt.buddys.domain.post.repository.PostRepository;
import org.sopt.buddys.domain.post.repository.PostRepository.AuthorPostCountProjection;
import org.sopt.buddys.domain.post.repository.PostTagRepository;
import org.sopt.buddys.domain.post.repository.PostTagRepository.PostTagBulkProjection;
import org.sopt.buddys.domain.recommendation.code.ExchangeCountryRecommendationErrorCode;
import org.sopt.buddys.domain.recommendation.code.RecommendationErrorCode;
import org.sopt.buddys.domain.recommendation.service.result.RecommendedPostResult;
import org.sopt.buddys.domain.recommendation.service.result.RecommendedUserResult;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.domain.user.repository.UserTagRepository;
import org.sopt.buddys.domain.user.repository.UserTagRepository.UserTagBulkProjection;
import org.sopt.buddys.domain.user.service.UserService;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

  private final UserRepository userRepository;
  private final UserTagRepository userTagRepository;
  private final PostRepository postRepository;
  private final PostTagRepository postTagRepository;
  private final PostImageRepository postImageRepository;
  private final UserService userService;
  private final TagSimilarityCalculator similarityCalculator;

  public List<RecommendedUserResult> getRecommendedUsers(Long userId, int size) {
    RequesterContext ctx = resolveRequester(userId);

    List<User> candidates = userRepository.findByInterestCountryIdAndIdNotAndDeletedAtIsNull(ctx.interestCountryId(), userId);
    List<Long> candidateIds = candidates.stream().map(User::getId).toList();

    Map<Long, Map<TagType, Set<Long>>> candidateTags = fetchUserTagMap(candidateIds);

    List<User> onboardedCandidates = candidates.stream()
        .filter(candidate -> userService.isOnboardingCompleted(candidate, tagCountOf(candidateTags, candidate.getId())))
        .toList();

    Map<Long, Long> postCounts = fetchPostCounts(onboardedCandidates.stream().map(User::getId).toList());

    return onboardedCandidates.stream()
        .map(candidate -> {
          Map<TagType, Set<Long>> tags = candidateTags.getOrDefault(candidate.getId(), Map.of());
          TagSimilarityCalculator.SimilarityScore score = similarityCalculator.similarityOf(ctx.myTags(), tags);
          long postCount = postCounts.getOrDefault(candidate.getId(), 0L);
          return new RecommendedUserResult(candidate, score.total(), score.activity(), postCount);
        })
        .sorted(Comparator.comparingDouble(RecommendedUserResult::totalSimilarity).reversed()
            .thenComparing(Comparator.comparingDouble(RecommendedUserResult::activitySimilarity).reversed())
            .thenComparing(Comparator.comparingLong(RecommendedUserResult::postCount).reversed()))
        .limit(size)
        .toList();
  }

  public List<RecommendedPostResult> getRecommendedPosts(Long userId, int size) {
    RequesterContext ctx = resolveRequester(userId);

    List<Post> candidates = postRepository.findByCountryIdAndStatusAndAuthorIdNot(ctx.interestCountryId(), PostStatus.RECRUITING, userId);
    List<Long> candidateIds = candidates.stream().map(Post::getId).toList();

    Map<Long, Map<TagType, Set<Long>>> postTags = fetchPostTagMap(candidateIds);
    Map<Long, String> thumbnails = fetchThumbnails(candidateIds);

    return candidates.stream()
        .map(post -> {
          Map<TagType, Set<Long>> tags = postTags.getOrDefault(post.getId(), Map.of());
          TagSimilarityCalculator.SimilarityScore score = similarityCalculator.similarityOf(ctx.myTags(), tags);
          String thumbnailUrl = thumbnails.get(post.getId());
          return new RecommendedPostResult(post, score.total(), score.activity(), thumbnailUrl);
        })
        .sorted(Comparator.comparingDouble(RecommendedPostResult::totalSimilarity).reversed()
            .thenComparing(Comparator.comparingDouble(RecommendedPostResult::activitySimilarity).reversed())
            .thenComparing(Comparator.comparingLong((RecommendedPostResult result) -> result.post().getViewCount()).reversed()))
        .limit(size)
        .toList();
  }

  public List<RecommendedUserResult> getExchangeCountryRecommendedUsers(Long userId, int size) {
    User me = userRepository.findByIdWithExchangeCountry(userId)
        .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));

    if (me.getExchangeCountry() == null) {
      throw new BaseException(ExchangeCountryRecommendationErrorCode.EXCHANGE_COUNTRY_NOT_SET);
    }

    List<User> candidates = userRepository.findByExchangeCountryIdWithExchangeCountry(
        me.getExchangeCountry().getId(),
        userId
    );
    List<Long> userIds = candidates.stream()
        .map(User::getId)
        .collect(Collectors.toList());
    userIds.add(userId);

    Map<Long, Map<TagType, Set<Long>>> tagsByUserId = fetchUserTagMap(userIds);
    Map<TagType, Set<Long>> myTags = tagsByUserId.getOrDefault(userId, Map.of());

    return candidates.stream()
        .map(candidate -> {
          Map<TagType, Set<Long>> candidateTags = tagsByUserId.getOrDefault(candidate.getId(), Map.of());
          TagSimilarityCalculator.SimilarityScore score = similarityCalculator.similarityOf(myTags, candidateTags);
          return new RecommendedUserResult(candidate, score.total(), score.activity());
        })
        .sorted(Comparator.comparingDouble(RecommendedUserResult::totalSimilarity).reversed()
            .thenComparing(Comparator.comparingDouble(RecommendedUserResult::activitySimilarity).reversed())
            .thenComparing(result -> result.user().getNickname())
            .thenComparing(result -> result.user().getId()))
        .limit(size)
        .toList();
  }

  private User getUserWithInterestCountry(Long userId) {
    User user = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));
    if (user.getInterestCountry() == null) {
      throw new BaseException(RecommendationErrorCode.INTEREST_COUNTRY_NOT_SET);
    }
    return user;
  }

  private record RequesterContext(User me, Long interestCountryId, Map<TagType, Set<Long>> myTags) {}

  private RequesterContext resolveRequester(Long userId) {
    User me = getUserWithInterestCountry(userId);
    Map<TagType, Set<Long>> myTags = fetchUserTagMap(List.of(userId)).getOrDefault(userId, Map.of());
    return new RequesterContext(me, me.getInterestCountry().getId(), myTags);
  }

  private Map<Long, Long> fetchPostCounts(List<Long> authorIds) {
    if (authorIds.isEmpty()) {
      return Map.of();
    }
    return postRepository.countByAuthorIdIn(authorIds).stream()
        .collect(Collectors.toMap(AuthorPostCountProjection::getAuthorId, AuthorPostCountProjection::getPostCount));
  }

  private long tagCountOf(Map<Long, Map<TagType, Set<Long>>> tagMap, Long userId) {
    return tagMap.getOrDefault(userId, Map.of()).values().stream().mapToLong(Set::size).sum();
  }

  private Map<Long, String> fetchThumbnails(List<Long> postIds) {
    if (postIds.isEmpty()) {
      return Map.of();
    }
    return postImageRepository.findThumbnailImageUrlsByPostIds(postIds).stream()
        .collect(Collectors.toMap(
            PostThumbnailProjection::getPostId,
            PostThumbnailProjection::getThumbnailImageUrl,
            (first, second) -> first
        ));
  }

  private Map<Long, Map<TagType, Set<Long>>> fetchUserTagMap(List<Long> userIds) {
    if (userIds.isEmpty()) {
      return Map.of();
    }
    return userTagRepository.findAllByUserIdIn(userIds).stream()
        .collect(Collectors.groupingBy(
            UserTagBulkProjection::getUserId,
            Collectors.groupingBy(
                UserTagBulkProjection::getTagType,
                Collectors.mapping(UserTagBulkProjection::getTagId, Collectors.toSet())
            )
        ));
  }

  private Map<Long, Map<TagType, Set<Long>>> fetchPostTagMap(List<Long> postIds) {
    if (postIds.isEmpty()) {
      return Map.of();
    }
    return postTagRepository.findAllByPostIdIn(postIds).stream()
        .collect(Collectors.groupingBy(
            PostTagBulkProjection::getPostId,
            Collectors.groupingBy(
                PostTagBulkProjection::getTagType,
                Collectors.mapping(PostTagBulkProjection::getTagId, Collectors.toSet())
            )
        ));
  }
}