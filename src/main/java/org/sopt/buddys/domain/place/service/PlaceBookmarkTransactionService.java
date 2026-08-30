package org.sopt.buddys.domain.place.service;

import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.place.entity.Place;
import org.sopt.buddys.domain.place.entity.PlaceBookmark;
import org.sopt.buddys.domain.place.entity.PlaceBookmarkId;
import org.sopt.buddys.domain.place.repository.PlaceBookmarkRepository;
import org.sopt.buddys.domain.place.repository.PlaceRepository;
import org.sopt.buddys.domain.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 장소 저장 관련 DB 쓰기만 담당한다. 구글 Places 호출은 트랜잭션 밖({@link PlaceBookmarkService})에서 수행한다.
 */
@Service
@RequiredArgsConstructor
public class PlaceBookmarkTransactionService {

  private final PlaceRepository placeRepository;
  private final PlaceBookmarkRepository placeBookmarkRepository;
  private final UserRepository userRepository;

  @Transactional
  public Place savePlace(Place place) {
    return placeRepository.save(place);
  }

  @Transactional
  public void saveBookmark(Long userId, Long placeId) {
    PlaceBookmarkId id = new PlaceBookmarkId(userId, placeId);
    if (placeBookmarkRepository.existsById(id)) {
      return;
    }
    try {
      placeBookmarkRepository.save(new PlaceBookmark(
          userRepository.getReferenceById(userId),
          placeRepository.getReferenceById(placeId)
      ));
    } catch (DataIntegrityViolationException e) {
      // 동시 요청으로 이미 저장된 경우만 멱등 처리하고, 그 외 무결성 위반은 그대로 전파한다.
      if (!placeBookmarkRepository.existsById(id)) {
        throw e;
      }
    }
  }

  @Transactional
  public void deleteBookmark(Long userId, Long placeId) {
    placeBookmarkRepository.deleteById(new PlaceBookmarkId(userId, placeId));
  }
}
