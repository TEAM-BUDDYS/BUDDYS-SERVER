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
    summary = "내 관심 국가 및 파견 국가 조회",
    description = "로그인한 사용자가 설정한 관심 국가와 파견 국가의 ID, 이름, 영문 이름, ISO 코드를 조회합니다. "
        + "설정하지 않은 국가는 null로 반환합니다."
)
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "국가 조회 성공",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = BaseResponse.class),
            examples = {
                @ExampleObject(name = "관심 국가와 파견 국가를 모두 설정한 경우", value = """
                    {
                      "success": true,
                      "code": "GLB-S001",
                      "message": "요청이 성공했습니다.",
                      "data": {
                        "interestCountry": {
                          "id": 31,
                          "name": "프랑스",
                          "englishName": "France",
                          "code": "FR"
                        },
                        "exchangeCountry": {
                          "id": 71,
                          "name": "독일",
                          "englishName": "Germany",
                          "code": "DE"
                        }
                      }
                    }
                    """),
                @ExampleObject(name = "파견 국가를 설정하지 않은 경우", value = """
                    {
                      "success": true,
                      "code": "GLB-S001",
                      "message": "요청이 성공했습니다.",
                      "data": {
                        "interestCountry": {
                          "id": 31,
                          "name": "프랑스",
                          "englishName": "France",
                          "code": "FR"
                        },
                        "exchangeCountry": null
                      }
                    }
                    """)
            }
        )
    )
})
@UserNotFoundResponse
@CommonErrorResponses
public @interface GetMyCountriesSwagger {
}
