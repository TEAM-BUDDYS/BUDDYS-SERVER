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
    summary = "프로필 편집 정보 조회",
    description = "편집 가능한 프로필 값과 사용자가 지정한 순서대로 정렬된 전체 태그를 반환합니다. 태그 배열의 앞 3개가 대표 태그입니다."
)
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "프로필 편집 정보 조회 성공",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = BaseResponse.class),
            examples = @ExampleObject(value = """
                {
                  "success": true,
                  "code": "GLB-S001",
                  "message": "요청이 성공했습니다.",
                  "data": {
                    "nickname": "정바미",
                    "gender": "FEMALE",
                    "birthDate": "2004-10-24",
                    "bio": "안녕하세요 김버디입니다~~",
                    "orderedTags": [
                      { "id": 27, "name": "계획형", "tagType": "TRAVEL_STYLE" },
                      { "id": 1, "name": "여행", "tagType": "ACTIVITY" },
                      { "id": 13, "name": "자연", "tagType": "INTEREST" },
                      { "id": 28, "name": "즉흥형", "tagType": "TRAVEL_STYLE" }
                    ]
                  }
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
public @interface GetProfileEditSwagger {
}
