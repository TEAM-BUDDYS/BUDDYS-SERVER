package org.sopt.buddys.domain.user.controller.swagger;

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
)
public @interface UserNotFoundResponse {
}
