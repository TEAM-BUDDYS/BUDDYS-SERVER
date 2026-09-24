package org.sopt.buddys.domain.location.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CityRecommendedRadiusCalculatorTest {

  @DisplayName("인구 5만 미만이면 5km를 반환한다")
  @Test
  void calculate_smallCity_returns5km() {
    assertThat(CityRecommendedRadiusCalculator.calculate(0L)).isEqualTo(5_000);
    assertThat(CityRecommendedRadiusCalculator.calculate(49_999L)).isEqualTo(5_000);
  }

  @DisplayName("인구 5만 이상 20만 미만이면 10km를 반환한다")
  @Test
  void calculate_mediumCity_returns10km() {
    assertThat(CityRecommendedRadiusCalculator.calculate(50_000L)).isEqualTo(10_000);
    assertThat(CityRecommendedRadiusCalculator.calculate(199_999L)).isEqualTo(10_000);
  }

  @DisplayName("인구 20만 이상 100만 미만이면 15km를 반환한다")
  @Test
  void calculate_largeCity_returns15km() {
    assertThat(CityRecommendedRadiusCalculator.calculate(200_000L)).isEqualTo(15_000);
    assertThat(CityRecommendedRadiusCalculator.calculate(999_999L)).isEqualTo(15_000);
  }

  @DisplayName("인구 100만 이상 500만 미만이면 25km를 반환한다")
  @Test
  void calculate_veryLargeCity_returns25km() {
    assertThat(CityRecommendedRadiusCalculator.calculate(1_000_000L)).isEqualTo(25_000);
    assertThat(CityRecommendedRadiusCalculator.calculate(4_999_999L)).isEqualTo(25_000);
  }

  @DisplayName("인구 500만 이상이면 40km를 반환한다")
  @Test
  void calculate_megaCity_returns40km() {
    assertThat(CityRecommendedRadiusCalculator.calculate(5_000_000L)).isEqualTo(40_000);
    assertThat(CityRecommendedRadiusCalculator.calculate(30_000_000L)).isEqualTo(40_000);
  }
}
