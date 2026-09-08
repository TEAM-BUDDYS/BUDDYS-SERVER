package org.sopt.buddys.domain.post.service.result;

public record PostBookmarkResult(
    Long postId,
    boolean isBookmarked
) {
}
