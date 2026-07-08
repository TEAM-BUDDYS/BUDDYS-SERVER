package org.sopt.buddys.domain.post.service;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.location.code.LocationErrorCode;
import org.sopt.buddys.domain.location.entity.City;
import org.sopt.buddys.domain.location.entity.Country;
import org.sopt.buddys.domain.location.repository.CityRepository;
import org.sopt.buddys.domain.location.repository.CountryRepository;
import org.sopt.buddys.domain.post.code.PostErrorCode;
import org.sopt.buddys.domain.post.dto.request.CreatePostRequest;
import org.sopt.buddys.domain.post.entity.AgeCondition;
import org.sopt.buddys.domain.post.entity.Post;
import org.sopt.buddys.domain.post.entity.PostAgeCondition;
import org.sopt.buddys.domain.post.entity.PostImage;
import org.sopt.buddys.domain.post.entity.PostTag;
import org.sopt.buddys.domain.post.repository.PostAgeConditionRepository;
import org.sopt.buddys.domain.post.repository.PostImageRepository;
import org.sopt.buddys.domain.post.repository.PostRepository;
import org.sopt.buddys.domain.post.repository.PostTagRepository;
import org.sopt.buddys.domain.tag.entity.Tag;
import org.sopt.buddys.domain.tag.repository.TagRepository;
import org.sopt.buddys.domain.user.code.UserErrorCode;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

  private final PostRepository postRepository;
  private final PostAgeConditionRepository postAgeConditionRepository;
  private final PostTagRepository postTagRepository;
  private final PostImageRepository postImageRepository;
  private final UserRepository userRepository;
  private final CountryRepository countryRepository;
  private final CityRepository cityRepository;
  private final TagRepository tagRepository;

  @Transactional
  public Post createPost(Long userId, CreatePostRequest request) {
    validateDateRange(request);
    validateRequiredConditions(request);

    User author = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));
    Country country = countryRepository.findById(request.countryId())
        .orElseThrow(() -> new BaseException(LocationErrorCode.COUNTRY_NOT_FOUND));
    City city = getCity(request.countryId(), request.cityId());

    Post post = postRepository.save(new Post(
        author,
        country,
        city,
        request.title().trim(),
        request.content().trim(),
        request.startDate(),
        request.endDate(),
        request.gender(),
        request.companionType(),
        request.recruitmentCountType()
    ));

    savePostAgeConditions(post, request.ageConditions());
    savePostTags(post, request.tagIds());
    savePostImages(post, request.imageUrls());

    return post;
  }

  private City getCity(Long countryId, Long cityId) {
    City city = cityRepository.findById(cityId)
        .orElseThrow(() -> new BaseException(PostErrorCode.CITY_NOT_FOUND));
    if (!cityRepository.existsByIdAndCountry_Id(cityId, countryId)) {
      throw new BaseException(PostErrorCode.CITY_NOT_IN_COUNTRY);
    }
    return city;
  }

  private void validateRequiredConditions(CreatePostRequest request) {
    if (request.cityId() == null
        || request.ageConditions() == null
        || request.ageConditions().isEmpty()
        || request.gender() == null
        || request.companionType() == null
        || request.recruitmentCountType() == null) {
      throw new BaseException(GlobalErrorCode.INVALID_REQUEST);
    }
  }

  private void savePostAgeConditions(Post post, List<AgeCondition> ageConditions) {
    Set<AgeCondition> distinctAgeConditions = new LinkedHashSet<>(ageConditions);

    postAgeConditionRepository.saveAll(distinctAgeConditions.stream()
        .map(ageCondition -> new PostAgeCondition(post, ageCondition))
        .toList());
  }

  private void savePostTags(Post post, List<Long> tagIds) {
    if (tagIds == null || tagIds.isEmpty()) {
      return;
    }

    Set<Long> distinctTagIds = new LinkedHashSet<>(tagIds);
    List<Tag> tags = tagRepository.findAllById(distinctTagIds);
    if (tags.size() != distinctTagIds.size()) {
      throw new BaseException(PostErrorCode.TAG_NOT_FOUND);
    }

    postTagRepository.saveAll(tags.stream()
        .map(tag -> new PostTag(post, tag))
        .toList());
  }

  private void savePostImages(Post post, List<String> imageUrls) {
    if (imageUrls == null || imageUrls.isEmpty()) {
      return;
    }

    postImageRepository.saveAll(IntStream.range(0, imageUrls.size())
        .mapToObj(index -> new PostImage(post, imageUrls.get(index).trim(), (short) index))
        .toList());
  }

  private void validateDateRange(CreatePostRequest request) {
    if (request.startDate() == null
        || request.endDate() == null
        || request.startDate().isBefore(LocalDate.now())
        || request.endDate().isBefore(LocalDate.now())
        || request.endDate().isBefore(request.startDate())) {
      throw new BaseException(GlobalErrorCode.INVALID_REQUEST);
    }
  }
}
