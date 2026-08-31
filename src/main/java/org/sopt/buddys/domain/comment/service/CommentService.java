package org.sopt.buddys.domain.comment.service;

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
import org.sopt.buddys.domain.user.service.AuthorProfileMapper;
import org.sopt.buddys.global.common.TimeAgoFormatter;
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
    if (!postRepository.existsByIdAndDeletedAtIsNull(postId)) {
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
            AuthorProfileMapper.maskedNickname(comment.getAuthor()),
            AuthorProfileMapper.maskedProfileImageUrl(comment.getAuthor()),
            comment.getContent(),
            comment.getCreatedAt(),
            TimeAgoFormatter.format(comment.getCreatedAt(), now)
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
    Post post = postRepository.findByIdAndDeletedAtIsNull(postId)
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
}
