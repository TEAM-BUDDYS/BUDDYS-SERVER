package org.sopt.buddys.domain.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.List;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sopt.buddys.domain.course.service.CourseService;
import org.sopt.buddys.domain.course.service.result.CourseListResult;
import org.sopt.buddys.domain.course.service.result.CourseSearchResult;
import org.sopt.buddys.domain.post.service.PostService;
import org.sopt.buddys.domain.post.service.command.PostSearchCondition;
import org.sopt.buddys.domain.post.service.result.PostListResult;
import org.sopt.buddys.domain.search.service.command.SearchSort;
import org.sopt.buddys.domain.search.service.command.SearchType;
import org.sopt.buddys.domain.user.entity.AccountStatus;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

  @InjectMocks private SearchService searchService;
  @Mock private CourseService courseService;
  @Mock private UserRepository userRepository;
  @Mock private PostService postService;

  @ParameterizedTest(name = "type={0}, page={1}: 선택 영역만 조회하고 첫 페이지에서만 사용자 count한다")
  @CsvSource({
      ",0,LATEST", ",2,LATEST", "POST,0,LATEST", "POST,2,LATEST",
      "COURSE,0,LATEST", "COURSE,2,LATEST", "USER,0,LATEST", "USER,2,LATEST",
      ",0,BOOKMARK", ",2,BOOKMARK", "POST,0,BOOKMARK", "POST,2,BOOKMARK",
      "COURSE,0,BOOKMARK", "COURSE,2,BOOKMARK", "USER,0,BOOKMARK", "USER,2,BOOKMARK"
  })
  void search_queriesOnlySelectedSections(SearchType type, int page, SearchSort sort) {
    var pageable = PageRequest.of(page, 3);
    var condition = PostSearchCondition.keywordOnly("Paris");
    assertThat(condition).isEqualTo(
        new PostSearchCondition("Paris", null, null, null, null, null, null, null, null));
    var courses = new CourseSearchResult(new CourseListResult(List.of(), page, 3, false), page == 0 ? 8L : null);
    var posts = new PostListResult(List.of(), page, 3, false, 7);
    if (type == null || type == SearchType.COURSE) {
      given(courseService.searchCourses(1L, "Paris", page, 3, sort)).willReturn(courses);
    }
    if (type == null || type == SearchType.POST) {
      given(postService.getPosts(1L, condition, page, 3, sort)).willReturn(posts);
    }
    if (type == null || type == SearchType.USER) {
      given(userRepository.searchActiveUsersByNickname("Paris", 1L, AccountStatus.ACTIVE, pageable))
          .willReturn(new SliceImpl<>(List.of(), pageable, false));
      if (page == 0) {
        given(userRepository.countActiveUsersByNickname("Paris", 1L, AccountStatus.ACTIVE))
            .willReturn(6L);
      }
    }

    var result = searchService.search(1L, "Paris", page, 3, type, sort);

    if (type == null || type == SearchType.COURSE) {
      assertThat(result.courses()).isSameAs(courses);
      verify(courseService).searchCourses(1L, "Paris", page, 3, sort);
    } else {
      assertThat(result.courses()).isNull();
      verifyNoInteractions(courseService);
    }
    if (type == null || type == SearchType.POST) {
      assertThat(result.posts()).isSameAs(posts);
      verify(postService).getPosts(1L, condition, page, 3, sort);
    } else {
      assertThat(result.posts()).isNull();
      verifyNoInteractions(postService);
    }
    if (type == null || type == SearchType.USER) {
      assertThat(result.users().totalElements()).isEqualTo(page == 0 ? 6L : null);
      assertThat(result.users().page()).isEqualTo(page);
      assertThat(result.users().size()).isEqualTo(3);
      verify(userRepository).searchActiveUsersByNickname("Paris", 1L, AccountStatus.ACTIVE, pageable);
      if (page == 0) {
        verify(userRepository).countActiveUsersByNickname("Paris", 1L, AccountStatus.ACTIVE);
      }
    } else {
      assertThat(result.users()).isNull();
      verifyNoInteractions(userRepository);
    }
    verifyNoMoreInteractions(courseService, postService, userRepository);
  }
}
