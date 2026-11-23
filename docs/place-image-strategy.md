# 장소 이미지 비용 절감 방안

> 작성일: 2026-11-17
> 대상: 장소(Place) 사진 노출 기능 / Google Places Photo API 비용

## 1. 배경

- 장소 사진은 서버의 사진 프록시를 거쳐서 내려가요.
- 요청이 올 때마다 서버가 Google을 **두 번** 불러요.
  - `GetPlace`: 사진 이름을 조회해요.
  - `GetPhotoMedia`: 실제 이미지를 가져와요.
- 사진 결과를 저장해 두는 곳(캐시)이 없어요. 그래서 같은 사진도 볼 때마다 매번 과금돼요.
- 그 결과 GCP 비용이 **월 약 ₩52,559** 나왔어요.

해결하는 방법은 크게 두 가지예요.

1. **Google 호출 횟수를 줄이기**: 방안 A, B
2. **Google 대신 다른 이미지 소스 쓰기**: 방안 C

---

## 2. 방안 A — Google 유지 + 캐시 / 호출 한도

| 항목 | 내용 |
|---|---|
| 서버 캐시 | `placeId → photoUri`를 Valkey에 TTL을 두고 저장해요. 같은 장소는 Google을 다시 부르지 않아요. 이미 만들어 둔 작업이 `stash@{0}`에 있어요(`PlacePhotoUriCacheRepository`와 테스트). |
| 클라이언트 캐시 | 사진 응답에 `Cache-Control: public, max-age=...`를 붙여요. 앱과 CDN이 같은 이미지를 다시 요청하지 않게 돼요. |
| 비용 폭주 방지 | GCP에서 API 키를 서버 IP로 제한하고, `GetPhotoMedia`에 일일 할당량을 걸어요. 예산 알림도 설정해요. |
| 사진 크기 | `maxWidthPx`와 `maxHeightPx`를 카드 크기에 맞게 줄여서 요청해요. |

- **화면에 보이는 사진:** 지금과 똑같아요. Google 지도에 나오는 그 장소의 실제 사진이에요.
- **장점:**
  - 품질과 커버리지가 그대로예요.
  - 작업량이 가장 적어요. stash를 다시 적용하면 돼요.
- **단점:**
  - 처음 보는 장소는 여전히 과금돼요.
  - Google 약관상 사진 원본은 오래 저장할 수 없어요. 그래서 URI만 짧은 TTL로 캐시해야 해요.

## 3. 방안 B — 사진 소스 우선순위

```
1순위: 사용자가 코스(day)에 올린 이미지 (S3, 비용 0)
2순위: Google Places 사진 (방안 A의 캐시 적용)
3순위: 기본 이미지 (국가/카테고리별 placeholder)
```

- **화면에 보이는 사진:**
  - 코스가 많은 장소는 **사용자가 직접 찍은 사진**이 보여요.
  - 코스가 없는 장소만 Google 사진이 보여요.
  - Google 사진도 없으면 기본 이미지가 보여요.
- **장점:**
  - 인기 장소일수록 Google 호출이 0에 가까워져요.
  - 앱의 성격에 맞는 사진이 보여요. 교환학생이 실제로 다녀온 사진이에요.
- **단점:**
  - 코스 이미지와 장소를 연결하는 기준이 필요해요. 예를 들어 day에 포함된 장소의 대표 이미지를 정해야 해요.
  - 사용자 사진은 품질이 제각각이에요.
  - 삭제된 코스나 탈퇴한 사용자의 이미지를 어떻게 처리할지 정해야 해요.

## 4. 방안 C — 대체 API

### 4-1. 장소의 실제 사진을 주는 API

| API | 비용 / 무료 한도 | 화면에 보이는 이미지 | 주의 |
|---|---|---|---|
| **Foursquare Places API** | 샌드박스 크레딧 $200을 쓴 뒤 사용량만큼 과금해요. 사진은 Premium 등급이에요. 1천 건당 가격은 출처마다 달라서 확인이 필요해요. | 사용자들이 Foursquare/Swarm에 체크인하며 올린 사진이에요. 음식, 매장 내부, 간판 사진이 많고 일상적인 폰 사진 느낌이에요. | 유럽 소도시나 작은 가게는 사진이 없는 경우가 있어요. Google placeId와 바로 연결되지 않아서 이름과 좌표로 매칭해야 해요. |
| **Tripadvisor Content API** | 월 5,000건 무료로 알려져 있어요(카드 등록 필요). 2026-08에 기존 API가 종료되고 새 API로 바뀐다는 글이 있는데 확인되지 않았어요. | 장소당 최대 5장이에요. 여행자 리뷰 사진이라 관광지 전경, 랜드마크, 레스토랑 음식 사진 위주예요. | 관광지와 식당 위주예요. 화면에 Tripadvisor 로고와 링크를 표시해야 해요. |
| **Wikimedia Commons / Wikidata (P18)** | 무료 | 백과사전용 대표 사진이에요. 에펠탑 정면이나 성당 외관처럼 정제된 랜드마크 사진이에요. | 유명 랜드마크만 있어요. 식당이나 카페는 거의 없어요. CC 라이선스 표시가 필요해요. Google 장소를 Wikidata 항목에 연결하는 매칭이 필요해요. |

### 4-2. 분위기 이미지(스톡)만 주는 API

| API | 비용 / 무료 한도 | 화면에 보이는 이미지 | 주의 |
|---|---|---|---|
| **Unsplash API** | 무료. 시간당 50건으로 시작하고, 심사 후 시간당 5,000건이에요. | 검색어("Paris cafe" 등)로 고른 **고퀄리티 감성 사진**이에요. 그 장소가 아니라 **비슷한 분위기의 다른 곳**일 수 있어요. | 핫링크(이미지 URL 그대로 사용)가 필수이고, 작가와 Unsplash 표시가 필요해요. |
| **Pexels API** | 무료. 시간당 약 200건, 월 약 2만 건이에요(확인 필요). | Unsplash와 비슷한 스톡 사진이에요. | 출처 표시를 권장해요. |
| **Pixabay API** | 무료 | 스톡 사진과 일러스트예요. 조금 더 "스톡 사진 같은" 느낌이에요. | 영구 핫링크가 안 돼서 서버에 저장해야 해요(확인 필요). |

> 스톡 API를 장소 카드에 쓰면 "루브르 박물관" 카드에 **다른 박물관 사진**이 뜰 수 있어요. 사용자가 오해할 수 있어서 장소 사진 대신 쓰기에는 비추천이에요. **국가, 도시 섹션의 배너 이미지**로는 적합해요.

### 4-3. 쓰기 어려운 API

- **Naver 이미지 검색 API:** 검색 결과 이미지라 저작권이 불분명해요. 앱에 노출할 권리가 없어요.
- **Kakao Local API:** 장소 사진을 제공하지 않아요. 해외 커버리지도 약해요.

### 4-4. 같은 장소("파리 생제르맹 거리의 작은 카페")일 때 화면 예시

| 소스 | 보이는 이미지 |
|---|---|
| Google (현재) | 그 카페의 외관과 내부, 메뉴 사진 (사용자와 업주가 올린 사진) |
| Foursquare | 사진이 있다면 그 카페의 커피와 디저트 근접샷. 없으면 빈 이미지 |
| Tripadvisor | 리뷰가 있다면 그 카페 음식 사진. 작은 카페는 없을 가능성이 높아요 |
| Wikimedia | 없음 (랜드마크가 아니라서) |
| Unsplash | "Paris cafe"로 검색한 **다른 파리 카페**의 감성 사진 |
| 사용자 업로드 (방안 B) | 그 코스를 만든 교환학생이 직접 찍은 사진 |

---

## 5. 방안 D — 캐시 + 호출 한도 차단 + 대체 이미지 (A+B+스톡 조합)

> 캐시를 적용하고, Google 호출이 일정 건수를 넘으면 차단해요. 차단되면 사용자 이미지나 Unsplash로 대체해요.

### 5-1. 흐름

```
사진 요청 (placeId)
 ├─ 1. Valkey에 Google photoUri 캐시가 있는가? → 있으면 반환 (비용 0)
 ├─ 2. 오늘 Google 호출 수 < 일일 한도인가?
 │     ├─ 예 → Google 호출 → 캐시 저장 → 카운터 +1 → 반환
 │     └─ 아니오(또는 Google 429/오류/사진 없음) → 3번으로
 ├─ 3. 사용자 업로드 이미지(S3)가 있는가?  → 있으면 반환 (비용 0)
 ├─ 4. 도시 단위 Unsplash 이미지 캐시가 있는가? → 있으면 반환
 │     └─ 없으면 Unsplash 검색("{도시명}") → 캐시 저장 → 반환
 └─ 5. 모두 실패 → 기본 이미지
```

- Google 사진을 1순위로 써요. 사진 품질과 일관성을 유지하기 위해서예요.
- 사용자 이미지는 Google을 쓸 수 없을 때(한도 초과, 장애, 사진 없음)의 첫 번째 대체 수단이에요.

### 5-2. 차단은 두 겹으로 걸어요

| 단계 | 위치 | 역할 |
|---|---|---|
| 1차 (주 차단) | 서버 Valkey 카운터 `photo:google:count:{yyyyMMdd}` | 한도의 약 90%에 도달하면 서버가 Google을 부르지 않고 바로 대체 이미지로 넘겨요. 사용자는 실패나 지연을 느끼지 않아요. |
| 2차 (안전장치) | GCP 콘솔의 Quotas & System Limits에서 `GetPlace`, `GetPhotoMedia` 일일 요청 수 제한 | 서버 버그나 키 유출이 있어도 Google이 `429 RESOURCE_EXHAUSTED`를 돌려줘서 과금이 막혀요. 서버는 429를 받으면 대체 이미지로 넘겨요. |
| 보조 | API 키 제한(서버 IP + Places API만 허용), 예산 알림 | **예산 알림은 알림만 보내고 차단하지 않아요.** 실제 차단은 할당량으로 해야 해요. |

- 일일 한도는 `월 무료 건수 ÷ 30`을 기준으로 잡아요.
- GCP 할당량은 미국 태평양 시간 자정에 초기화돼요. 서버 카운터의 날짜 기준도 맞추면 좋아요.
- 카운터 키는 TTL을 2일 정도로 두면 자동으로 정리돼요.

### 5-3. Unsplash 사용 규칙

- **장소 이름이 아니라 도시 이름**으로 검색해요(예: "Paris", "Barcelona").
  - 장소 이름으로 검색하면 다른 장소 사진이 나올 수 있어요.
- 결과는 `photo:unsplash:city:{cityId}`로 길게 캐시해요.
  - 호출 수가 도시 수만큼으로 줄어서 시간당 50건(데모 한도)으로도 충분해요.
- 이미지 URL은 Unsplash가 준 URL을 그대로 써요(핫링크). 이미지 파일 요청은 호출 한도에 포함되지 않아요.
- 화면에 **작가 이름과 Unsplash 표시**가 필요해요.
  - 응답에 `source`(USER / GOOGLE / UNSPLASH / DEFAULT)와 `attribution` 필드를 내려주고, 클라이언트가 표시해요.

### 5-4. 비용

| 구성 | 비용 |
|---|---|
| Valkey 캐시와 카운터 | 이미 운영 중인 ElastiCache를 쓰니까 추가 비용이 거의 없어요. |
| GCP 할당량과 키 제한 | 무료 (설정만 하면 돼요) |
| Google 사진 호출 | 일일 한도를 월 무료 건수 안으로 잡으면 **0원**이에요. 2025년 3월부터 $200 크레딧 대신 SKU별 월 무료 건수가 적용돼요. 정확한 건수는 GCP 가격표에서 확인이 필요해요. |
| 사용자 이미지 (S3) | 이미 저장된 사진이라 전송 비용 정도만 들어요. |
| Unsplash | 무료 |

→ **전체를 사실상 무료로 운영할 수 있고, 비용 폭주 위험도 막을 수 있어요.**

### 5-5. 주의할 점

- **사진이 바뀌는 문제:** 한도가 걸린 날에는 어제 Google 사진이던 장소가 Unsplash 사진으로 바뀔 수 있어요.
  - Google photoUri 캐시 TTL을 길게 잡아서 줄여요.
  - `Cache-Control`도 붙여서, 이미 본 장소는 그대로 보이게 해요.
- **Google 약관:** 사진 원본은 서버에 영구 저장하면 안 돼요. URI만 캐시해요.
- **모니터링:** 대체 이미지로 넘어간 횟수를 메트릭(Prometheus)으로 남겨요. 한도를 조정하는 근거가 돼요.

---

## 6. 방안 D 구현 내용

### 6-1. 실제 동작 순서 (`GET /api/v1/places/{placeId}/photo`)

1. **GOOGLE:**
   - Valkey 캐시(`place:photo:google:{placeId}:{maxWidth}`)를 먼저 봐요.
   - 캐시가 없으면 일일 카운터(`place:photo:google-count:{yyyyMMdd}`, 미국 태평양 시간 기준)를 1 올려요. 한도 이내일 때만 구글을 불러요.
   - 구글에 사진이 없으면 "사진 없음"도 캐시해서 같은 장소로 다시 부르지 않아요.
   - 구글이 실패하면(장애, 429) 캐시하지 않고 다음 단계로 넘어가요.
2. **USER:** 구글을 쓸 수 없을 때, 장소가 그 장소 하나뿐인 코스 일차의 첫 사진을 써요. 삭제된 코스는 빼고, 최근 코스부터 봐요.
   - 장소가 여러 개인 일차는 사진이 어느 장소 것인지 알 수 없어서 제외해요.
3. **UNSPLASH:**
   - 검색어는 도시 영문명 → 국가 영문명 → 구글 표기 도시명 → 구글 표기 국가명 순으로 정해요.
   - 검색어별로 캐시해요(`place:photo:unsplash:{keyword}`).
   - DB에 저장되지 않은 장소(검색 결과에만 있는 장소)는 도시를 알 수 없어서 Unsplash를 쓰지 않아요.
4. 모두 없으면 **404 (PLACE-E002)** 를 내려줘요. 앱이 기본 이미지를 보여줘요.

### 6-2. Valkey 장애 시

| 동작 | 처리 |
|---|---|
| 캐시 조회, 저장 | 캐시가 없는 것으로 보고 계속 진행해요. |
| 구글 일일 카운터 | 구글 호출을 **막아요**(fail-closed). 비용 보호가 우선이에요. |

### 6-3. 응답 헤더 (프론트 전달용)

| 헤더 | 값 |
|---|---|
| `Location` | 이미지 URL (302) |
| `X-Photo-Source` | `USER` / `GOOGLE` / `UNSPLASH` |
| `X-Photo-Author-Name` | (UNSPLASH만) 작가 이름, UTF-8 퍼센트 인코딩 |
| `X-Photo-Author-Url` | (UNSPLASH만) 작가 Unsplash 프로필 URL (utm 포함) |
| `Cache-Control` | `max-age=1800, private` |

- UNSPLASH 사진은 화면에 **"Photo by {작가} on Unsplash"** 표시가 필요해요.
- 이미지 로더가 리다이렉트를 자동으로 따라가면 헤더를 읽기 어려워요. 그래서 프론트와 표시 방법을 협의해야 해요.

### 6-4. 설정 (환경 변수)

| 환경 변수 | 기본값 | 설명 |
|---|---|---|
| `PLACE_PHOTO_GOOGLE_DAILY_LIMIT` | 30 | 하루 구글 사진 조회 한도(1회 = GetPlace + GetPhotoMedia). 월 무료 건수 ÷ 30 이하로 잡아요. |
| `PLACE_PHOTO_GOOGLE_URI_CACHE_TTL` | 6h | 구글 photoUri 캐시 시간 |
| `PLACE_PHOTO_FALLBACK_CACHE_TTL` | 7d | Unsplash 결과 캐시 시간 |
| `PLACE_PHOTO_NOT_FOUND_CACHE_TTL` | 1d | "사진 없음" 캐시 시간 |
| `PLACE_PHOTO_CLIENT_CACHE_MAX_AGE` | 30m | 응답 `Cache-Control` max-age |
| `UNSPLASH_ACCESS_KEY` | (없음) | 비어 있으면 Unsplash를 쓰지 않아요. |

### 6-5. 모니터링 (Prometheus)

- `place_photo_resolved_total{source="USER|GOOGLE|UNSPLASH|NONE"}`: 출처별 사진 응답 수
- `place_photo_google_quota_exceeded_total`: 일일 한도 때문에 구글을 막은 횟수

### 6-6. 서버 밖에서 할 일

- GCP 콘솔 Quotas & System Limits에서 Places API(New)의 일일 요청 수를 제한해요(2차 안전장치).
- API 키를 서버 IP로 제한하고, Places API만 허용해요.
- 예산 알림을 설정해요.
- Unsplash 앱을 등록하고 `UNSPLASH_ACCESS_KEY`를 서버 `.env`에 넣어요. 실서비스 트래픽이면 Production 승인(시간당 5,000건)을 신청해요.

---

## 7. 비교와 추천

| 방안 | 비용 절감 | 사진 정확도 | 작업량 |
|---|---|---|---|
| A. 캐시 + 한도 | 중~상 | 상 (변화 없음) | 하 (stash 재적용) |
| B. 사용자 사진 우선 | 상 | 상 (실제 방문 사진) | 중 |
| C-1. 장소 사진 API로 교체 | 중 | 중~하 (커버리지 부족) | 상 (매칭 로직, 출처 표시) |
| C-2. 스톡 API | 상 | 하 (다른 장소 사진) | 중 |
| **D. 캐시 + 한도 차단 + 대체 이미지** | **최상 (무료 한도 내 운영)** | 상~중 (차단 시에만 도시 이미지) | 중 |

### 추천 로드맵 (방안 D 기준)

1. **1단계 — 비용 차단 (바로):**
   - `stash@{0}`의 캐시 작업을 develop 기준으로 다시 적용해요.
   - `Cache-Control`을 추가해요.
   - GCP 키 제한, 일일 할당량, 예산 알림을 설정해요.
2. **2단계 — 서버 카운터와 대체 이미지:**
   - Valkey 일일 카운터를 만들어요.
   - 한도를 넘으면 기본 이미지로 대체해요.
   - 응답에 `source` 필드를 추가해요.
3. **3단계 — 사용자 이미지 1순위:** 코스 이미지와 장소를 연결해요.
4. **4단계 — Unsplash 대체:**
   - 도시 단위로 검색하고 결과를 캐시해요.
   - 응답에 `attribution`을 내려줘서 화면에 표시해요.
5. **보류:** 장소 사진 API 전체 교체(Foursquare, Tripadvisor)는 커버리지가 부족해서 보류해요.

---

## 8. 출처

- [Foursquare Pricing (공식)](https://location.foursquare.com/?p=19)
- [Foursquare Pricing History — PricingSaaS](https://pricingsaas.com/companies/foursquare)
- [FSQ OS Places is free. So why pay for place search?](https://openplacesapi.com/blog/fsq-os-places-free-place-search)
- [Tripadvisor Content API FAQ (공식)](https://tripadvisor-content-api.readme.io/reference/faq)
- [Best TripAdvisor Data APIs for Developers (2026)](https://www.socialcrawl.dev/blog/best-tripadvisor-data-apis-2026)
- [Tripadvisor pricing — apispine](https://apispine.com/tripadvisor/pricing)
- [Unsplash API Guidelines (공식)](https://help.unsplash.com/api-guidelines)
- [Unsplash: When should I apply for a higher rate limit](https://help.unsplash.com/en/articles/3887917-when-should-i-apply-for-a-higher-rate-limit)

> Pexels, Pixabay, Wikimedia 항목은 공식 문서로 다시 확인하지 않았어요. 도입 전에 각 공식 가이드를 확인하세요.
