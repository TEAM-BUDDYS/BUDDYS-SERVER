package org.sopt.buddys.domain.user.controller.swagger;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.sopt.buddys.global.response.BaseResponse;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Operation(summary = "내 프로필 조회", description = "로그인한 사용자의 프로필, 태그, 작성 게시글 목록을 조회합니다.")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "내 프로필 조회 성공"),
    @ApiResponse(
        responseCode = "401",
        description = "인증 필요",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = BaseResponse.class),
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "code": "GLB-E002",
                  "message": "인증이 필요합니다.",
                  "data": null
                }
                """)
        )
    ),
    @ApiResponse(
        responseCode = "404",
        description = "사용자 없음",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = BaseResponse.class),
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "code": "USER-E001",
                  "message": "사용자를 찾을 수 없습니다.",
                  "data": null
                }
                """)
        )
    ),
    @ApiResponse(
        responseCode = "500",
        description = "서버 내부 오류",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = BaseResponse.class),
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "code": "GLB-E005",
                  "message": "서버 내부 오류가 발생했습니다.",
                  "data": null
                }
                """)
        )
    )
})
public @interface GetMyProfileSwagger {
}
