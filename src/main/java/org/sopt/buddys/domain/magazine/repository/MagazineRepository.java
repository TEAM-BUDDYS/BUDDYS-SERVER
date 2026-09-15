package org.sopt.buddys.domain.magazine.repository;

import org.sopt.buddys.domain.magazine.entity.Magazine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MagazineRepository extends JpaRepository<Magazine, Long>, MagazineRepositoryCustom {
}
