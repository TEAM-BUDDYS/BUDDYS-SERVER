package org.sopt.buddys.domain.post.repository;

import org.sopt.buddys.domain.post.entity.PostGenderCondition;
import org.sopt.buddys.domain.post.entity.PostGenderConditionId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostGenderConditionRepository
    extends JpaRepository<PostGenderCondition, PostGenderConditionId> {
}
