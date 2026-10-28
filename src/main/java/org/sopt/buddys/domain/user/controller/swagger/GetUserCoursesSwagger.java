package org.sopt.buddys.domain.user.controller.swagger;

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
@Operation(summary = "타 유저 코스 목록 조회", description = "특정 사용자가 작성하거나 동행으로 태그된 코스 목록을 최신순으로 조회합니다.")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "타 유저 코스 목록 조회 성공")
})
@InvalidRequestResponse
@UserNotFoundResponse
@CommonErrorResponses
public @interface GetUserCoursesSwagger {
}
