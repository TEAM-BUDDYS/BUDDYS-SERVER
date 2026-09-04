package org.sopt.buddys.domain.place.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.sopt.buddys.domain.place.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, Long> {

  Optional<Place> findByGooglePlaceId(String googlePlaceId);

  List<Place> findByGooglePlaceIdIn(Collection<String> googlePlaceIds);
}
