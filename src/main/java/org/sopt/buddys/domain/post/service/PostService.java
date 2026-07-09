package org.sopt.buddys.domain.post.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.Comparator;
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
import org.sopt.buddys.domain.post.entity.AgeCondition;
import org.sopt.buddys.domain.post.entity.GenderCondition;
import org.sopt.buddys.domain.post.entity.Post;
import org.sopt.buddys.domain.post.entity.PostAgeCondition;
import org.sopt.buddys.domain.post.entity.PostGenderCondition;
import org.sopt.buddys.domain.post.entity.PostImage;
import org.sopt.buddys.domain.post.entity.PostStatus;
import org.sopt.buddys.domain.post.entity.PostTag;
import org.sopt.buddys.domain.post.repository.PostAgeConditionRepository;
import org.sopt.buddys.domain.post.repository.PostGenderConditionRepository;
import org.sopt.buddys.domain.post.repository.PostImageRepository;
import org.sopt.buddys.domain.post.repository.PostRepository;
import org.sopt.buddys.domain.post.repository.PostTagRepository;
import org.sopt.buddys.domain.post.service.command.CreatePostCommand;
import org.sopt.buddys.domain.post.service.result.PostDetailResult;
import org.sopt.buddys.domain.tag.entity.Tag;
import org.sopt.buddys.domain.tag.entity.TagType;
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

  private static final int MAX_ACTIVITY_TAG_COUNT = 3;
  private static final int MAX_INTEREST_TAG_COUNT = 2;
  private static final int MAX_TRAVEL_STYLE_TAG_COUNT = 2;

  private final PostRepository postRepository;
  private final PostAgeConditionRepository postAgeConditionRepository;
  private final PostGenderConditionRepository postGenderConditionRepository;
  private final PostTagRepository postTagRepository;
  private final PostImageRepository postImageRepository;
  private final UserRepository userRepository;
  private final CountryRepository countryRepository;
  private final CityRepository cityRepository;
  private final TagRepository tagRepository;

  @Transactional
  public Post createPost(Long userId, CreatePostCommand command) {
    validateRequiredConditions(command);
    validateDateRange(command);

    User author = userRepository.findByIdAndDeletedAtIsNull(userId)
        .orElseThrow(() -> new BaseException(UserErrorCode.USER_NOT_FOUND));
    Country country = countryRepository.findById(command.countryId())
        .orElseThrow(() -> new BaseException(LocationErrorCode.COUNTRY_NOT_FOUND));
    City city = getCity(command.countryId(), command.cityId());

    Post post = postRepository.save(new Post(
        author,
        country,
        city,
        command.title().trim(),
        command.content().trim(),
        command.startDate(),
        command.endDate(),
        command.companionType(),
        command.recruitmentCountType()
    ));

    savePostAgeConditions(post, command.ageConditions());
    savePostGenderConditions(post, command.genderConditions());
    savePostTags(post, command.tagIds());
    savePostImages(post, command.imageUrls());

    return post;
  }

  @Transactional
  public PostDetailResult getPostDetail(Long userId, Long postId) {
    Post post = postRepository.findDetailById(postId)
        .orElseThrow(() -> new BaseException(PostErrorCode.POST_NOT_FOUND));

    post.increaseViewCount();

    return toPostDetailResult(userId, post);
  }

  @Transactional
  public Post updatePostStatus(Long userId, Long postId, PostStatus status) {
    if (status == null) {
      throw new BaseException(GlobalErrorCode.INVALID_REQUEST);
    }

    Post post = postRepository.findById(postId)
        .orElseThrow(() -> new BaseException(PostErrorCode.POST_NOT_FOUND));

    if (!post.getAuthor().getId().equals(userId)) {
      throw new BaseException(GlobalErrorCode.FORBIDDEN);
    }

    post.updateStatus(status);
    return post;
  }

  private PostDetailResult toPostDetailResult(Long userId, Post post) {
    return new PostDetailResult(
        post.getId(),
        toAuthorResult(post.getAuthor()),
        post.getAuthor().getId().equals(userId),
        post.getStatus(),
        post.getTitle(),
        postImageRepository.findImageUrlsByPostId(post.getId()),
        toCountryResult(post.getCountry()),
        toCityResult(post.getCity()),
        post.getStartDate(),
        post.getEndDate(),
        post.getRecruitmentCountType(),
        post.getContent(),
        getAgeConditions(post.getId()),
        getGenderConditions(post.getId()),
        post.getCompanionType(),
        getTagResults(post.getId()),
        post.getViewCount(),
        post.getCommentCount(),
        post.getCreatedAt()
    );
  }

  private PostDetailResult.AuthorResult toAuthorResult(User author) {
    Country country = author.getExchangeCountry();
    return new PostDetailResult.AuthorResult(
        author.getId(),
        author.getNickname(),
        author.getProfileImageUrl(),
        country == null ? null : country.getName(),
        toAge(author.getBirthDate()),
        toAgeRange(author.getBirthDate()),
        author.getGender()
    );
  }

  private PostDetailResult.CityResult toCityResult(City city) {
    return new PostDetailResult.CityResult(
        city.getId(),
        city.getName(),
        city.getKoreanName()
    );
  }

  private PostDetailResult.CountryResult toCountryResult(Country country) {
    return new PostDetailResult.CountryResult(
        country.getId(),
        country.getName()
    );
  }

  private List<AgeCondition> getAgeConditions(Long postId) {
    return postAgeConditionRepository.findAgeConditionsByPostId(postId)
        .stream()
        .sorted(Comparator.comparingInt(Enum::ordinal))
        .toList();
  }

  private List<GenderCondition> getGenderConditions(Long postId) {
    return postGenderConditionRepository.findGenderConditionsByPostId(postId)
        .stream()
        .sorted(Comparator.comparingInt(Enum::ordinal))
        .toList();
  }

  private List<PostDetailResult.TagResult> getTagResults(Long postId) {
    return postTagRepository.findAllByPostIdWithTag(postId)
        .stream()
        .map(postTag -> new PostDetailResult.TagResult(
            postTag.getTag().getId(),
            postTag.getTag().getName(),
            postTag.getTag().getTagType()
        ))
        .toList();
  }

  private String toAgeRange(LocalDate birthDate) {
    Integer age = toAge(birthDate);
    if (age == null) {
      return null;
    }
    if (age < 10) {
      return "10대 미만";
    }
    return "%d0대".formatted(age / 10);
  }

  private Integer toAge(LocalDate birthDate) {
    if (birthDate == null) {
      return null;
    }
    return Period.between(birthDate, LocalDate.now()).getYears();
  }

  private City getCity(Long countryId, Long cityId) {
    City city = cityRepository.findById(cityId)
        .orElseThrow(() -> new BaseException(LocationErrorCode.CITY_NOT_FOUND));
    if (!city.getCountry().getId().equals(countryId)) {
      throw new BaseException(PostErrorCode.CITY_NOT_IN_COUNTRY);
    }
    return city;
  }

  private void validateRequiredConditions(CreatePostCommand command) {
    if (command.countryId() == null
        || command.cityId() == null
        || command.startDate() == null
        || command.endDate() == null
        || command.title() == null
        || command.title().isBlank()
        || command.content() == null
        || command.content().isBlank()
        || command.ageConditions() == null
        || command.ageConditions().isEmpty()
        || command.genderConditions() == null
        || command.genderConditions().isEmpty()
        || command.companionType() == null
        || command.recruitmentCountType() == null
        || command.tagIds() == null
        || command.tagIds().isEmpty()) {
      throw new BaseException(GlobalErrorCode.INVALID_REQUEST);
    }
  }

  private void savePostAgeConditions(Post post, List<AgeCondition> ageConditions) {
    Set<AgeCondition> distinctAgeConditions = new LinkedHashSet<>(ageConditions);

    postAgeConditionRepository.saveAll(distinctAgeConditions.stream()
        .map(ageCondition -> new PostAgeCondition(post, ageCondition))
        .toList());
  }

  private void savePostGenderConditions(Post post, List<GenderCondition> genderConditions) {
    Set<GenderCondition> distinctGenderConditions = new LinkedHashSet<>(genderConditions);

    postGenderConditionRepository.saveAll(distinctGenderConditions.stream()
        .map(genderCondition -> new PostGenderCondition(post, genderCondition))
        .toList());
  }

  private void savePostTags(Post post, List<Long> tagIds) {
    if (tagIds == null || tagIds.isEmpty()) {
      throw new BaseException(PostErrorCode.ACTIVITY_TAG_REQUIRED);
    }

    Set<Long> distinctTagIds = new LinkedHashSet<>(tagIds);
    List<Tag> tags = tagRepository.findAllById(distinctTagIds);
    if (tags.size() != distinctTagIds.size()) {
      throw new BaseException(PostErrorCode.TAG_NOT_FOUND);
    }
    validateTagTypeCounts(tags);

    postTagRepository.saveAll(tags.stream()
        .map(tag -> new PostTag(post, tag))
        .toList());
  }

  private void validateTagTypeCounts(List<Tag> tags) {
    long activityTagCount = countTagsByType(tags, TagType.ACTIVITY);
    if (activityTagCount == 0) {
      throw new BaseException(PostErrorCode.ACTIVITY_TAG_REQUIRED);
    }
    if (activityTagCount > MAX_ACTIVITY_TAG_COUNT
        || countTagsByType(tags, TagType.INTEREST) > MAX_INTEREST_TAG_COUNT
        || countTagsByType(tags, TagType.TRAVEL_STYLE) > MAX_TRAVEL_STYLE_TAG_COUNT) {
      throw new BaseException(PostErrorCode.TAG_LIMIT_EXCEEDED);
    }
  }

  private long countTagsByType(List<Tag> tags, TagType tagType) {
    return tags.stream()
        .filter(tag -> tag.getTagType() == tagType)
        .count();
  }

  private void savePostImages(Post post, List<String> imageUrls) {
    if (imageUrls == null || imageUrls.isEmpty()) {
      return;
    }

    postImageRepository.saveAll(IntStream.range(0, imageUrls.size())
        .mapToObj(index -> new PostImage(post, imageUrls.get(index).trim(), (short) index))
        .toList());
  }

  private void validateDateRange(CreatePostCommand command) {
    if (command.startDate().isBefore(LocalDate.now())
        || command.endDate().isBefore(LocalDate.now())
        || command.endDate().isBefore(command.startDate())) {
      throw new BaseException(GlobalErrorCode.INVALID_REQUEST);
    }
  }
}
