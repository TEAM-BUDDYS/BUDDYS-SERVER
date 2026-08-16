package org.sopt.buddys.domain.place.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PlaceCategoryMapperTest {

  @DisplayName("구글 primaryType이 우리 4종 카테고리로 매핑된다")
  @Test
  void fromGooglePrimaryType_knownTypes_mapToOurCategories() {
    assertThat(PlaceCategoryMapper.fromGooglePrimaryType("restaurant")).contains(PlaceCategory.RESTAURANT);
    assertThat(PlaceCategoryMapper.fromGooglePrimaryType("cafe")).contains(PlaceCategory.CAFE);
    assertThat(PlaceCategoryMapper.fromGooglePrimaryType("museum")).contains(PlaceCategory.TOURISM);
    assertThat(PlaceCategoryMapper.fromGooglePrimaryType("lodging")).contains(PlaceCategory.ACCOMMODATION);
  }

  @DisplayName("매핑 테이블에 없는 타입은 ETC로 폴백한다")
  @Test
  void fromGooglePrimaryType_unknownType_returnsEtc() {
    assertThat(PlaceCategoryMapper.fromGooglePrimaryType("parking")).contains(PlaceCategory.ETC);
  }

  @DisplayName("primaryType 자체가 없으면 빈 값을 반환한다")
  @Test
  void fromGooglePrimaryType_nullType_returnsEmpty() {
    assertThat(PlaceCategoryMapper.fromGooglePrimaryType(null)).isEmpty();
  }

  @DisplayName("우리 카테고리는 매핑된 모든 구글 타입 목록으로 변환된다")
  @Test
  void toGoogleIncludedTypes_ourCategory_mapsToAllGoogleTypes() {
    assertThat(PlaceCategoryMapper.toGoogleIncludedTypes(PlaceCategory.RESTAURANT))
        .contains("restaurant", "fast_food_restaurant", "bar");
    assertThat(PlaceCategoryMapper.toGoogleIncludedTypes(PlaceCategory.CAFE))
        .contains("cafe", "coffee_shop", "bakery");
    assertThat(PlaceCategoryMapper.toGoogleIncludedTypes(PlaceCategory.TOURISM))
        .contains("tourist_attraction", "museum", "beach");
    assertThat(PlaceCategoryMapper.toGoogleIncludedTypes(PlaceCategory.ACCOMMODATION))
        .contains("lodging", "hotel", "hostel");
  }

  @DisplayName("ETC 카테고리나 카테고리가 없으면 매핑된 구글 타입이 없다")
  @Test
  void toGoogleIncludedTypes_etcOrNullCategory_returnsEmpty() {
    assertThat(PlaceCategoryMapper.toGoogleIncludedTypes(PlaceCategory.ETC)).isEmpty();
    assertThat(PlaceCategoryMapper.toGoogleIncludedTypes(null)).isEmpty();
  }
}
