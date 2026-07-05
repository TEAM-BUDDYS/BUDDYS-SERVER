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
import org.sopt.buddys.domain.user.controller.swagger.UserNotFoundResponse;
import org.sopt.buddys.global.response.BaseResponse;
import org.sopt.buddys.global.swagger.CommonErrorResponses;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Operation(summary = "채팅방 생성", description = "상대방과의 1:1 채팅방이 없으면 생성하고, 이미 있으면 기존 채팅방을 반환합니다.")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "채팅방 조회 또는 생성 성공"),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = BaseResponse.class),
            examples = {
                @ExampleObject(
                    name = "자기 자신과 채팅방 생성 불가",
                    value = """
                        {
                          "success": false,
                          "code": "CHAT-E001",
                          "message": "자기 자신과는 채팅방을 생성할 수 없습니다.",
                          "data": null
                        }
                        """
                ),
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
                )
            }
        )
    )
})
@UserNotFoundResponse
@CommonErrorResponses
public @interface CreateChatRoomSwagger {
}
