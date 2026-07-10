package org.sopt.buddys.domain.recommendation.service.result;

import org.sopt.buddys.domain.user.entity.User;

public record RecommendedUserResult(
    User user,
    double totalSimilarity,
    double activitySimilarity,
    long postCount
) {}