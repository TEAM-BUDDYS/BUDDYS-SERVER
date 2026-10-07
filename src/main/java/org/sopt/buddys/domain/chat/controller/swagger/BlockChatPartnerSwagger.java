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

        - 차단 후에는 내가 상대방에게 메시지를 보낼 수 없습니다.
        - 상대방은 차단 사실을 알 수 없으며, 기존과 같이 메시지를 보낼 수 있습니다.
        - 차단 이후 상대방이 보낸 메시지는 저장되지만, 나의 메시지 목록·채팅방 목록·실시간 수신에는 표시되지 않습니다.
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
