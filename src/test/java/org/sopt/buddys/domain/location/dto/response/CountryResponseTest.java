package org.sopt.buddys.domain.location.dto.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.location.entity.Country;

class CountryResponseTest {

  @DisplayName("국가의 한글명과 영문명을 함께 반환한다")
  @Test
  void from_returnsKoreanAndEnglishName() {
    // given
    Country country = createCountry(1L, "대한민국", "South Korea", "KR");

    // when
    CountryResponse response = CountryResponse.from(country);

    // then
    assertThat(response).isEqualTo(new CountryResponse(1L, "대한민국", "South Korea", "KR"));
  }

  @DisplayName("영문명이 없는 국가는 영문명을 null로 반환한다")
  @Test
  void from_withoutEnglishName_returnsNullEnglishName() {
    // given
    Country country = createCountry(1L, "대한민국", null, "KR");

    // when
    CountryResponse response = CountryResponse.from(country);

    // then
    assertThat(response.name()).isEqualTo("대한민국");
    assertThat(response.englishName()).isNull();
  }

  private Country createCountry(Long id, String name, String englishName, String isoCode) {
    Country country = mock(Country.class);
    given(country.getId()).willReturn(id);
    given(country.getName()).willReturn(name);
    given(country.getEnglishName()).willReturn(englishName);
    given(country.getIsoCode()).willReturn(isoCode);
    return country;
  }
}
