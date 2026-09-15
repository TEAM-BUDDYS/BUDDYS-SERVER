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
import org.sopt.buddys.global.swagger.InvalidRequestResponse;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Operation(
    summary = "닉네임으로 사용자 검색",
    description = """
        코스 동행 등록 시 추가할 사용자를 닉네임으로 검색합니다.

        - keyword는 대소문자 구분 없이 닉네임에 부분 일치(contains)로 검색되며, 검색 결과는 닉네임 오름차순으로 정렬됩니다.
        - keyword를 생략하거나 빈 문자열/공백만 전달하면 빈 리스트가 반환됩니다.
        - 로그인한 본인은 검색 결과에서 제외됩니다.
        - 커서 없는 Slice 기반 페이지네이션을 사용하며, 다음 페이지 존재 여부는 응답의 hasNext로 확인합니다.
        """
)
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "사용자 검색 성공",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = BaseResponse.class),
            examples = @ExampleObject(value = """
                {
                  "success": true,
                  "code": "GLB-S001",
                  "message": "요청이 성공했습니다.",
                  "data": {
                    "users": [
                      {
                        "userId": 2,
                        "nickname": "버디",
                        "profileImageUrl": "https://example.com/profile.png"
                      }
                    ],
                    "page": 0,
                    "size": 20,
                    "hasNext": false
                  }
                }
                """)
        )
    )
})
@InvalidRequestResponse
@CommonErrorResponses
public @interface SearchUsersByNicknameSwagger {
}
