package org.sopt.buddys.domain.user.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Locale;
import org.sopt.buddys.domain.user.entity.AccountStatus;
import org.sopt.buddys.domain.user.entity.QUser;
import org.sopt.buddys.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

public class UserRepositoryImpl implements UserRepositoryCustom {

  private static final QUser user = QUser.user;

  private final JPAQueryFactory queryFactory;

  public UserRepositoryImpl(EntityManager entityManager) {
    this.queryFactory = new JPAQueryFactory(entityManager);
  }

  @Override
  public Slice<User> searchActiveUsersByNickname(
      String keyword,
      Long excludeUserId,
      AccountStatus accountStatus,
      Pageable pageable
  ) {
    List<User> users = queryFactory
        .selectFrom(user)
        .where(
            user.nickname.lower().contains(keyword.toLowerCase(Locale.ROOT)),
            user.id.ne(excludeUserId),
            user.accountStatus.eq(accountStatus),
            user.deletedAt.isNull()
        )
        .orderBy(user.createdAt.desc(), user.id.desc())
        .offset(pageable.getOffset())
        .limit(pageable.getPageSize() + 1L)
        .fetch();

    boolean hasNext = users.size() > pageable.getPageSize();
    if (hasNext) {
      users = users.subList(0, pageable.getPageSize());
    }
    return new SliceImpl<>(users, pageable, hasNext);
  }
}
