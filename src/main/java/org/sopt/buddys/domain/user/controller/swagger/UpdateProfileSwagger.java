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
    summary = "프로필 수정",
    description = """
            프로필 정보를 전체 수정합니다.
            모든 필드를 요청에 포함해야 합니다.
            태그는 전달된 orderedTagIds로 전체 교체되며,
            bio를 null로 전달하면 기존 자기소개가 삭제됩니다.
            태그 배열의 앞 3개가 대표 태그입니다.
            """
)
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "프로필 수정 성공",
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
        responseCode = "400",
        description = "잘못된 프로필 수정 요청",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = BaseResponse.class),
            examples = {
                @ExampleObject(
                    name = "요청값 검증 실패",
                    value = """
                        {
                          "success": false,
                          "code": "GLB-E001",
                          "message": "잘못된 요청입니다.",
                          "data": {
                            "nickname": "크기가 0에서 14 사이여야 합니다"
                          }
                        }
                        """
                ),
                @ExampleObject(
                    name = "태그 선택 개수 오류",
                    value = """
                        {
                          "success": false,
                          "code": "USER-E008",
                          "message": "활동과 관심사 태그는 각각 1~3개, 여행 스타일 태그는 1~5개 선택해야 합니다.",
                          "data": null
                        }
                        """
                ),
                @ExampleObject(
                    name = "중복 태그 포함",
                    value = """
                        {
                          "success": false,
                          "code": "USER-E009",
                          "message": "중복된 태그가 포함되어 있습니다.",
                          "data": null
                        }
                        """
                )
            }
        )
    ),
    @ApiResponse(
        responseCode = "404",
        description = "사용자 또는 태그 없음",
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
                    name = "태그 없음",
                    value = """
                        {
                          "success": false,
                          "code": "USER-E006",
                          "message": "존재하지 않는 태그가 포함되어 있습니다.",
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
public @interface UpdateProfileSwagger {
}
