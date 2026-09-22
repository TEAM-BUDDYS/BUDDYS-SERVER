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
@Operation(
    summary = "채팅 상대방 차단",
    description = """
        해당 채팅방의 상대방을 차단합니다.

        - 차단 후에는 양쪽 모두 해당 상대방과 서로 메시지를 보낼 수 없습니다.
        - 기존 채팅 내역은 삭제되지 않고 그대로 유지됩니다.
        - 이미 차단한 상대방을 다시 차단해도 오류 없이 처리됩니다(멱등).
        """
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "차단 성공"),
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
        description = "채팅방을 찾을 수 없음",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = BaseResponse.class),
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "code": "CHAT-E002",
                  "message": "채팅방을 찾을 수 없습니다.",
                  "data": null
                }
                """)
        )
    )
})
@CommonErrorResponses
public @interface BlockChatPartnerSwagger {
}
