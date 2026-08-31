package org.sopt.buddys.domain.verification.controller.swagger;

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

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses({
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
        responseCode = "500",
        description = "인증 메일 발송 실패 또는 서버 내부 오류",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = BaseResponse.class),
            examples = {
                @ExampleObject(
                    name = "인증 메일 발송 실패",
                    value = """
                        {
                          "success": false,
                          "code": "UNIV-E002",
                          "message": "인증 메일 발송에 실패했습니다.",
                          "data": null
                        }
                        """
                ),
                @ExampleObject(
                    name = "서버 내부 오류",
                    value = """
                        {
                          "success": false,
                          "code": "GLB-E005",
                          "message": "서버 내부 오류가 발생했습니다.",
                          "data": null
                        }
                        """
                )
            }
        )
    )
})
public @interface VerificationMailSendFailedResponse {
}
