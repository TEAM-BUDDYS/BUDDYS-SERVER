package org.sopt.buddys.domain.chat.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.chat.dto.request.ChatMessageSendRequest;
import org.sopt.buddys.domain.chat.dto.response.ChatMessageEventResponse;
import org.sopt.buddys.domain.chat.entity.ChatMessage;
import org.sopt.buddys.domain.chat.entity.ChatRoom;
import org.sopt.buddys.domain.chat.service.ChatMessageCommandService;
import org.sopt.buddys.domain.chat.service.ChatReadService;
import org.sopt.buddys.domain.chat.service.ChatRoomService;
import org.sopt.buddys.domain.chat.service.result.ChatMessageSendResult;
import org.sopt.buddys.domain.chat.service.result.ChatRoomListResult.ChatRoomListItemResult;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

class ChatMessageWebSocketControllerTest {

  private static final long CHAT_ROOM_ID = 1L;
  private static final long SENDER_ID = 2L;
  private static final long BLOCKER_ID = 3L;

  private ChatMessageCommandService chatMessageCommandService;
  private ChatRoomService chatRoomService;
  private SimpMessagingTemplate messagingTemplate;
  private ChatMessageWebSocketController controller;

  @BeforeEach
  void setUp() {
    chatMessageCommandService = mock(ChatMessageCommandService.class);
    chatRoomService = mock(ChatRoomService.class);
    messagingTemplate = mock(SimpMessagingTemplate.class);
    controller = new ChatMessageWebSocketController(
        chatMessageCommandService,
        mock(ChatReadService.class),
        chatRoomService,
        messagingTemplate
    );
  }

  @DisplayName("메시지는 수신 대상에게만 개인 경로로 전달되고, 전송자를 차단·신고한 멤버에게는 전달되지 않는다")
  @Test
  void sendMessage_sendsOnlyToRecipients() {
    // given
    when(chatMessageCommandService.sendMessage(SENDER_ID, CHAT_ROOM_ID, "안녕하세요"))
        .thenReturn(new ChatMessageSendResult(createMessage()));
    when(chatRoomService.getMessageRecipientIds(CHAT_ROOM_ID, SENDER_ID))
        .thenReturn(List.of(SENDER_ID));
    when(chatRoomService.getChatRoomListItemForNotification(anyLong(), eq(CHAT_ROOM_ID)))
        .thenReturn(createChatRoomListItem());

    // when
    controller.sendMessage(
        CHAT_ROOM_ID,
        new ChatMessageSendRequest("안녕하세요"),
        principalOf(SENDER_ID)
    );

    // then
    verify(messagingTemplate).convertAndSendToUser(
        eq(String.valueOf(SENDER_ID)),
        eq("/sub/chat-rooms/" + CHAT_ROOM_ID),
        any(ChatMessageEventResponse.class)
    );
    verify(messagingTemplate, never()).convertAndSendToUser(
        eq(String.valueOf(BLOCKER_ID)),
        anyString(),
        any(Object.class)
    );
    verify(messagingTemplate, never()).convertAndSend(anyString(), any(Object.class));
    verify(chatRoomService).getChatRoomListItemForNotification(SENDER_ID, CHAT_ROOM_ID);
    verify(chatRoomService, never()).getChatRoomListItemForNotification(BLOCKER_ID, CHAT_ROOM_ID);
  }

  private ChatMessage createMessage() {
    ChatRoom chatRoom = ChatRoom.createDirect(SENDER_ID + ":" + BLOCKER_ID);
    ReflectionTestUtils.setField(chatRoom, "id", CHAT_ROOM_ID);

    User sender = User.builder()
        .email("sender@test.com")
        .provider(AuthProvider.KAKAO)
        .providerId("sender-provider-id")
        .nickname("전송자")
        .build();
    ReflectionTestUtils.setField(sender, "id", SENDER_ID);

    ChatMessage message = new ChatMessage(chatRoom, sender, "안녕하세요");
    ReflectionTestUtils.setField(message, "id", 101L);
    ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.of(2026, 7, 9, 12, 0));
    return message;
  }

  private ChatRoomListItemResult createChatRoomListItem() {
    return new ChatRoomListItemResult(
        CHAT_ROOM_ID,
        BLOCKER_ID,
        "차단한사용자",
        null,
        "안녕하세요",
        LocalDateTime.of(2026, 7, 9, 12, 0),
        0L
    );
  }

  private Principal principalOf(Long userId) {
    return () -> String.valueOf(userId);
  }
}
