package org.sopt.buddys.domain.post.repository;

import java.util.List;
import org.sopt.buddys.domain.post.entity.GenderCondition;
import org.sopt.buddys.domain.post.entity.PostGenderCondition;
import org.sopt.buddys.domain.post.entity.PostGenderConditionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostGenderConditionRepository
    extends JpaRepository<PostGenderCondition, PostGenderConditionId> {

  void deleteAllByPostId(Long postId);

  @Query("""
      select pgc.id.genderCondition
      from PostGenderCondition pgc
      where pgc.post.id = :postId
      """)
  List<GenderCondition> findGenderConditionsByPostId(@Param("postId") Long postId);
}
