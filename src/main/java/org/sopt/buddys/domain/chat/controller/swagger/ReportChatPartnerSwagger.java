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
    summary = "채팅 상대방 신고",
    description = """
        해당 채팅방의 상대방을 신고합니다.

        - 신고 사유(reason)는 선택 입력이며, 생략하면 사유 없이 즉시 접수됩니다.
        - 신고 접수 시 신고자·신고 대상자 정보(및 입력된 사유)를 포함한 메일이 운영팀으로 발송됩니다.
        """
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "신고 접수 성공"),
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
    ),
    @ApiResponse(
        responseCode = "500",
        description = "신고 접수 메일 발송 실패",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = BaseResponse.class),
            examples = @ExampleObject(value = """
                {
                  "success": false,
                  "code": "CHAT-E004",
                  "message": "신고 접수 메일 발송에 실패했습니다.",
                  "data": null
                }
                """)
        )
    )
})
@CommonErrorResponses
public @interface ReportChatPartnerSwagger {
}
