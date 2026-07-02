package org.sopt.buddys.domain.user.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.post.entity.Post;
import org.sopt.buddys.domain.post.repository.PostImageRepository;
import org.sopt.buddys.domain.post.repository.PostRepository;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.dto.response.UserProfileResponse;
import org.sopt.buddys.domain.user.dto.response.UserProfileResponse.PostResponse;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.domain.user.repository.UserTagRepository;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

  private final UserRepository userRepository;
  private final UserTagRepository userTagRepository;
  private final PostRepository postRepository;
  private final PostImageRepository postImageRepository;

  public UserProfileResponse getProfile(Long userId) {
    User user = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));

    List<String> tags = userTagRepository.findTagNamesByUserId(userId);
    List<Post> posts = postRepository.findByAuthorIdOrderByCreatedAtDesc(userId);
    Map<Long, String> thumbnailImageUrls = getThumbnailImageUrls(posts);

    List<PostResponse> postResponses = posts.stream()
        .map(post -> PostResponse.from(post, thumbnailImageUrls.get(post.getId())))
        .toList();

    return UserProfileResponse.of(user, tags, postResponses);
  }

  private Map<Long, String> getThumbnailImageUrls(List<Post> posts) {
    List<Long> postIds = posts.stream()
        .map(Post::getId)
        .toList();

    if (postIds.isEmpty()) {
      return Map.of();
    }

    return postImageRepository.findThumbnailImageUrlsByPostIds(postIds)
        .stream()
        .collect(Collectors.toMap(
            PostImageRepository.PostThumbnailProjection::getPostId,
            PostImageRepository.PostThumbnailProjection::getThumbnailImageUrl,
            (first, second) -> first
        ));
  }
}
