package org.sopt.buddys.domain.recommendation.service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.recommendation.code.RecommendationErrorCode;
import org.sopt.buddys.domain.recommendation.service.result.RecommendedUserResult;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.domain.user.repository.UserTagRepository;
import org.sopt.buddys.domain.user.repository.UserTagRepository.UserTagBulkProjection;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

  private static final double ACTIVITY_WEIGHT = 0.4;
  private static final double INTEREST_WEIGHT = 0.3;
  private static final double TRAVEL_STYLE_WEIGHT = 0.3;

  private final UserRepository userRepository;
  private final UserTagRepository userTagRepository;

  public List<RecommendedUserResult> getExchangeCountryRecommendedUsers(Long userId, int size) {
    User me = userRepository.findByIdWithExchangeCountry(userId)
        .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));

    if (me.getExchangeCountry() == null) {
      throw new BaseException(RecommendationErrorCode.EXCHANGE_COUNTRY_NOT_SET);
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
          SimilarityScore score = similarityOf(myTags, candidateTags);
          return new RecommendedUserResult(candidate, score.total(), score.activity());
        })
        .sorted(Comparator.comparingDouble(RecommendedUserResult::totalSimilarity).reversed()
            .thenComparing(Comparator.comparingDouble(RecommendedUserResult::activitySimilarity).reversed())
            .thenComparing(result -> result.user().getNickname())
            .thenComparing(result -> result.user().getId()))
        .limit(size)
        .toList();
  }

  private Map<Long, Map<TagType, Set<Long>>> fetchUserTagMap(List<Long> userIds) {
    if (userIds.isEmpty()) {
      return Map.of();
    }
    return userTagRepository.findAllByUserIdIn(userIds)
        .stream()
        .collect(Collectors.groupingBy(
            UserTagBulkProjection::getUserId,
            Collectors.groupingBy(
                UserTagBulkProjection::getTagType,
                Collectors.mapping(UserTagBulkProjection::getTagId, Collectors.toSet())
            )
        ));
  }

  private SimilarityScore similarityOf(Map<TagType, Set<Long>> a, Map<TagType, Set<Long>> b) {
    double activity = categorySimilarity(a, b, TagType.ACTIVITY);
    double total = ACTIVITY_WEIGHT * activity
        + INTEREST_WEIGHT * categorySimilarity(a, b, TagType.INTEREST)
        + TRAVEL_STYLE_WEIGHT * categorySimilarity(a, b, TagType.TRAVEL_STYLE);
    return new SimilarityScore(total, activity);
  }

  private double categorySimilarity(Map<TagType, Set<Long>> a, Map<TagType, Set<Long>> b, TagType type) {
    return jaccard(a.getOrDefault(type, Set.of()), b.getOrDefault(type, Set.of()));
  }

  private double jaccard(Set<Long> a, Set<Long> b) {
    if (a.isEmpty() && b.isEmpty()) {
      return 0.0;
    }
    Set<Long> union = new HashSet<>(a);
    union.addAll(b);
    Set<Long> intersection = new HashSet<>(a);
    intersection.retainAll(b);
    return (double) intersection.size() / union.size();
  }

  private record SimilarityScore(double total, double activity) {
  }
}
