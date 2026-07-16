package org.sopt.buddys.domain.comment.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.comment.entity.Comment;
import org.sopt.buddys.domain.comment.repository.CommentRepository;
import org.sopt.buddys.domain.comment.service.result.CommentListResult;
import org.sopt.buddys.domain.comment.service.result.CommentListResult.CommentResult;
import org.sopt.buddys.domain.post.code.PostErrorCode;
import org.sopt.buddys.domain.post.entity.Post;
import org.sopt.buddys.domain.post.repository.PostRepository;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static org.sopt.buddys.global.common.PageConstants.MAX_PAGE_SIZE;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

  private final CommentRepository commentRepository;
  private final PostRepository postRepository;
  private final UserRepository userRepository;

  public CommentListResult getComments(Long postId, int page, int size) {
    validatePageRequest(page, size);
    if (!postRepository.existsById(postId)) {
      throw new BaseException(PostErrorCode.POST_NOT_FOUND);
    }

    LocalDateTime now = LocalDateTime.now();
    Slice<Comment> commentSlice = commentRepository.findAllByPostIdWithAuthorOrderByCreatedAtAsc(
        postId,
        PageRequest.of(page, size)
    );
    List<CommentResult> comments = commentSlice.getContent()
        .stream()
        .map(comment -> new CommentResult(
            comment.getId(),
            comment.getAuthor().getId(),
            comment.getAuthor().getNickname(),
            comment.getAuthor().getProfileImageUrl(),
            comment.getContent(),
            comment.getCreatedAt(),
            toTimeAgo(comment.getCreatedAt(), now)
        ))
        .toList();

    return new CommentListResult(
        comments,
        commentSlice.getNumber(),
        commentSlice.getSize(),
        commentSlice.hasNext()
    );
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

    Comment comment = commentRepository.save(new Comment(
        post,
        author,
        content.trim()
    ));
    postRepository.increaseCommentCount(postId);
    return comment;
  }

  private void validatePageRequest(int page, int size) {
    if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
      throw new BaseException(GlobalErrorCode.INVALID_REQUEST);
    }
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

    if (days < 365) {
      return "%d개월 전".formatted(Math.min(days / 30, 11));
    }

    return "%d년 전".formatted(days / 365);
  }
}
