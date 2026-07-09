package org.sopt.buddys.domain.post.repository;

import org.sopt.buddys.domain.post.entity.PostAgeCondition;
import org.sopt.buddys.domain.post.entity.PostAgeConditionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostAgeConditionRepository extends JpaRepository<PostAgeCondition, PostAgeConditionId> {
}
