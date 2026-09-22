package org.sopt.buddys.domain.magazine.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Locale;
import org.sopt.buddys.domain.magazine.entity.Magazine;
import org.sopt.buddys.domain.magazine.entity.MagazineCategory;
import org.sopt.buddys.domain.magazine.entity.MagazineSort;
import org.sopt.buddys.domain.magazine.entity.QMagazine;
import org.sopt.buddys.domain.magazine.entity.QMagazineBookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public class MagazineRepositoryImpl implements MagazineRepositoryCustom {

  private static final QMagazine magazine = QMagazine.magazine;
  private static final QMagazineBookmark magazineBookmark = QMagazineBookmark.magazineBookmark;

  private final JPAQueryFactory queryFactory;

  public MagazineRepositoryImpl(EntityManager entityManager) {
    this.queryFactory = new JPAQueryFactory(entityManager);
  }

  @Override
  public Page<Magazine> searchMagazines(
      MagazineCategory category,
      String keyword,
      MagazineSort sort,
      Pageable pageable
  ) {
    BooleanExpression keywordCondition = keywordContains(keyword);
    JPAQuery<Magazine> contentQuery = queryFactory
        .selectFrom(magazine)
        .where(magazine.category.eq(category), keywordCondition);

    if (sort == MagazineSort.BOOKMARK) {
      contentQuery
          .leftJoin(magazineBookmark).on(magazineBookmark.magazine.eq(magazine))
          .groupBy(magazine.id)
          .orderBy(
              magazineBookmark.count().desc(),
              magazine.publishedAt.desc(),
              magazine.id.desc()
          );
    } else {
      contentQuery.orderBy(magazine.publishedAt.desc(), magazine.id.desc());
    }

    List<Magazine> magazines = contentQuery
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize())
        .fetch();

    Long totalCount = queryFactory
        .select(magazine.count())
        .from(magazine)
        .where(magazine.category.eq(category), keywordCondition)
        .fetchOne();

    return new PageImpl<>(magazines, pageable, totalCount == null ? 0L : totalCount);
  }

  private BooleanExpression keywordContains(String keyword) {
    if (keyword == null) {
      return null;
    }
    String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
    return magazine.title.lower().contains(normalizedKeyword)
        .or(magazine.summary.lower().contains(normalizedKeyword));
  }
}
