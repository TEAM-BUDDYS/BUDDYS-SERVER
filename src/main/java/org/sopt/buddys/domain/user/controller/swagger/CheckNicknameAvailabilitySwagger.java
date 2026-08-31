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
import org.sopt.buddys.global.swagger.InvalidRequestResponse;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Operation(summary = "닉네임 중복 확인", description = "현재 사용자를 제외하고 닉네임 사용 가능 여부를 확인합니다.")
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "닉네임 중복 확인 성공",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = BaseResponse.class),
            examples = {
                @ExampleObject(
                    name = "사용 가능한 닉네임",
                    value = """
                        {
                          "success": true,
                          "code": "GLB-S001",
                          "message": "요청이 성공했습니다.",
                          "data": {
                            "available": true
                          }
                        }
                        """
                ),
                @ExampleObject(
                    name = "이미 사용 중인 닉네임",
                    value = """
                        {
                          "success": true,
                          "code": "GLB-S001",
                          "message": "요청이 성공했습니다.",
                          "data": {
                            "available": false
                          }
                        }
                        """
                )
            }
        )
    )
})
@InvalidRequestResponse
@UserNotFoundResponse
@CommonErrorResponses
public @interface CheckNicknameAvailabilitySwagger {
}
