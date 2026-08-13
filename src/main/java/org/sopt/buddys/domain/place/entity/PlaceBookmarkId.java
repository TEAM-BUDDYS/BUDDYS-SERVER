package org.sopt.buddys.domain.place.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceBookmarkId implements Serializable {

  @Column(name = "user_id")
  private Long userId;

  @Column(name = "place_id")
  private Long placeId;

  public PlaceBookmarkId(Long userId, Long placeId) {
    this.userId = userId;
    this.placeId = placeId;
  }
}