package org.sopt.buddys.domain.chat.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.chat.dto.request.ChatMessageSendRequest;
import org.sopt.buddys.domain.chat.dto.request.ChatReadRequest;
import org.sopt.buddys.domain.chat.dto.response.ChatMessageEventResponse;
import org.sopt.buddys.domain.chat.dto.response.ChatReadEventResponse;
import org.sopt.buddys.domain.chat.dto.response.ChatRoomListEventResponse;
import org.sopt.buddys.domain.chat.service.ChatMessageCommandService;
import org.sopt.buddys.domain.chat.service.ChatReadService;
import org.sopt.buddys.domain.chat.service.ChatRoomService;
import org.sopt.buddys.domain.chat.service.result.ChatMessageSendResult;
import org.sopt.buddys.domain.chat.service.result.ChatReadResult;
import org.sopt.buddys.domain.chat.service.result.ChatRoomListResult.ChatRoomListItemResult;
import org.sopt.buddys.global.common.code.GlobalErrorCode;
import org.sopt.buddys.global.exception.BaseException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

@Controller
@Validated
@RequiredArgsConstructor
public class ChatMessageWebSocketController {

  private static final String CHAT_ROOM_TOPIC_PREFIX = "/sub/chat-rooms/";
  private static final String CHAT_ROOM_LIST_USER_DESTINATION = "/sub/chat-room-list";

  private final ChatMessageCommandService chatMessageCommandService;
  private final ChatReadService chatReadService;
  private final ChatRoomService chatRoomService;
  private final SimpMessagingTemplate messagingTemplate;

  @MessageMapping("/chat-rooms/{chatRoomId}/messages")
  public void sendMessage(
      @DestinationVariable Long chatRoomId,
      @Payload @Valid ChatMessageSendRequest request,
      Principal principal
  ) {

    Long userId = getUserId(principal);
    ChatMessageSendResult result = chatMessageCommandService.sendMessage(
        userId,
        chatRoomId,
        request.content()
    );

    messagingTemplate.convertAndSend(
        CHAT_ROOM_TOPIC_PREFIX + chatRoomId,
        ChatMessageEventResponse.from(result)
    );

    sendChatRoomListUpdateToMembers(chatRoomId);
  }

  @MessageMapping("/chat-rooms/{chatRoomId}/read")
  public void readMessage(
      @DestinationVariable Long chatRoomId,
      @Payload @Valid ChatReadRequest request,
      Principal principal
  ) {

    Long userId = getUserId(principal);
    ChatReadResult result = chatReadService.markAsRead(
        userId,
        chatRoomId,
        request.lastReadMessageId()
    );

    messagingTemplate.convertAndSend(
        CHAT_ROOM_TOPIC_PREFIX + chatRoomId,
        ChatReadEventResponse.from(result)
    );

    sendChatRoomListUpdate(userId, chatRoomId);
  }

  private void sendChatRoomListUpdateToMembers(Long chatRoomId) {
    chatRoomService.getChatRoomMemberIds(chatRoomId)
        .forEach(memberId -> sendChatRoomListUpdate(memberId, chatRoomId));
  }

  private void sendChatRoomListUpdate(
      Long userId,
      Long chatRoomId
  ) {

    ChatRoomListItemResult chatRoom = chatRoomService.getChatRoomListItem(userId, chatRoomId);

    messagingTemplate.convertAndSendToUser(
        userId.toString(),
        CHAT_ROOM_LIST_USER_DESTINATION,
        ChatRoomListEventResponse.from(chatRoom)
    );
  }

  private Long getUserId(
      Principal principal
  ) {

    try {
      return Long.valueOf(principal.getName());
    } catch (NullPointerException | NumberFormatException e) {
      throw new BaseException(GlobalErrorCode.INVALID_REQUEST);
    }
  }
}
