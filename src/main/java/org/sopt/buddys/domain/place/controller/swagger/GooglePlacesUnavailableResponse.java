package org.sopt.buddys.domain.place.controller.swagger;

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
    responseCode = "502",
    description = "구글 Places 응답 실패 (업스트림 오류)",
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = BaseResponse.class),
        examples = @ExampleObject(value = """
            {
              "success": false,
              "code": "PLACE-E001",
              "message": "지도 서비스 응답에 실패했습니다.",
              "data": null
            }
            """)
    )
)
public @interface GooglePlacesUnavailableResponse {
}
