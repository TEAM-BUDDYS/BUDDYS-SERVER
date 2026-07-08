package org.sopt.buddys.domain.user.controller.swagger;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
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
    summary = "온보딩 완료",
    description = "관심 국가/도시, 교환학생 정보(선택), 태그, 프로필 정보를 저장하여 온보딩을 완료합니다."
)
@RequestBody(
    required = true,
    content = @Content(
        mediaType = "application/json",
        examples = @ExampleObject(value = """
            {
              "interestCountryId": 31,
              "interestCityId": 20692,
              "exchangeCountryId": 71,
              "exchangeUniversity": "Harvard University",
              "exchangeStartDate": "2027-03-01",
              "exchangeEndDate": "2027-08-31",
              "activityTagIds": [1, 2, 3],
              "interestTagIds": [13, 26],
              "travelStyleTagIds": [27, 30],
              "nickname": "버디즈",
              "gender": "FEMALE",
              "birthDate": "2004-02-22",
              "bio": "같이 여행 다녀요!",
              "profileImageUrl": "https://example.com/profile.png"
            }
            """)
    )
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "온보딩 완료 성공"),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = BaseResponse.class),
            examples = {
                @ExampleObject(
                    name = "잘못된 요청",
                    value = """
                        {
                          "success": false,
                          "code": "GLB-E001",
                          "message": "잘못된 요청입니다.",
                          "data": null
                        }
                        """
                ),
                @ExampleObject(
                    name = "잘못된 교환학생 기간",
                    value = """
                        {
                          "success": false,
                          "code": "USER-E002",
                          "message": "교환학생 시작일은 종료일보다 이전이어야 합니다.",
                          "data": null
                        }
                        """
                ),
                @ExampleObject(
                    name = "관심 도시-국가 불일치",
                    value = """
                        {
                          "success": false,
                          "code": "USER-E003",
                          "message": "관심 도시가 관심 국가에 속하지 않습니다.",
                          "data": null
                        }
                        """
                ),
                @ExampleObject(
                    name = "유효하지 않은 태그",
                    value = """
                        {
                          "success": false,
                          "code": "USER-E004",
                          "message": "유효하지 않은 태그가 포함되어 있습니다.",
                          "data": null
                        }
                        """
                )
            }
        )
    ),
    @ApiResponse(
        responseCode = "404",
        description = "사용자, 국가 또는 도시 없음",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = BaseResponse.class),
            examples = {
                @ExampleObject(
                    name = "사용자 없음",
                    value = """
                        {
                          "success": false,
                          "code": "USER-E001",
                          "message": "사용자를 찾을 수 없습니다.",
                          "data": null
                        }
                        """
                ),
                @ExampleObject(
                    name = "국가 없음",
                    value = """
                        {
                          "success": false,
                          "code": "LOC-E001",
                          "message": "국가를 찾을 수 없습니다.",
                          "data": null
                        }
                        """
                ),
                @ExampleObject(
                    name = "도시 없음",
                    value = """
                        {
                          "success": false,
                          "code": "LOC-E002",
                          "message": "도시를 찾을 수 없습니다.",
                          "data": null
                        }
                        """
                )
            }
        )
    ),
    @ApiResponse(
        responseCode = "409",
        description = "닉네임 중복",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = BaseResponse.class),
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "code": "AUTH-E003",
                  "message": "이미 사용 중인 닉네임입니다.",
                  "data": null
                }
                """)
        )
    )
})
@CommonErrorResponses
public @interface CompleteOnboardingSwagger {
}