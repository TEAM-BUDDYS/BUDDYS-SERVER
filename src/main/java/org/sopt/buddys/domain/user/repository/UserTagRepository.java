package org.sopt.buddys.domain.user.repository;

import java.util.List;
import org.sopt.buddys.domain.user.entity.UserTag;
import org.sopt.buddys.domain.user.entity.UserTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserTagRepository extends JpaRepository<UserTag, UserTagId> {

  @Query("""
      select t.name
      from UserTag ut
      join ut.tag t
      where ut.user.id = :userId
      order by ut.createdAt asc
      """)
  List<String> findTagNamesByUserId(@Param("userId") Long userId);
}
