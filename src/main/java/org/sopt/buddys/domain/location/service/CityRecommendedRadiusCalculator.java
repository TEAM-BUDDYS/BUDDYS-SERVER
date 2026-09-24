package org.sopt.buddys.domain.location.service;

public final class CityRecommendedRadiusCalculator {

  private static final long SMALL_CITY_POPULATION_THRESHOLD = 50_000L;
  private static final long MEDIUM_CITY_POPULATION_THRESHOLD = 200_000L;
  private static final long LARGE_CITY_POPULATION_THRESHOLD = 1_000_000L;
  private static final long MEGA_CITY_POPULATION_THRESHOLD = 5_000_000L;

  private static final int SMALL_CITY_RADIUS_METERS = 5_000;
  private static final int MEDIUM_CITY_RADIUS_METERS = 10_000;
  private static final int LARGE_CITY_RADIUS_METERS = 15_000;
  private static final int VERY_LARGE_CITY_RADIUS_METERS = 25_000;
  private static final int MEGA_CITY_RADIUS_METERS = 40_000;

  private CityRecommendedRadiusCalculator() {
  }

  public static int calculate(long population) {
    if (population < SMALL_CITY_POPULATION_THRESHOLD) {
      return SMALL_CITY_RADIUS_METERS;
    }
    if (population < MEDIUM_CITY_POPULATION_THRESHOLD) {
      return MEDIUM_CITY_RADIUS_METERS;
    }
    if (population < LARGE_CITY_POPULATION_THRESHOLD) {
      return LARGE_CITY_RADIUS_METERS;
    }
    if (population < MEGA_CITY_POPULATION_THRESHOLD) {
      return VERY_LARGE_CITY_RADIUS_METERS;
    }
    return MEGA_CITY_RADIUS_METERS;
  }
}
