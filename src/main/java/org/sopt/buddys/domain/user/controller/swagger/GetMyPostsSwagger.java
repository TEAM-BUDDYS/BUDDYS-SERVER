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
@Operation(summary = "내가 작성한 게시글 목록 조회", description = "로그인한 사용자가 작성한 게시글 목록을 조회합니다.")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "내가 작성한 게시글 목록 조회 성공")
})
@InvalidRequestResponse
@UserNotFoundResponse
@CommonErrorResponses
public @interface GetMyPostsSwagger {
}
