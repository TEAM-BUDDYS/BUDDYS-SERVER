package org.sopt.buddys.domain.course.service;

import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.course.entity.CourseBookmark;
import org.sopt.buddys.domain.course.repository.CourseBookmarkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseBookmarkTransactionService {

  private final CourseBookmarkRepository courseBookmarkRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public CourseBookmark create(CourseBookmark courseBookmark) {
    return courseBookmarkRepository.saveAndFlush(courseBookmark);
  }
}
