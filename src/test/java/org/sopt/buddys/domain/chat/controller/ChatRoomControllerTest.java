package org.sopt.buddys.domain.chat.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sopt.buddys.domain.chat.entity.ChatMessage;
import org.sopt.buddys.domain.chat.entity.ChatRoom;
import org.sopt.buddys.domain.chat.service.ChatMessageService;
import org.sopt.buddys.domain.chat.service.ChatRoomService;
import org.sopt.buddys.domain.chat.service.result.ChatMessageListResult;
import org.sopt.buddys.domain.chat.service.result.ChatMessageListResult.ChatMessageResult;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.global.security.annotation.LoginUser;
import org.springframework.core.MethodParameter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

class ChatRoomControllerTest {

  private static final long LOGIN_USER_ID = 1L;

  private ChatMessageService chatMessageService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    ChatRoomService chatRoomService = mock(ChatRoomService.class);
    chatMessageService = mock(ChatMessageService.class);

    mockMvc = MockMvcBuilders
        .standaloneSetup(new ChatRoomController(chatRoomService, chatMessageService))
        .setCustomArgumentResolvers(new TestLoginUserArgumentResolver())
        .build();
  }

  @DisplayName("메시지 목록 조회는 UTC offset 커서를 파싱하고 UTC offset 시간으로 응답한다")
  @Test
  void getMessages_withUtcCursor_returnsUtcOffsetTimes() throws Exception {
    // given
    Long chatRoomId = 1L;
    Long cursorMessageId = 101L;
    LocalDateTime sentAt = LocalDateTime.of(2026, 7, 7, 14, 30);
    ChatMessage message = createMessage(chatRoomId, 101L, sentAt);

    when(chatMessageService.getMessages(
        eq(LOGIN_USER_ID),
        eq(chatRoomId),
        eq(sentAt),
        eq(cursorMessageId),
        eq(30)
    )).thenReturn(new ChatMessageListResult(
        List.of(new ChatMessageResult(message, false, true)),
        sentAt,
        cursorMessageId,
        true
    ));

    // when & then
    mockMvc.perform(get("/api/v1/chat-rooms/{chatRoomId}/messages", chatRoomId)
            .param("cursorSentAt", "2026-07-07T14:30:00Z")
            .param("cursorMessageId", cursorMessageId.toString())
            .param("size", "30"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.messages[0].sentAt").value("2026-07-07T14:30:00Z"))
        .andExpect(jsonPath("$.data.nextCursorSentAt").value("2026-07-07T14:30:00Z"));

    verify(chatMessageService).getMessages(
        LOGIN_USER_ID,
        chatRoomId,
        sentAt,
        cursorMessageId,
        30
    );
  }

  private ChatMessage createMessage(
      Long chatRoomId,
      Long messageId,
      LocalDateTime sentAt
  ) {

    ChatRoom chatRoom = ChatRoom.createDirect("1:2");
    ReflectionTestUtils.setField(chatRoom, "id", chatRoomId);

    User sender = User.builder()
        .email("sender@test.com")
        .provider(AuthProvider.KAKAO)
        .providerId("sender-provider-id")
        .nickname("민지")
        .profileImageUrl("https://example.com/profile.png")
        .build();
    ReflectionTestUtils.setField(sender, "id", 2L);

    ChatMessage message = new ChatMessage(chatRoom, sender, "안녕하세요!");
    ReflectionTestUtils.setField(message, "id", messageId);
    ReflectionTestUtils.setField(message, "createdAt", sentAt);
    return message;
  }

  private static class TestLoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
      return parameter.hasParameterAnnotation(LoginUser.class)
          && parameter.getParameterType().equals(Long.class);
    }

    @Override
    public Object resolveArgument(
        MethodParameter parameter,
        ModelAndViewContainer mavContainer,
        NativeWebRequest webRequest,
        WebDataBinderFactory binderFactory
    ) {

      return LOGIN_USER_ID;
    }
  }
}
