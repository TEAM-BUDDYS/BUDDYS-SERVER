package org.sopt.buddys.domain.chat.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.chat.controller.swagger.CreateChatRoomSwagger;
import org.sopt.buddys.domain.chat.controller.swagger.GetChatMessagesSwagger;
import org.sopt.buddys.domain.chat.controller.swagger.GetChatRoomsSwagger;
import org.sopt.buddys.domain.chat.controller.swagger.GetChatRoomSwagger;
import org.sopt.buddys.domain.chat.dto.request.CreateChatRoomRequest;
import org.sopt.buddys.domain.chat.dto.response.ChatMessageListResponse;
import org.sopt.buddys.domain.chat.dto.response.ChatRoomListResponse;
import org.sopt.buddys.domain.chat.dto.response.ChatRoomResponse;
import org.sopt.buddys.domain.chat.service.ChatMessageService;
import org.sopt.buddys.domain.chat.service.ChatRoomService;
import org.sopt.buddys.domain.chat.util.ChatTimeConverter;
import org.sopt.buddys.global.common.code.GlobalSuccessCode;
import org.sopt.buddys.global.response.BaseResponse;
import org.sopt.buddys.global.security.annotation.LoginUser;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
  private final ChatMessageService chatMessageService;

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

  @GetChatRoomSwagger
  @GetMapping("/{chatRoomId}")
  public BaseResponse<ChatRoomResponse> getChatRoom(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @Parameter(description = "조회할 채팅방 ID", example = "1")
      @PathVariable Long chatRoomId
  ) {

    return BaseResponse.success(
        GlobalSuccessCode.OK,
        ChatRoomResponse.from(chatRoomService.getChatRoom(userId, chatRoomId))
    );
  }

  @GetChatMessagesSwagger
  @GetMapping("/{chatRoomId}/messages")
  public BaseResponse<ChatMessageListResponse> getMessages(
      @Parameter(hidden = true)
      @LoginUser Long userId,
      @Parameter(description = "조회할 채팅방 ID", example = "1")
      @PathVariable Long chatRoomId,
      @Parameter(description = "커서 메시지 전송 시각. UTC 기준입니다.", example = "2026-07-07T14:30:00Z")
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
      OffsetDateTime cursorSentAt,
      @Parameter(description = "커서 메시지 ID", example = "101")
      @RequestParam(required = false) Long cursorMessageId,
      @Parameter(description = "조회 개수. 1 이상 100 이하입니다.", example = "30")
      @RequestParam(defaultValue = "30") @Min(1) @Max(100) int size
  ) {

    return BaseResponse.success(
        GlobalSuccessCode.OK,
        ChatMessageListResponse.from(
            chatMessageService.getMessages(
                userId,
                chatRoomId,
                ChatTimeConverter.toStorageTime(cursorSentAt),
                cursorMessageId,
                size
            )
        )
    );
  }
}
