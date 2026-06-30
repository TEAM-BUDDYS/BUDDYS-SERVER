package org.sopt.buddys.domain.location.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "country",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_country_name", columnNames = "name")
    }
)
public class Country {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 80)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Continent continent;
}