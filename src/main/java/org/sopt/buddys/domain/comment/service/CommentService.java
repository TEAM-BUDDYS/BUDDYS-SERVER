package org.sopt.buddys.domain.comment.service;

import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.comment.entity.Comment;
import org.sopt.buddys.domain.comment.repository.CommentRepository;
import org.sopt.buddys.domain.post.code.PostErrorCode;
import org.sopt.buddys.domain.post.entity.Post;
import org.sopt.buddys.domain.post.repository.PostRepository;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

  private final CommentRepository commentRepository;
  private final PostRepository postRepository;
  private final UserRepository userRepository;

  @Transactional
  public Comment createComment(
      Long userId,
      Long postId,
      String content
  ) {
    User author = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));
    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new BaseException(PostErrorCode.POST_NOT_FOUND));

    post.increaseCommentCount();

    return commentRepository.save(new Comment(
        post,
        author,
        content.trim()
    ));
  }
}
