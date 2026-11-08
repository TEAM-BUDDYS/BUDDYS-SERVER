package org.sopt.buddys.domain.user.dto.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.course.service.result.SecondSectionCountry;
import org.sopt.buddys.domain.course.service.result.SecondSectionCountryReason;
import org.sopt.buddys.domain.location.dto.response.CountryResponse;
import org.sopt.buddys.domain.location.entity.Country;
import org.sopt.buddys.domain.user.service.result.UserCountriesResult;

class UserCountriesResponseTest {

  @DisplayName("관심 국가와 파견 국가의 ID, 이름, 영문 이름, ISO 코드를 반환한다")
  @Test
  void from_returnsBothCountries() {
    // given
    Country france = createCountry(31L, "프랑스", "France", "FR");
    Country germany = createCountry(71L, "독일", "Germany", "DE");
    UserCountriesResult result = new UserCountriesResult(france, germany, null);

    // when
    UserCountriesResponse response = UserCountriesResponse.from(result);

    // then
    assertThat(response.interestCountry()).isEqualTo(new CountryResponse(31L, "프랑스", "France", "FR"));
    assertThat(response.exchangeCountry()).isEqualTo(new CountryResponse(71L, "독일", "Germany", "DE"));
  }

  @DisplayName("파견 국가가 없으면 파견 국가를 null로 반환한다")
  @Test
  void from_withoutExchangeCountry_returnsNullExchangeCountry() {
    // given
    Country france = createCountry(31L, "프랑스", "France", "FR");
    UserCountriesResult result = new UserCountriesResult(france, null, null);

    // when
    UserCountriesResponse response = UserCountriesResponse.from(result);

    // then
    assertThat(response.interestCountry()).isEqualTo(new CountryResponse(31L, "프랑스", "France", "FR"));
    assertThat(response.exchangeCountry()).isNull();
  }

  @DisplayName("관심 국가가 없으면 관심 국가를 null로 반환한다")
  @Test
  void from_withoutInterestCountry_returnsNullInterestCountry() {
    // given
    UserCountriesResult result = new UserCountriesResult(null, null, null);

    // when
    UserCountriesResponse response = UserCountriesResponse.from(result);

    // then
    assertThat(response.interestCountry()).isNull();
    assertThat(response.exchangeCountry()).isNull();
  }

  @DisplayName("두 번째 섹션 국가를 국가 정보와 선정 이유로 반환하고, 없으면 null로 반환한다")
  @Test
  void from_returnsSecondSectionCountry() {
    // given
    Country france = createCountry(31L, "프랑스", "France", "FR");
    Country spain = createCountry(240L, "스페인", "Spain", "ES");
    UserCountriesResult result = new UserCountriesResult(france, france,
        new SecondSectionCountry(spain, SecondSectionCountryReason.MOST_COURSES));

    // when
    UserCountriesResponse response = UserCountriesResponse.from(result);
    UserCountriesResponse withoutSecond = UserCountriesResponse.from(new UserCountriesResult(france, france, null));

    // then
    assertThat(response.secondSectionCountry()).isEqualTo(new UserCountriesResponse.SecondSectionCountryResponse(
        240L, "스페인", "Spain", "ES", SecondSectionCountryReason.MOST_COURSES));
    assertThat(withoutSecond.secondSectionCountry()).isNull();
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
