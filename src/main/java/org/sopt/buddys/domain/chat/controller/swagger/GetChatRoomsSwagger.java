package org.sopt.buddys.domain.chat.controller.swagger;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.sopt.buddys.domain.user.controller.swagger.UserNotFoundResponse;
import org.sopt.buddys.global.swagger.CommonErrorResponses;
import org.sopt.buddys.global.swagger.InvalidRequestResponse;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Operation(summary = "채팅방 목록 조회", description = "로그인한 사용자가 참여 중인 채팅방 목록을 조회합니다.")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "채팅방 목록 조회 성공")
})
@InvalidRequestResponse
@UserNotFoundResponse
@CommonErrorResponses
public @interface GetChatRoomsSwagger {
}
