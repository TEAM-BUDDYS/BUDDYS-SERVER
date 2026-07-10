package org.sopt.buddys.domain.post.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.sopt.buddys.domain.post.entity.GenderCondition;
import org.sopt.buddys.domain.post.entity.Post;
import org.sopt.buddys.domain.post.entity.PostStatus;
import org.sopt.buddys.domain.post.entity.QPost;
import org.sopt.buddys.domain.post.entity.QPostAgeCondition;
import org.sopt.buddys.domain.post.entity.QPostGenderCondition;
import org.sopt.buddys.domain.post.entity.QPostTag;
import org.sopt.buddys.domain.post.service.command.PostSearchCondition;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

public class PostRepositoryImpl implements PostRepositoryCustom {

  private static final QPost post = QPost.post;
  private static final QPostAgeCondition postAgeCondition = QPostAgeCondition.postAgeCondition;
  private static final QPostGenderCondition postGenderCondition = QPostGenderCondition.postGenderCondition;
  private static final QPostTag postTag = QPostTag.postTag;

  private final JPAQueryFactory queryFactory;

  public PostRepositoryImpl(EntityManager entityManager) {
    this.queryFactory = new JPAQueryFactory(entityManager);
  }

  @Override
  public Slice<Post> searchPosts(Long userId, PostSearchCondition condition, Pageable pageable) {
    List<Post> posts = queryFactory
        .selectFrom(post)
        .distinct()
        .join(post.country).fetchJoin()
        .where(toPredicate(userId, condition))
        .orderBy(post.createdAt.desc(), post.id.desc())
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize() + 1L)
        .fetch();

    boolean hasNext = posts.size() > pageable.getPageSize();
    if (hasNext) {
      posts = posts.subList(0, pageable.getPageSize());
    }
    return new SliceImpl<>(posts, pageable, hasNext);
  }

  private BooleanBuilder toPredicate(Long userId, PostSearchCondition condition) {
    BooleanBuilder builder = new BooleanBuilder()
        .and(post.status.eq(PostStatus.RECRUITING))
        .and(post.author.id.ne(userId))
        .and(keywordContains(condition.keyword()))
        .and(countryEquals(condition.countryId()))
        .and(startDateGoe(condition))
        .and(endDateLoe(condition))
        .and(ageConditionIn(condition))
        .and(genderConditionIn(condition))
        .and(companionTypeIn(condition))
        .and(tagEquals(condition.tagId()));

    return builder;
  }

  private BooleanExpression keywordContains(String keyword) {
    String normalizedKeyword = normalizeKeyword(keyword);
    if (normalizedKeyword == null) {
      return null;
    }
    return post.title.lower().contains(normalizedKeyword)
        .or(post.content.lower().contains(normalizedKeyword));
  }

  private BooleanExpression countryEquals(Long countryId) {
    if (countryId == null) {
      return null;
    }
    return post.country.id.eq(countryId);
  }

  private BooleanExpression startDateGoe(PostSearchCondition condition) {
    if (condition.startDate() == null) {
      return null;
    }
    return post.startDate.goe(condition.startDate());
  }

  private BooleanExpression endDateLoe(PostSearchCondition condition) {
    if (condition.endDate() == null) {
      return null;
    }
    return post.endDate.loe(condition.endDate());
  }

  private BooleanExpression ageConditionIn(PostSearchCondition condition) {
    if (condition.ageConditions() == null || condition.ageConditions().isEmpty()) {
      return null;
    }
    return post.id.in(
        JPAExpressions
            .select(postAgeCondition.post.id)
            .from(postAgeCondition)
            .where(postAgeCondition.id.ageCondition.in(condition.ageConditions()))
    );
  }

  private BooleanExpression genderConditionIn(PostSearchCondition condition) {
    if (condition.genderConditions() == null || condition.genderConditions().isEmpty()) {
      return null;
    }
    return post.id.in(
        JPAExpressions
            .select(postGenderCondition.post.id)
            .from(postGenderCondition)
            .where(postGenderCondition.id.genderCondition.in(expandGenderConditions(condition.genderConditions())))
    );
  }

  private BooleanExpression companionTypeIn(PostSearchCondition condition) {
    if (condition.companionTypes() == null || condition.companionTypes().isEmpty()) {
      return null;
    }
    return post.companionType.in(condition.companionTypes());
  }

  private BooleanExpression tagEquals(Long tagId) {
    if (tagId == null) {
      return null;
    }
    return post.id.in(
        JPAExpressions
            .select(postTag.post.id)
            .from(postTag)
            .where(postTag.tag.id.eq(tagId))
    );
  }

  private Set<GenderCondition> expandGenderConditions(List<GenderCondition> genderConditions) {
    Set<GenderCondition> expandedGenderConditions = EnumSet.copyOf(genderConditions);
    if (expandedGenderConditions.contains(GenderCondition.MALE)
        || expandedGenderConditions.contains(GenderCondition.FEMALE)) {
      expandedGenderConditions.add(GenderCondition.ANY);
    }
    return expandedGenderConditions;
  }

  private String normalizeKeyword(String keyword) {
    if (keyword == null || keyword.isBlank()) {
      return null;
    }
    return keyword.trim().toLowerCase(Locale.ROOT);
  }
}
