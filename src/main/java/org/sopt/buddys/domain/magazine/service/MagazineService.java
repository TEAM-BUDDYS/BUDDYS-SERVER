package org.sopt.buddys.domain.magazine.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.magazine.code.MagazineErrorCode;
import org.sopt.buddys.domain.magazine.entity.Magazine;
import org.sopt.buddys.domain.magazine.entity.MagazineCategory;
import org.sopt.buddys.domain.magazine.entity.MagazineSort;
import org.sopt.buddys.domain.magazine.repository.MagazineBookmarkRepository;
import org.sopt.buddys.domain.magazine.repository.MagazineRepository;
import org.sopt.buddys.domain.magazine.service.result.MagazineBookmarkResult;
import org.sopt.buddys.domain.magazine.service.result.MagazineListResult;
import org.sopt.buddys.domain.magazine.service.result.MagazineListResult.MagazineSummaryResult;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MagazineService {

  private final MagazineRepository magazineRepository;
  private final MagazineBookmarkRepository magazineBookmarkRepository;

  public MagazineListResult getMagazines(
      Long userId,
      MagazineCategory category,
      String keyword,
      MagazineSort sort,
      int page,
      int size
  ) {
    Pageable pageable = PageRequest.of(page, size);
    Page<Magazine> magazinePage = magazineRepository.searchMagazines(category, keyword, sort, pageable);

    Set<Long> bookmarkedMagazineIds = getBookmarkedMagazineIds(userId, magazinePage.getContent());
    List<MagazineSummaryResult> magazines = magazinePage.getContent().stream()
        .map(magazine -> new MagazineSummaryResult(magazine, bookmarkedMagazineIds.contains(magazine.getId())))
        .toList();

    return new MagazineListResult(
        magazinePage.getTotalElements(),
        magazinePage.getNumber(),
        magazinePage.getSize(),
        magazinePage.hasNext(),
        magazines
    );
  }

  @Transactional
  public MagazineBookmarkResult bookmarkMagazine(Long userId, Long magazineId) {
    validateMagazineExists(magazineId);

    magazineBookmarkRepository.insertOrKeep(userId, magazineId);
    return new MagazineBookmarkResult(magazineId, true);
  }

  @Transactional
  public MagazineBookmarkResult removeMagazineBookmark(Long userId, Long magazineId) {
    validateMagazineExists(magazineId);

    magazineBookmarkRepository.deleteByUserIdAndMagazineId(userId, magazineId);
    return new MagazineBookmarkResult(magazineId, false);
  }

  private void validateMagazineExists(Long magazineId) {
    if (!magazineRepository.existsById(magazineId)) {
      throw new BaseException(MagazineErrorCode.MAGAZINE_NOT_FOUND);
    }
  }

  private Set<Long> getBookmarkedMagazineIds(Long userId, List<Magazine> magazines) {
    List<Long> magazineIds = magazines.stream()
        .map(Magazine::getId)
        .toList();

    if (magazineIds.isEmpty()) {
      return Set.of();
    }
    return new HashSet<>(magazineBookmarkRepository.findBookmarkedMagazineIds(userId, magazineIds));
  }
}
