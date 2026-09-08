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
    summary = "저장한 장소 목록 조회",
    description = "로그인 유저가 저장한 장소를 최근 저장순으로 페이징 조회합니다. "
        + "각 항목은 저장 시점 스냅샷이며, 사진은 photoUrl 프록시로 실시간 조회합니다.")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "조회 성공")
})
@InvalidRequestResponse
@CommonErrorResponses
public @interface GetBookmarkedPlacesSwagger {
}
