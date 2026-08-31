package org.sopt.buddys.domain.verification.controller.swagger;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.sopt.buddys.global.response.BaseResponse;

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponse(
    responseCode = "429",
    description = "인증번호 입력 가능 횟수 초과",
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = BaseResponse.class),
        examples = @ExampleObject(value = """
            {
              "success": false,
              "code": "UNIV-E003",
              "message": "인증번호 입력 가능 횟수를 초과했습니다. 인증번호를 다시 발급해주세요.",
              "data": null
            }
            """)
    )
)
public @interface VerificationAttemptLimitExceededResponse {
}
