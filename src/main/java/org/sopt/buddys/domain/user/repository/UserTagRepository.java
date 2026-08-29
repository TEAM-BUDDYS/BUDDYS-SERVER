package org.sopt.buddys.domain.user.repository;

import java.util.Collection;
import java.util.List;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.domain.user.entity.UserTag;
import org.sopt.buddys.domain.user.entity.UserTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserTagRepository extends JpaRepository<UserTag, UserTagId> {

  @Query("""
      select t.tagType as tagType,
             t.id as tagId,
             t.name as tagName,
             ut.displayOrder as displayOrder
      from UserTag ut
      join ut.tag t
      where ut.user.id = :userId
      order by ut.displayOrder asc
      """)
  List<UserTagProjection> findTagsByUserId(@Param("userId") Long userId);

  boolean existsByUserId(Long userId);

  long countByUserId(Long userId);

  @Query("""
      select ut
      from UserTag ut
      join fetch ut.tag t
      where ut.user.id = :userId
      order by ut.displayOrder asc
      """)
  List<UserTag> findAllWithTagByUserId(@Param("userId") Long userId);

  @Modifying
  @Query("delete from UserTag ut where ut.user.id = :userId")
  int deleteAllByUserId(@Param("userId") Long userId);

  @Query("""
      select ut.user.id as userId,
             t.id as tagId,
             t.tagType as tagType
      from UserTag ut
      join ut.tag t
      where ut.user.id in :userIds
      """)
  List<UserTagBulkProjection> findAllByUserIdIn(@Param("userIds") Collection<Long> userIds);

  interface UserTagBulkProjection {
    Long getUserId();
    Long getTagId();
    TagType getTagType();
  }

  interface UserTagProjection {
    TagType getTagType();
    Long getTagId();
    String getTagName();
    int getDisplayOrder();
  }
}
