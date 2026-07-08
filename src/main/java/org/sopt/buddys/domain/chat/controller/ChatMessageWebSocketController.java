package org.sopt.buddys.domain.chat.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.chat.dto.request.ChatMessageSendRequest;
import org.sopt.buddys.domain.chat.dto.response.ChatMessageEventResponse;
import org.sopt.buddys.domain.chat.service.ChatMessageCommandService;
import org.sopt.buddys.domain.chat.service.result.ChatMessageSendResult;
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

  private final ChatMessageCommandService chatMessageCommandService;
  private final SimpMessagingTemplate messagingTemplate;

  @MessageMapping("/chat-rooms/{chatRoomId}/messages")
  public void sendMessage(
      @DestinationVariable Long chatRoomId,
      @Payload @Valid ChatMessageSendRequest request,
      Principal principal
  ) {

    Long userId = Long.valueOf(principal.getName());
    ChatMessageSendResult result = chatMessageCommandService.sendMessage(
        userId,
        chatRoomId,
        request.content()
    );

    messagingTemplate.convertAndSend(
        CHAT_ROOM_TOPIC_PREFIX + chatRoomId,
        ChatMessageEventResponse.from(result)
    );
  }
}
