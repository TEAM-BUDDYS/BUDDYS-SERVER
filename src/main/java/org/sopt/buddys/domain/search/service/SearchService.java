package org.sopt.buddys.domain.search.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.course.service.CourseService;
import org.sopt.buddys.domain.post.service.PostService;
import org.sopt.buddys.domain.post.service.command.PostSearchCondition;
import org.sopt.buddys.domain.search.service.result.SearchResult;
import org.sopt.buddys.domain.search.service.result.UserSearchResult;
import org.sopt.buddys.domain.search.service.result.UserSearchResult.UserSummaryResult;
import org.sopt.buddys.domain.user.entity.AccountStatus;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

  private final CourseService courseService;
  private final UserRepository userRepository;
  private final PostService postService;

  public SearchResult search(Long userId, String keyword, int page, int size) {
    return new SearchResult(
        courseService.searchCourses(userId, keyword, page, size),
        searchUsers(userId, keyword, page, size),
        postService.getPosts(
            userId,
            new PostSearchCondition(keyword, null, null, null, null, null, null, null),
            page,
            size
        )
    );
  }

  private UserSearchResult searchUsers(Long userId, String keyword, int page, int size) {
    Slice<User> users = userRepository.searchActiveUsersByNickname(
        keyword,
        userId,
        AccountStatus.ACTIVE,
        PageRequest.of(page, size)
    );
    List<UserSummaryResult> content = users.getContent().stream()
        .map(user -> new UserSummaryResult(
            user.getId(),
            user.getNickname(),
            user.getProfileImageUrl()
        ))
        .toList();

    return new UserSearchResult(content, users.getNumber(), users.getSize(), users.hasNext());
  }
}
