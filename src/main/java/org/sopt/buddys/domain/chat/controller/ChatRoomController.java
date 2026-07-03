package org.sopt.buddys.domain.chat.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.chat.controller.swagger.CreateChatRoomSwagger;
import org.sopt.buddys.domain.chat.dto.request.CreateChatRoomRequest;
import org.sopt.buddys.domain.chat.dto.response.ChatRoomResponse;
import org.sopt.buddys.domain.chat.service.ChatRoomService;
import org.sopt.buddys.global.common.code.GlobalSuccessCode;
import org.sopt.buddys.global.response.BaseResponse;
import org.sopt.buddys.global.security.annotation.LoginUser;
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

  @CreateChatRoomSwagger
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
