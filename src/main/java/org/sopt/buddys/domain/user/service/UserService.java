package org.sopt.buddys.domain.user.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.post.entity.Post;
import org.sopt.buddys.domain.post.repository.PostImageRepository;
import org.sopt.buddys.domain.post.repository.PostRepository;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.domain.user.repository.UserTagRepository;
import org.sopt.buddys.domain.user.service.result.UserPostsResult;
import org.sopt.buddys.domain.user.service.result.UserPostsResult.PostResult;
import org.sopt.buddys.domain.user.service.result.UserProfileResult;
import org.sopt.buddys.domain.user.service.result.UserProfileResult.TagGroupResult;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

  private static final int REQUIRED_ONBOARDING_TAG_COUNT = 3;

  private final UserRepository userRepository;
  private final UserTagRepository userTagRepository;
  private final PostRepository postRepository;
  private final PostImageRepository postImageRepository;

  public boolean isOnboardingCompleted(User user) {
    return isOnboardingCompleted(user, userTagRepository.countByUserId(user.getId()));
  }

  public boolean isOnboardingCompleted(User user, long tagCount) {
    boolean hasRequiredProfile = user.getGender() != null && user.getBirthDate() != null;
    return hasRequiredProfile && tagCount >= REQUIRED_ONBOARDING_TAG_COUNT;
  }

  public UserProfileResult getProfile(Long userId) {
    User user = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));

    List<UserTagRepository.UserTagProjection> userTags = userTagRepository.findTagsByUserId(userId);
    Map<TagType, List<String>> tagsByType = shuffleTagsByType(groupTagsByType(userTags));
    List<String> representativeTags = getFirstTagsByType(tagsByType);
    List<TagGroupResult> allTags = toTagGroupResponses(tagsByType);

    return new UserProfileResult(user, representativeTags, allTags);
  }

  public UserPostsResult getPosts(Long userId, int page, int size) {
    validateUserExists(userId);
    validatePageRequest(page, size);

    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    Slice<Post> posts = postRepository.findByAuthorId(userId, pageable);
    Map<Long, String> thumbnailImageUrls = getThumbnailImageUrls(posts);

    List<PostResult> postResults = posts.getContent()
        .stream()
        .map(post -> new PostResult(post, thumbnailImageUrls.get(post.getId())))
        .toList();

    return new UserPostsResult(
        postResults,
        posts.getNumber(),
        posts.getSize(),
        posts.hasNext()
    );
  }

  private void validatePageRequest(int page, int size) {
    if (page < 0 || size < 1) {
      throw new BaseException(GlobalErrorCode.INVALID_REQUEST);
    }
  }

  private void validateUserExists(Long userId) {
    if (!userRepository.existsByIdAndDeletedAtIsNull(userId)) {
      throw new BaseException(UserErrorCode.USER_NOT_FOUND);
    }
  }

  private Map<TagType, List<String>> groupTagsByType(
      List<UserTagRepository.UserTagProjection> userTags
  ) {

    return userTags.stream()
        .collect(Collectors.groupingBy(
            UserTagRepository.UserTagProjection::getTagType,
            () -> new EnumMap<>(TagType.class),
            Collectors.mapping(UserTagRepository.UserTagProjection::getTagName, Collectors.toList())
        ));
  }

  private Map<TagType, List<String>> shuffleTagsByType(Map<TagType, List<String>> tagsByType) {
    tagsByType.replaceAll((tagType, tags) -> {
      List<String> shuffledTags = new ArrayList<>(tags);
      Collections.shuffle(shuffledTags);
      return shuffledTags;
    });
    return tagsByType;
  }

  private List<String> getFirstTagsByType(Map<TagType, List<String>> tagsByType) {
    return Stream.of(TagType.ACTIVITY, TagType.INTEREST, TagType.TRAVEL_STYLE)
        .map(tagsByType::get)
        .filter(tags -> tags != null && !tags.isEmpty())
        .map(tags -> tags.get(0))
        .toList();
  }

  private List<TagGroupResult> toTagGroupResponses(Map<TagType, List<String>> tagsByType) {
    return Stream.of(TagType.ACTIVITY, TagType.INTEREST, TagType.TRAVEL_STYLE)
        .map(tagType -> new TagGroupResult(tagType, tagsByType.getOrDefault(tagType, List.of())))
        .toList();
  }

  private Map<Long, String> getThumbnailImageUrls(Slice<Post> posts) {
    List<Long> postIds = posts.getContent()
        .stream()
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
