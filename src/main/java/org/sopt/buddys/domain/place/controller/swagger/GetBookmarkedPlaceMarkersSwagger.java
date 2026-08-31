package org.sopt.buddys.domain.place.controller.swagger;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.sopt.buddys.global.swagger.CommonErrorResponses;
import org.sopt.buddys.global.swagger.InvalidRequestResponse;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Operation(
    summary = "지도 영역 내 저장한 장소 조회",
    description = "\"현재 화면에서 저장한 장소만 보기\"용. 지도 bounds(남서/북동 좌표) 안에 있는 "
        + "저장 장소를 페이징 없이 반환합니다. 좌표가 없는 저장 장소는 제외되며, "
        + "상한(300개) 초과 시 truncated=true로 잘라서 반환합니다.")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공")
})
@InvalidRequestResponse
@CommonErrorResponses
public @interface GetBookmarkedPlaceMarkersSwagger {
}
