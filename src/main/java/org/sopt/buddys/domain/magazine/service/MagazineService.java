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
import org.sopt.buddys.domain.magazine.service.result.BookmarkedMagazineListResult;
import org.sopt.buddys.domain.magazine.service.result.MagazineListResult;
import org.sopt.buddys.domain.magazine.service.result.MagazineListResult.MagazineSummaryResult;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.sopt.buddys.global.common.PageConstants.MAX_PAGE_SIZE;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MagazineService {

  private final MagazineRepository magazineRepository;
  private final MagazineBookmarkRepository magazineBookmarkRepository;
  private final UserRepository userRepository;

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

  public BookmarkedMagazineListResult getBookmarkedMagazines(Long userId, int page, int size) {
    validatePageRequest(page, size);
    Pageable pageable = PageRequest.of(page, size);
    Slice<Magazine> magazines = magazineBookmarkRepository.findBookmarkedMagazinesByUserId(userId, pageable);

    return new BookmarkedMagazineListResult(
        magazines.getContent(),
        magazines.getNumber(),
        magazines.getSize(),
        magazines.hasNext()
    );
  }

  private void validatePageRequest(int page, int size) {
    if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
      throw new BaseException(GlobalErrorCode.INVALID_REQUEST);
    }
  }

  @Transactional
  public MagazineBookmarkResult bookmarkMagazine(Long userId, Long magazineId) {
    validateMagazineExists(magazineId);
    userRepository.findActiveByIdForUpdate(userId)
        .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));

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
