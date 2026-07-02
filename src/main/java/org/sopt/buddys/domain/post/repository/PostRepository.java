package org.sopt.buddys.domain.post.repository;

import java.util.List;
import org.sopt.buddys.domain.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

  List<Post> findByAuthorIdOrderByCreatedAtDesc(Long authorId);
}
