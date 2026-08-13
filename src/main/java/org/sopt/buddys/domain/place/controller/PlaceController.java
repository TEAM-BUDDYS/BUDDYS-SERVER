package org.sopt.buddys.domain.place.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.place.dto.response.PlaceSearchResponse;
import org.sopt.buddys.domain.place.entity.PlaceCategory;
import org.sopt.buddys.domain.place.service.PlaceService;
import org.sopt.buddys.global.common.code.GlobalSuccessCode;
import org.sopt.buddys.global.response.BaseResponse;
import org.sopt.buddys.global.security.annotation.LoginUser;
import org.sopt.buddys.global.swagger.CommonErrorResponses;
import org.sopt.buddys.global.swagger.InvalidRequestResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/places")
@Tag(name = "Place", description = "장소 검색 API")
public class PlaceController {

  private final PlaceService placeService;

  @Operation(summary = "장소 검색", description = "구글 Places API를 통해 장소를 검색합니다. lat/lng를 함께 주면 해당 좌표 주변 결과를 우선합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "검색 성공"),
      @ApiResponse(
          responseCode = "400",
          description = "잘못된 요청 — query 파라미터 누락, category 값이 유효하지 않음, lat/lng 중 하나만 전달됨 중 하나",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = BaseResponse.class),
              examples = {
                  @ExampleObject(
                      name = "query 파라미터 누락",
                      summary = "query를 아예 안 보냈거나 빈 문자열인 경우",
                      value = """
                          {
                            "success": false,
                            "code": "GLB-E001",
                            "message": "잘못된 요청입니다.",
                            "data": null
                          }
                          """
                  ),
                  @ExampleObject(
                      name = "잘못된 category 값",
                      summary = "RESTAURANT, CAFE, TOURISM, ACCOMMODATION 중 하나가 아닌 값을 보낸 경우",
                      value = """
                          {
                            "success": false,
                            "code": "GLB-E001",
                            "message": "잘못된 요청입니다.",
                            "data": null
                          }
                          """
                  ),
                  @ExampleObject(
                      name = "lat/lng 중 하나만 전달됨",
                      summary = "위치 편향은 lat, lng를 함께 보내야 하며 하나만 보내면 실패",
                      value = """
                          {
                            "success": false,
                            "code": "GLB-E001",
                            "message": "잘못된 요청입니다.",
                            "data": null
                          }
                          """
                  )
              }
          )
      ),
      @ApiResponse(
          responseCode = "502",
          description = "구글 Places 응답 실패 (업스트림 오류)",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = BaseResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "success": false,
                    "code": "PLACE-E001",
                    "message": "지도 서비스 응답에 실패했습니다.",
                    "data": null
                  }
                  """)
          )
      )
  })
  @CommonErrorResponses
  @GetMapping("/search")
  public BaseResponse<PlaceSearchResponse> searchPlaces(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @Parameter(description = "검색 키워드", example = "커피")
      @RequestParam @NotBlank String query,
      @Parameter(description = "장소 카테고리, 없으면 전체 검색", example = "CAFE")
      @RequestParam(required = false) PlaceCategory category,
      @Parameter(description = "위도. lng와 함께 넘기면 주변 검색으로 편향됩니다.", example = "37.5567")
      @RequestParam(required = false) BigDecimal lat,
      @Parameter(description = "경도. lat와 함께 넘기면 주변 검색으로 편향됩니다.", example = "126.9236")
      @RequestParam(required = false) BigDecimal lng,
      @Parameter(description = "이전 응답의 nextPageToken. 다음 페이지 조회 시 사용", example = "AeCrKx...")
      @RequestParam(required = false) String pageToken
  ) {
    return BaseResponse.success(GlobalSuccessCode.OK,
        PlaceSearchResponse.from(placeService.search(userId, query, category, lat, lng, pageToken))
    );
  }

  @Operation(summary = "장소 사진 프록시", description = "장소의 대표 사진 URL로 302 리다이렉트합니다. 구글 API 키를 클라이언트에 노출하지 않기 위한 프록시입니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "302", description = "리다이렉트 성공"),
      @ApiResponse(
          responseCode = "404",
          description = "사진 없음",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = BaseResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "success": false,
                    "code": "PLACE-E002",
                    "message": "장소 사진을 찾을 수 없습니다.",
                    "data": null
                  }
                  """)
          )
      ),
      @ApiResponse(
          responseCode = "502",
          description = "구글 Places 응답 실패 (업스트림 오류)",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = BaseResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "success": false,
                    "code": "PLACE-E001",
                    "message": "지도 서비스 응답에 실패했습니다.",
                    "data": null
                  }
                  """)
          )
      )
  })
  @InvalidRequestResponse
  @GetMapping("/{placeId}/photo")
  public ResponseEntity<Void> getPlacePhoto(
      @Parameter(description = "구글 place_id", example = "ChIJN1t_tDeuEmsRUsoyG83frY4")
      @PathVariable String placeId,
      @Parameter(description = "최대 가로 픽셀. 1 이상 4800 이하입니다.", example = "400")
      @RequestParam(defaultValue = "400") @Min(1) @Max(4800) int maxWidth
  ) {
    String photoUri = placeService.getPhotoRedirectUri(placeId, maxWidth);
    return ResponseEntity.status(HttpStatus.FOUND)
        .location(URI.create(photoUri))
        .build();
  }
}