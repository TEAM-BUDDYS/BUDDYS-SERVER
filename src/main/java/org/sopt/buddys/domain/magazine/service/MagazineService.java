package org.sopt.buddys.domain.magazine.service;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.magazine.code.MagazineErrorCode;
import org.sopt.buddys.domain.magazine.entity.Magazine;
import org.sopt.buddys.domain.magazine.repository.MagazineBookmarkRepository;
import org.sopt.buddys.domain.magazine.repository.MagazineRepository;
import org.sopt.buddys.domain.magazine.service.result.MagazineBookmarkResult;
import org.sopt.buddys.domain.magazine.service.result.MagazineListResult;
import org.sopt.buddys.domain.magazine.service.result.MagazineListResult.MagazineSummaryResult;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MagazineService {

  private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

  private final MagazineRepository magazineRepository;
  private final MagazineBookmarkRepository magazineBookmarkRepository;

  public MagazineListResult getMagazines(Long userId, Integer year, Integer month, int page, int size) {
    YearMonth yearMonth = resolveYearMonth(year, month);
    LocalDate startDate = yearMonth.atDay(1);
    LocalDate endDate = yearMonth.atEndOfMonth();

    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id")));
    Page<Magazine> magazinePage = magazineRepository.findByPublishedAtBetween(startDate, endDate, pageable);

    Set<Long> bookmarkedMagazineIds = getBookmarkedMagazineIds(userId, magazinePage.getContent());
    List<MagazineSummaryResult> magazines = magazinePage.getContent().stream()
        .map(magazine -> new MagazineSummaryResult(magazine, bookmarkedMagazineIds.contains(magazine.getId())))
        .toList();

    return new MagazineListResult(
        yearMonth.getYear(),
        yearMonth.getMonthValue(),
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

  private YearMonth resolveYearMonth(Integer year, Integer month) {
    if (year == null && month == null) {
      return YearMonth.now(SEOUL_ZONE);
    }
    if (year == null || month == null) {
      throw new BaseException(MagazineErrorCode.INVALID_YEAR_MONTH);
    }
    try {
      return YearMonth.of(year, month);
    } catch (DateTimeException e) {
      throw new BaseException(MagazineErrorCode.INVALID_YEAR_MONTH);
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
