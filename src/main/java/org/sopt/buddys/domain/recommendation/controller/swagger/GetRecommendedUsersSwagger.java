package org.sopt.buddys.domain.recommendation.controller.swagger;

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
    summary = "추천 사용자 조회",
    description = "관심 국가가 같고 취향 태그 유사도가 높은 순으로 추천 사용자를 조회합니다."
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "추천 사용자 조회 성공"),
    @ApiResponse(
        responseCode = "400",
        description = "관심 국가 미설정",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = BaseResponse.class),
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "code": "REC-E001",
                  "message": "관심 국가를 설정해주세요.",
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
    )
})
@CommonErrorResponses
public @interface GetRecommendedUsersSwagger {
}