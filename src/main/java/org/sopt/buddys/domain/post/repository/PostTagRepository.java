package org.sopt.buddys.domain.post.repository;

import org.sopt.buddys.domain.post.entity.PostTag;
import org.sopt.buddys.domain.post.entity.PostTagId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostTagRepository extends JpaRepository<PostTag, PostTagId> {
}
