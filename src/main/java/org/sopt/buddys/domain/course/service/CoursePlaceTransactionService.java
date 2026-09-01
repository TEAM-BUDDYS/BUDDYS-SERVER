package org.sopt.buddys.domain.course.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.place.entity.Place;
import org.sopt.buddys.domain.place.repository.PlaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 코스 생성/수정 중 신규 장소 캐시 행을 별도 트랜잭션으로 저장한다.
 * 동시 요청이 같은 google_place_id를 저장해 unique 제약에 걸리더라도, 실패가 이 트랜잭션 안에서만
 * 롤백되어 바깥 코스 트랜잭션이 오염되지 않도록 한다. 제약 위반 후 승자 행을 다시 읽을 때도
 * 새 트랜잭션(=최신 스냅샷)으로 조회해야 REPEATABLE READ에서도 커밋된 행이 보인다.
 */
@Service
@RequiredArgsConstructor
public class CoursePlaceTransactionService {

  private final PlaceRepository placeRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public Place save(Place place) {
    return placeRepository.saveAndFlush(place);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public Optional<Place> findByGooglePlaceId(String googlePlaceId) {
    return placeRepository.findByGooglePlaceId(googlePlaceId);
  }
}
