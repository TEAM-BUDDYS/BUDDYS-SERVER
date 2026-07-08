package org.sopt.buddys.domain.chat.controller.swagger;

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
@Operation(summary = "채팅방 상세 조회", description = "채팅방 ID로 채팅방 기본 정보와 상대방 정보를 조회합니다.")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "채팅방 상세 조회 성공"),
    @ApiResponse(
        responseCode = "403",
        description = "채팅방 접근 권한 없음",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = BaseResponse.class),
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "code": "GLB-E003",
                  "message": "접근 권한이 없습니다.",
                  "data": null
                }
                """)
        )
    ),
    @ApiResponse(
        responseCode = "404",
        description = "채팅방 없음",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = BaseResponse.class),
            examples = {
                @ExampleObject(
                    name = "채팅방 없음",
                    value = """
                        {
                          "success": false,
                          "code": "CHAT-E002",
                          "message": "채팅방을 찾을 수 없습니다.",
                          "data": null
                        }
                        """
                ),
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
                )
            }
        )
    )
})
@CommonErrorResponses
public @interface GetChatRoomSwagger {
}
