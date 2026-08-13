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

  @DisplayName("우리 카테고리는 구글 includedType으로 변환된다")
  @Test
  void toGoogleIncludedType_ourCategory_mapsToGoogleType() {
    assertThat(PlaceCategoryMapper.toGoogleIncludedType(PlaceCategory.RESTAURANT)).isEqualTo("restaurant");
    assertThat(PlaceCategoryMapper.toGoogleIncludedType(PlaceCategory.CAFE)).isEqualTo("cafe");
    assertThat(PlaceCategoryMapper.toGoogleIncludedType(PlaceCategory.TOURISM)).isEqualTo("tourist_attraction");
    assertThat(PlaceCategoryMapper.toGoogleIncludedType(PlaceCategory.ACCOMMODATION)).isEqualTo("lodging");
  }

  @DisplayName("카테고리가 없으면 includedType도 없다")
  @Test
  void toGoogleIncludedType_nullCategory_returnsNull() {
    assertThat(PlaceCategoryMapper.toGoogleIncludedType(null)).isNull();
  }
}