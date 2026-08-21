package org.sopt.buddys.domain.place.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PlaceCategoryMapperTest {

  @DisplayName("구글 primaryType이 우리 4종 카테고리로 매핑된다")
  @Test
  void resolveCategory_knownPrimaryType_mapsToOurCategories() {
    assertThat(PlaceCategoryMapper.resolveCategory("restaurant", List.of())).contains(PlaceCategory.RESTAURANT);
    assertThat(PlaceCategoryMapper.resolveCategory("cafe", List.of())).contains(PlaceCategory.CAFE);
    assertThat(PlaceCategoryMapper.resolveCategory("museum", List.of())).contains(PlaceCategory.TOURISM);
    assertThat(PlaceCategoryMapper.resolveCategory("lodging", List.of())).contains(PlaceCategory.ACCOMMODATION);
  }

  @DisplayName("primaryType이 매핑 테이블에 없어도 types의 상위 타입으로 매핑된다")
  @Test
  void resolveCategory_unknownPrimaryType_fallsBackToTypes() {
    assertThat(PlaceCategoryMapper.resolveCategory("japanese_restaurant", List.of("japanese_restaurant", "restaurant", "food")))
        .contains(PlaceCategory.RESTAURANT);
  }

  @DisplayName("primaryType과 types 어디에도 매핑되는 타입이 없으면 ETC로 폴백한다")
  @Test
  void resolveCategory_noMatchAnywhere_returnsEtc() {
    assertThat(PlaceCategoryMapper.resolveCategory("parking", List.of("parking", "point_of_interest")))
        .contains(PlaceCategory.ETC);
  }

  @DisplayName("primaryType과 types가 모두 없으면 빈 값을 반환한다")
  @Test
  void resolveCategory_noTypeInfo_returnsEmpty() {
    assertThat(PlaceCategoryMapper.resolveCategory(null, List.of())).isEmpty();
    assertThat(PlaceCategoryMapper.resolveCategory(null, null)).isEmpty();
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
