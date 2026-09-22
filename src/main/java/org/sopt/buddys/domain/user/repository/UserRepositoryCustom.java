package org.sopt.buddys.domain.user.repository;

import org.sopt.buddys.domain.user.entity.AccountStatus;
import org.sopt.buddys.domain.user.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface UserRepositoryCustom {

  Slice<User> searchActiveUsersByNickname(
      String keyword,
      Long excludeUserId,
      AccountStatus accountStatus,
      Pageable pageable
  );
}
