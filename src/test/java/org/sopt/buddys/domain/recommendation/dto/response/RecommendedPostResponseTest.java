package org.sopt.buddys.domain.recommendation.dto.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.location.entity.Country;
import org.sopt.buddys.domain.post.entity.Post;
import org.sopt.buddys.domain.recommendation.service.result.RecommendedPostResult;
import org.sopt.buddys.domain.user.entity.User;

class RecommendedPostResponseTest {

  @DisplayName("추천 게시글 응답에 작성자 정보, 국가, 조회수를 포함한다")
  @Test
  void from_includesAuthorProfileCountryAndViewCount() {
    // given
    User author = mock(User.class);
    Country country = mock(Country.class);
    Post post = mock(Post.class);

    given(author.getId()).willReturn(100L);
    given(author.getNickname()).willReturn("가윤");
    given(author.getProfileImageUrl()).willReturn("https://example.com/profile.png");
    given(country.getId()).willReturn(1L);
    given(country.getName()).willReturn("France");
    given(post.getId()).willReturn(10L);
    given(post.getTitle()).willReturn("같이 유럽 여행 가실 분!");
    given(post.getContent()).willReturn("본문");
    given(post.getStartDate()).willReturn(LocalDate.of(2027, 3, 1));
    given(post.getEndDate()).willReturn(LocalDate.of(2027, 8, 31));
    given(post.getAuthor()).willReturn(author);
    given(post.getCountry()).willReturn(country);
    given(post.getViewCount()).willReturn(25L);

    RecommendedPostResult result = new RecommendedPostResult(
        post,
        1.0,
        1.0,
        "https://example.com/thumbnail.png"
    );

    // when
    RecommendedPostResponse response = RecommendedPostResponse.from(result);

    // then
    assertThat(response.author().userId()).isEqualTo(100L);
    assertThat(response.author().nickname()).isEqualTo("가윤");
    assertThat(response.author().profileImageUrl()).isEqualTo("https://example.com/profile.png");
    assertThat(response.country().countryId()).isEqualTo(1L);
    assertThat(response.country().name()).isEqualTo("France");
    assertThat(response.viewCount()).isEqualTo(25L);
  }
}
