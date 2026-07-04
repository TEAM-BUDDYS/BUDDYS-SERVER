package org.sopt.buddys.domain.tag.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.tag.entity.Tag;
import org.sopt.buddys.domain.tag.entity.TagType;
import org.sopt.buddys.domain.tag.repository.TagRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TagService {
  private final TagRepository tagRepository;

  public List<Tag> getTagsByType(TagType type) {
    return tagRepository.findAllByTagType(type);
  }
}
