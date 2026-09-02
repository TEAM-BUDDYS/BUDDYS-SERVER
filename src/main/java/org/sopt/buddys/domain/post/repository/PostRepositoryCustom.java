package org.sopt.buddys.domain.post.repository;

import java.time.LocalDate;
import java.util.List;
import org.sopt.buddys.domain.post.entity.Post;
import org.sopt.buddys.domain.post.service.command.PostSearchCondition;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface PostRepositoryCustom {

  Slice<Post> searchPosts(Long userId, PostSearchCondition condition, Pageable pageable);

  List<Post> findClosingSoonPosts(LocalDate today, int limit);
}
