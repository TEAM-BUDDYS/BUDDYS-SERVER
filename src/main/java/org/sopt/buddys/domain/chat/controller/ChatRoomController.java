package org.sopt.buddys.domain.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.chat.dto.request.CreateChatRoomRequest;
import org.sopt.buddys.domain.chat.dto.response.ChatRoomResponse;
import org.sopt.buddys.domain.chat.service.ChatRoomService;
import org.sopt.buddys.domain.user.controller.swagger.UserNotFoundResponse;
import org.sopt.buddys.global.common.code.GlobalSuccessCode;
import org.sopt.buddys.global.response.BaseResponse;
import org.sopt.buddys.global.security.annotation.LoginUser;
import org.sopt.buddys.global.swagger.CommonErrorResponses;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat-rooms")
@Tag(name = "Chat Room", description = "채팅 API")
public class ChatRoomController {

  private final ChatRoomService chatRoomService;

  @Operation(summary = "채팅방 생성", description = "상대방과의 1:1 채팅방이 없으면 생성하고, 이미 있으면 기존 채팅방을 반환합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "채팅방 조회 또는 생성 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청")
  })
  @UserNotFoundResponse
  @CommonErrorResponses
  @PostMapping
  public BaseResponse<ChatRoomResponse> createChatRoom(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @RequestBody @Valid CreateChatRoomRequest request
  ) {

    return BaseResponse.success(
        GlobalSuccessCode.OK,
        ChatRoomResponse.from(
            chatRoomService.createOrGetChatRoom(userId, request.participantUserId())
        )
    );
  }
}
