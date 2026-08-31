package org.sopt.buddys.domain.post.repository;

import org.sopt.buddys.domain.post.entity.PostBookmark;
import org.sopt.buddys.domain.post.entity.PostBookmarkId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostBookmarkRepository extends JpaRepository<PostBookmark, PostBookmarkId> {
}
