package org.sopt.buddys.domain.chat.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.chat.controller.swagger.CreateChatRoomSwagger;
import org.sopt.buddys.domain.chat.controller.swagger.GetChatRoomsSwagger;
import org.sopt.buddys.domain.chat.dto.request.CreateChatRoomRequest;
import org.sopt.buddys.domain.chat.dto.response.ChatRoomListResponse;
import org.sopt.buddys.domain.chat.dto.response.ChatRoomResponse;
import org.sopt.buddys.domain.chat.service.ChatRoomService;
import org.sopt.buddys.global.common.code.GlobalSuccessCode;
import org.sopt.buddys.global.response.BaseResponse;
import org.sopt.buddys.global.security.annotation.LoginUser;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
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

  @GetChatRoomsSwagger
  @GetMapping
  public BaseResponse<ChatRoomListResponse> getChatRooms(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @Parameter(description = "페이지 번호. 0 이상입니다.", example = "0")
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @Parameter(description = "페이지 크기. 1 이상 100 이하입니다.", example = "20")
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
  ) {

    return BaseResponse.success(
        GlobalSuccessCode.OK,
        ChatRoomListResponse.from(chatRoomService.getChatRooms(userId, page, size))
    );
  }
}
