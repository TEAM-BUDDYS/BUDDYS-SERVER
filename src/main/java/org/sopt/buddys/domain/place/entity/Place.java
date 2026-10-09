package org.sopt.buddys.domain.place.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.sopt.buddys.domain.location.entity.City;
import org.sopt.buddys.domain.location.entity.Country;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(
    name = "place",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_place_google_place_id", columnNames = "google_place_id")
    }
)
public class Place {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "google_place_id", nullable = false, length = 512)
  private String googlePlaceId;

  @Column(nullable = false, length = 255)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private PlaceCategory category;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "country_id")
  private Country country;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "city_id")
  private City city;

  @Column(length = 512)
  private String address;

  // 구글 표기(한글) 국가/도시명. country/city 마스터 데이터와 연결되지 않는 표시용 문자열이다.
  @Column(name = "country_name", length = 100)
  private String countryName;

  @Column(name = "city_name", length = 100)
  private String cityName;

  @Column(precision = 10, scale = 7)
  private BigDecimal latitude;

  @Column(precision = 10, scale = 7)
  private BigDecimal longitude;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public void updateAddress(String address) {
    this.address = address;
  }

  public void fillMissingLocation(String address, String countryName, String cityName) {
    if (isBlank(this.address) && !isBlank(address)) {
      this.address = address;
    }
    if (isBlank(this.countryName) && !isBlank(countryName)) {
      this.countryName = countryName;
    }
    if (isBlank(this.cityName) && !isBlank(cityName)) {
      this.cityName = cityName;
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
