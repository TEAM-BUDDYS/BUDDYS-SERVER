package org.sopt.buddys.domain.post.repository;

import org.sopt.buddys.domain.post.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

  Slice<Post> findByAuthorId(Long authorId, Pageable pageable);
}
