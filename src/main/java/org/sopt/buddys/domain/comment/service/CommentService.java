package org.sopt.buddys.domain.comment.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.comment.dto.response.CommentListResponse;
import org.sopt.buddys.domain.comment.dto.response.CommentListResponse.CommentResponse;
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

  public CommentListResponse getComments(Long postId) {
    if (!postRepository.existsById(postId)) {
      throw new BaseException(PostErrorCode.POST_NOT_FOUND);
    }

    LocalDateTime now = LocalDateTime.now();
    List<CommentResponse> comments = commentRepository.findAllByPostIdWithAuthorOrderByCreatedAtAsc(postId)
        .stream()
        .map(comment -> new CommentResponse(
            comment.getId(),
            comment.getAuthor().getNickname(),
            comment.getContent(),
            comment.getCreatedAt(),
            toTimeAgo(comment.getCreatedAt(), now)
        ))
        .toList();

    return CommentListResponse.of(comments);
  }

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

  private String toTimeAgo(LocalDateTime createdAt, LocalDateTime now) {
    long seconds = Math.max(0, Duration.between(createdAt, now).getSeconds());
    if (seconds < 60) {
      return "방금 전";
    }

    long minutes = seconds / 60;
    if (minutes < 60) {
      return "%d분 전".formatted(minutes);
    }

    long hours = minutes / 60;
    if (hours < 24) {
      return "%d시간 전".formatted(hours);
    }

    long days = hours / 24;
    if (days < 30) {
      return "%d일 전".formatted(days);
    }

    long months = days / 30;
    if (months < 12) {
      return "%d개월 전".formatted(months);
    }

    return "%d년 전".formatted(days / 365);
  }
}
