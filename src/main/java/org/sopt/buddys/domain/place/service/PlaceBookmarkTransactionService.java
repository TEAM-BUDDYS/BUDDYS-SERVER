package org.sopt.buddys.domain.place.service;

import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.place.entity.Place;
import org.sopt.buddys.domain.place.entity.PlaceBookmarkId;
import org.sopt.buddys.domain.place.repository.PlaceBookmarkRepository;
import org.sopt.buddys.domain.place.repository.PlaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaceBookmarkTransactionService {

  private final PlaceRepository placeRepository;
  private final PlaceBookmarkRepository placeBookmarkRepository;

  @Transactional
  public Place savePlace(Place place) {
    return placeRepository.save(place);
  }

  @Transactional
  public void saveBookmark(Long userId, Long placeId) {
    placeBookmarkRepository.insertOrKeep(userId, placeId);
  }

  @Transactional
  public void deleteBookmark(Long userId, Long placeId) {
    placeBookmarkRepository.deleteById(new PlaceBookmarkId(userId, placeId));
  }
}
