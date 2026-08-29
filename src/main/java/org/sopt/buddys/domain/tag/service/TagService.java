package org.sopt.buddys.domain.tag.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.tag.dto.response.TagGroupListResponse;
import org.sopt.buddys.domain.tag.entity.Tag;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.domain.tag.repository.TagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagService {
  private final TagRepository tagRepository;

  public List<Tag> getTagsByType(TagType type) {
    return tagRepository.findAllByTagTypeOrderByIdAsc(type);
  }

  public List<TagGroupListResponse> getAllTagGroups() {
    Map<TagType, List<Tag>> tagsByType = tagRepository.findAllByOrderByIdAsc().stream()
        .collect(Collectors.groupingBy(Tag::getTagType));
    return Stream.of(TagType.ACTIVITY, TagType.INTEREST, TagType.TRAVEL_STYLE)
        .map(type -> TagGroupListResponse.of(type, tagsByType.getOrDefault(type, List.of())))
        .toList();
  }
}
