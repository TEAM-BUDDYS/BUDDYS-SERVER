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
    summary = "장소 저장 취소",
    description = "구글 place_id로 장소를 로그인 유저의 저장 목록에서 제거합니다. 저장돼 있지 않아도 성공 응답합니다.")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "저장 취소 성공")
})
@InvalidRequestResponse
@CommonErrorResponses
public @interface CancelPlaceBookmarkSwagger {
}
