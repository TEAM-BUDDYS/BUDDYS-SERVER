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
    responseCode = "400",
    description = "인증 코드가 올바르지 않거나 만료됨",
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = BaseResponse.class),
        examples = @ExampleObject(value = """
            {
              "success": false,
              "code": "UNIV-E001",
              "message": "인증 코드가 올바르지 않거나 만료되었습니다.",
              "data": null
            }
            """)
    )
)
public @interface VerificationCodeInvalidResponse {
}
