package org.sopt.buddys.domain.auth.controller.swagger;

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
@Operation(summary = "로그아웃", description = "저장된 리프레시 토큰을 폐기하고 리프레시 토큰 쿠키를 삭제합니다.")
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "로그아웃 성공",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = BaseResponse.class),
            examples = @ExampleObject(
                name = "로그아웃 성공",
                value = """
                    {
                      "success": true,
                      "code": "GLB-S001",
                      "message": "요청이 성공했습니다."
                    }
                    """
            )
        )
    ),
    @ApiResponse(
        responseCode = "401",
        description = "인증되지 않은 사용자",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = BaseResponse.class),
            examples = @ExampleObject(
                name = "인증 실패",
                value = """
                    {
                      "success": false,
                      "code": "GLB-E002",
                      "message": "인증이 필요합니다."
                    }
                    """
            )
        )
    )
})
public @interface LogoutSwagger {
}
