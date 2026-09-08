package org.sopt.buddys.domain.magazine.repository;

import java.time.LocalDate;
import org.sopt.buddys.domain.magazine.entity.Magazine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MagazineRepository extends JpaRepository<Magazine, Long> {

  Page<Magazine> findByPublishedAtBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);
}
