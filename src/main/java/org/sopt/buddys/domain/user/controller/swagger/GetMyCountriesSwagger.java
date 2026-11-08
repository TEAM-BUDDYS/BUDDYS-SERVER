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
        + "설정하지 않은 국가는 null로 반환합니다. "
        + "secondSectionCountry는 홈 두 번째 코스 섹션에 보여줄 국가입니다. 파견 국가가 있고 관심 국가와 다르면 파견 국가(EXCHANGE), "
        + "파견 국가가 없거나 관심 국가와 같으면 관심 국가를 제외하고 코스가 가장 많은 국가(MOST_COURSES)이며, 보여줄 국가가 없으면 null입니다."
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
                        },
                        "secondSectionCountry": {
                          "id": 71,
                          "name": "독일",
                          "englishName": "Germany",
                          "code": "DE",
                          "reason": "EXCHANGE"
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
                        "exchangeCountry": null,
                        "secondSectionCountry": {
                          "id": 240,
                          "name": "스페인",
                          "englishName": "Spain",
                          "code": "ES",
                          "reason": "MOST_COURSES"
                        }
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
