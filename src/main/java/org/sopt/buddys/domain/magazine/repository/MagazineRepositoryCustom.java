package org.sopt.buddys.domain.magazine.repository;

import org.sopt.buddys.domain.magazine.entity.Magazine;
import org.sopt.buddys.domain.magazine.entity.MagazineCategory;
import org.sopt.buddys.domain.magazine.entity.MagazineSort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MagazineRepositoryCustom {

  Page<Magazine> searchMagazines(
      MagazineCategory category,
      String keyword,
      MagazineSort sort,
      Pageable pageable
  );
}
