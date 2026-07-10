package org.sopt.buddys.domain.recommendation.service.result;

import org.sopt.buddys.domain.post.entity.Post;

public record RecommendedPostResult(
    Post post,
    double totalSimilarity,
    double activitySimilarity,
    String thumbnailUrl
) {}