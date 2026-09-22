package org.sopt.buddys.domain.magazine.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MagazineListRequestTest {

  @DisplayName("유니코드 공백만 있는 검색어는 검색 조건을 적용하지 않는다")
  @Test
  void normalizedKeyword_unicodeWhitespaceOnly_returnsNull() {
    MagazineListRequest request = createRequest("\u2003");

    assertThat(request.normalizedKeyword()).isNull();
  }

  @DisplayName("검색어 앞뒤의 유니코드 공백을 제거한다")
  @Test
  void normalizedKeyword_unicodeWhitespaceAroundKeyword_returnsKeyword() {
    MagazineListRequest request = createRequest("\u2003교환학생\u2003");

    assertThat(request.normalizedKeyword()).isEqualTo("교환학생");
  }

  @DisplayName("검색어 앞뒤의 일반 공백도 기존처럼 제거한다")
  @Test
  void normalizedKeyword_asciiWhitespaceAroundKeyword_returnsKeyword() {
    MagazineListRequest request = createRequest("  교환학생  ");

    assertThat(request.normalizedKeyword()).isEqualTo("교환학생");
  }

  private MagazineListRequest createRequest(String keyword) {
    return new MagazineListRequest(null, keyword, null, null, null);
  }
}
