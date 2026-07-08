package org.sopt.buddys.domain.post.repository;

import java.util.List;
import org.sopt.buddys.domain.post.entity.AgeCondition;
import org.sopt.buddys.domain.post.entity.PostAgeCondition;
import org.sopt.buddys.domain.post.entity.PostAgeConditionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostAgeConditionRepository extends JpaRepository<PostAgeCondition, PostAgeConditionId> {

  @Query("""
      select pac.id.ageCondition
      from PostAgeCondition pac
      where pac.post.id = :postId
      """)
  List<AgeCondition> findAgeConditionsByPostId(@Param("postId") Long postId);
}
