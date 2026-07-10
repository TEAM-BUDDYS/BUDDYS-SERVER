package org.sopt.buddys.domain.recommendation.service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.springframework.stereotype.Component;

@Component
public class TagSimilarityCalculator {

  private static final double ACTIVITY_WEIGHT = 0.4;
  private static final double INTEREST_WEIGHT = 0.3;
  private static final double TRAVEL_STYLE_WEIGHT = 0.3;

  public record SimilarityScore(double total, double activity) {
  }

  public SimilarityScore similarityOf(Map<TagType, Set<Long>> a, Map<TagType, Set<Long>> b) {
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
}