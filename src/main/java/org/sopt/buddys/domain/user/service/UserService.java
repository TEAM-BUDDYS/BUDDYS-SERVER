package org.sopt.buddys.domain.user.service;

import java.util.ArrayList;
import java.util.Collections;
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
import org.sopt.buddys.domain.user.dto.response.UserProfileResponse;
import org.sopt.buddys.domain.user.dto.response.UserProfileResponse.PostResponse;
import org.sopt.buddys.domain.user.dto.response.UserProfileResponse.TagGroupResponse;
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

    List<UserTagRepository.UserTagProjection> userTags = userTagRepository.findTagsByUserId(userId);
    Map<TagType, List<String>> tagsByType = shuffleTagsByType(groupTagsByType(userTags));
    List<String> representativeTags = getFirstTagsByType(tagsByType);
    List<TagGroupResponse> allTags = toTagGroupResponses(tagsByType);
    List<Post> posts = postRepository.findByAuthorIdOrderByCreatedAtDesc(userId);
    Map<Long, String> thumbnailImageUrls = getThumbnailImageUrls(posts);

    List<PostResponse> postResponses = posts.stream()
        .map(post -> PostResponse.from(post, thumbnailImageUrls.get(post.getId())))
        .toList();

    return UserProfileResponse.of(user, representativeTags, allTags, postResponses);
  }

  private Map<TagType, List<String>> groupTagsByType(
      List<UserTagRepository.UserTagProjection> userTags
  ) {

    return userTags.stream()
        .collect(Collectors.groupingBy(
            UserTagRepository.UserTagProjection::getTagType,
            () -> new java.util.EnumMap<>(TagType.class),
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

  private List<TagGroupResponse> toTagGroupResponses(Map<TagType, List<String>> tagsByType) {
    return Stream.of(TagType.ACTIVITY, TagType.INTEREST, TagType.TRAVEL_STYLE)
        .map(tagType -> new TagGroupResponse(tagType, tagsByType.getOrDefault(tagType, List.of())))
        .toList();
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
