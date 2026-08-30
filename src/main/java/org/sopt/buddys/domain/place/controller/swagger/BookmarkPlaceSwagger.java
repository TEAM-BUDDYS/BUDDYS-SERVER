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
    summary = "장소 저장",
    description = "구글 place_id로 장소를 로그인 유저의 저장 목록에 추가합니다. 이미 저장돼 있으면 그대로 성공 응답합니다.")
@ApiResponses({
    @ApiResponse(responseCode = "201", description = "저장 성공")
})
@InvalidRequestResponse
@GooglePlacesUnavailableResponse
@CommonErrorResponses
public @interface BookmarkPlaceSwagger {
}
