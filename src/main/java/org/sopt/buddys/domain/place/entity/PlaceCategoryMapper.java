package org.sopt.buddys.domain.place.entity;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class PlaceCategoryMapper {

  private static final Map<PlaceCategory, String> CATEGORY_TO_GOOGLE_TYPE = Map.of(
      PlaceCategory.RESTAURANT, "restaurant",
      PlaceCategory.CAFE, "cafe",
      PlaceCategory.TOURISM, "tourist_attraction",
      PlaceCategory.ACCOMMODATION, "lodging"
  );

  private static final Map<String, PlaceCategory> GOOGLE_TYPE_TO_CATEGORY = Map.ofEntries(
      Map.entry("restaurant", PlaceCategory.RESTAURANT),
      Map.entry("fast_food_restaurant", PlaceCategory.RESTAURANT),
      Map.entry("meal_takeaway", PlaceCategory.RESTAURANT),
      Map.entry("meal_delivery", PlaceCategory.RESTAURANT),
      Map.entry("food_court", PlaceCategory.RESTAURANT),
      Map.entry("bar", PlaceCategory.RESTAURANT),
      Map.entry("cafe", PlaceCategory.CAFE),
      Map.entry("coffee_shop", PlaceCategory.CAFE),
      Map.entry("bakery", PlaceCategory.CAFE),
      Map.entry("tourist_attraction", PlaceCategory.TOURISM),
      Map.entry("museum", PlaceCategory.TOURISM),
      Map.entry("art_gallery", PlaceCategory.TOURISM),
      Map.entry("park", PlaceCategory.TOURISM),
      Map.entry("amusement_park", PlaceCategory.TOURISM),
      Map.entry("zoo", PlaceCategory.TOURISM),
      Map.entry("aquarium", PlaceCategory.TOURISM),
      Map.entry("historical_landmark", PlaceCategory.TOURISM),
      Map.entry("monument", PlaceCategory.TOURISM),
      Map.entry("church", PlaceCategory.TOURISM),
      Map.entry("hindu_temple", PlaceCategory.TOURISM),
      Map.entry("mosque", PlaceCategory.TOURISM),
      Map.entry("synagogue", PlaceCategory.TOURISM),
      Map.entry("lodging", PlaceCategory.ACCOMMODATION),
      Map.entry("hotel", PlaceCategory.ACCOMMODATION),
      Map.entry("motel", PlaceCategory.ACCOMMODATION),
      Map.entry("hostel", PlaceCategory.ACCOMMODATION),
      Map.entry("resort_hotel", PlaceCategory.ACCOMMODATION),
      Map.entry("guest_house", PlaceCategory.ACCOMMODATION),
      Map.entry("bed_and_breakfast", PlaceCategory.ACCOMMODATION)
  );

  private PlaceCategoryMapper() {
  }

  public static String toGoogleIncludedType(PlaceCategory category) {
    return category == null ? null : CATEGORY_TO_GOOGLE_TYPE.get(category);
  }

  public static Optional<PlaceCategory> fromGooglePrimaryType(String primaryType) {
    return primaryType == null ? Optional.empty() : Optional.ofNullable(GOOGLE_TYPE_TO_CATEGORY.get(primaryType));
  }
}