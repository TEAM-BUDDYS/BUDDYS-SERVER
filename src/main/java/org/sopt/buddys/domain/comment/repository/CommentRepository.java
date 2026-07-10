package org.sopt.buddys.domain.comment.repository;

import org.sopt.buddys.domain.comment.entity.Comment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

  @Query("""
      select c
      from Comment c
      join fetch c.author
      where c.post.id = :postId
      order by c.createdAt asc
      """)
  Slice<Comment> findAllByPostIdWithAuthorOrderByCreatedAtAsc(
      @Param("postId") Long postId,
      Pageable pageable
  );
}
