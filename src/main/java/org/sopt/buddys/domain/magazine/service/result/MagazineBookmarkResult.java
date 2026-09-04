package org.sopt.buddys.domain.magazine.service.result;

public record MagazineBookmarkResult(
    Long magazineId,
    boolean isBookmarked
) {
}
