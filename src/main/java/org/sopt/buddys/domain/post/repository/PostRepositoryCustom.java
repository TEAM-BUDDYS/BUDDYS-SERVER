package org.sopt.buddys.domain.post.repository;

import java.time.LocalDate;
import java.util.List;
import org.sopt.buddys.domain.post.entity.Post;
import org.sopt.buddys.domain.post.service.command.PostSearchCondition;
import org.sopt.buddys.domain.search.service.command.SearchSort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface PostRepositoryCustom {

  Slice<Post> searchPosts(Long userId, PostSearchCondition condition, SearchSort sort, Pageable pageable);

  long countPosts(PostSearchCondition condition);

  List<Post> findClosingSoonPosts(LocalDate today, int limit);
}
