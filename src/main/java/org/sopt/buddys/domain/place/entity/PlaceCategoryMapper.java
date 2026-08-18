package org.sopt.buddys.domain.place.entity;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class PlaceCategoryMapper {

  private static final Map<String, PlaceCategory> GOOGLE_TYPE_TO_CATEGORY = Map.ofEntries(
      Map.entry("restaurant", PlaceCategory.RESTAURANT),
      Map.entry("fast_food_restaurant", PlaceCategory.RESTAURANT),
      Map.entry("meal_takeaway", PlaceCategory.RESTAURANT),
      Map.entry("meal_delivery", PlaceCategory.RESTAURANT),
      Map.entry("food_court", PlaceCategory.RESTAURANT),
      Map.entry("bar", PlaceCategory.RESTAURANT),
      Map.entry("french_restaurant", PlaceCategory.RESTAURANT),
      Map.entry("cafe", PlaceCategory.CAFE),
      Map.entry("coffee_shop", PlaceCategory.CAFE),
      Map.entry("bakery", PlaceCategory.CAFE),
      Map.entry("pastry_shop", PlaceCategory.CAFE),
      // Culture
      Map.entry("tourist_attraction", PlaceCategory.TOURISM),
      Map.entry("museum", PlaceCategory.TOURISM),
      Map.entry("art_gallery", PlaceCategory.TOURISM),
      Map.entry("art_museum", PlaceCategory.TOURISM),
      Map.entry("cultural_landmark", PlaceCategory.TOURISM),
      Map.entry("castle", PlaceCategory.TOURISM),
      Map.entry("historical_place", PlaceCategory.TOURISM),
      Map.entry("history_museum", PlaceCategory.TOURISM),
      // Entertainment and Recreation
      Map.entry("park", PlaceCategory.TOURISM),
      Map.entry("amusement_park", PlaceCategory.TOURISM),
      Map.entry("zoo", PlaceCategory.TOURISM),
      Map.entry("aquarium", PlaceCategory.TOURISM),
      Map.entry("historical_landmark", PlaceCategory.TOURISM),
      Map.entry("monument", PlaceCategory.TOURISM),
      Map.entry("garden", PlaceCategory.TOURISM),
      Map.entry("plaza", PlaceCategory.TOURISM),
      Map.entry("botanical_garden", PlaceCategory.TOURISM),
      Map.entry("national_park", PlaceCategory.TOURISM),
      Map.entry("state_park", PlaceCategory.TOURISM),
      Map.entry("observation_deck", PlaceCategory.TOURISM),
      Map.entry("visitor_center", PlaceCategory.TOURISM),
      Map.entry("water_park", PlaceCategory.TOURISM),
      Map.entry("wildlife_park", PlaceCategory.TOURISM),
      Map.entry("wildlife_refuge", PlaceCategory.TOURISM),
      Map.entry("marina", PlaceCategory.TOURISM),
      // Places of Worship
      Map.entry("church", PlaceCategory.TOURISM),
      Map.entry("hindu_temple", PlaceCategory.TOURISM),
      Map.entry("mosque", PlaceCategory.TOURISM),
      Map.entry("synagogue", PlaceCategory.TOURISM),
      Map.entry("buddhist_temple", PlaceCategory.TOURISM),
      Map.entry("shinto_shrine", PlaceCategory.TOURISM),
      // Natural Features
      Map.entry("beach", PlaceCategory.TOURISM),
      Map.entry("island", PlaceCategory.TOURISM),
      Map.entry("lake", PlaceCategory.TOURISM),
      Map.entry("mountain_peak", PlaceCategory.TOURISM),
      Map.entry("nature_preserve", PlaceCategory.TOURISM),
      Map.entry("river", PlaceCategory.TOURISM),
      Map.entry("scenic_spot", PlaceCategory.TOURISM),
      Map.entry("woods", PlaceCategory.TOURISM),
      Map.entry("lodging", PlaceCategory.ACCOMMODATION),
      Map.entry("hotel", PlaceCategory.ACCOMMODATION),
      Map.entry("motel", PlaceCategory.ACCOMMODATION),
      Map.entry("hostel", PlaceCategory.ACCOMMODATION),
      Map.entry("resort_hotel", PlaceCategory.ACCOMMODATION),
      Map.entry("guest_house", PlaceCategory.ACCOMMODATION),
      Map.entry("bed_and_breakfast", PlaceCategory.ACCOMMODATION)
  );

  private static final Map<PlaceCategory, List<String>> CATEGORY_TO_GOOGLE_TYPES = GOOGLE_TYPE_TO_CATEGORY.entrySet()
      .stream()
      .filter(entry -> entry.getValue() != PlaceCategory.ETC)
      .collect(Collectors.groupingBy(
          Map.Entry::getValue,
          Collectors.mapping(Map.Entry::getKey, Collectors.toUnmodifiableList())
      ));

  private PlaceCategoryMapper() {
  }

  public static List<String> toGoogleIncludedTypes(PlaceCategory category) {
    return category == null ? List.of() : CATEGORY_TO_GOOGLE_TYPES.getOrDefault(category, List.of());
  }

  public static Optional<PlaceCategory> resolveCategory(String primaryType, List<String> types) {
    if (primaryType == null && (types == null || types.isEmpty())) {
      return Optional.empty();
    }

    PlaceCategory category = primaryType != null ? GOOGLE_TYPE_TO_CATEGORY.get(primaryType) : null;
    if (category == null && types != null) {
      category = types.stream()
          .map(GOOGLE_TYPE_TO_CATEGORY::get)
          .filter(Objects::nonNull)
          .findFirst()
          .orElse(null);
    }
    if (category == null) {
      log.debug("[PlaceCategoryMapper] 매핑 테이블에 없는 구글 타입 → ETC로 폴백: primaryType={}, types={}",
          primaryType, types);
      category = PlaceCategory.ETC;
    }
    return Optional.of(category);
  }
}
