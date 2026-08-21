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
import org.sopt.buddys.global.swagger.CommonErrorResponses;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Operation(
    summary = "회원 탈퇴",
    description = "로그인한 회원을 탈퇴 상태로 변경하고 개인정보를 익명화합니다. "
        + "리프레시 토큰과 개인화 데이터는 삭제하며, 작성한 콘텐츠는 유지됩니다."
)
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "회원 탈퇴 성공",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = BaseResponse.class),
            examples = @ExampleObject(value = """
                {
                  "success": true,
                  "code": "GLB-S001",
                  "message": "요청이 성공했습니다."
                }
                """)
        )
    )
})
@CommonErrorResponses
public @interface WithdrawUserSwagger {
}
