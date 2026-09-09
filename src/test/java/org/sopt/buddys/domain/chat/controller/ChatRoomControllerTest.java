package org.sopt.buddys.domain.chat.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.sopt.buddys.domain.chat.service.ChatUserBlockService;
import org.sopt.buddys.domain.chat.service.ChatUserReportService;
import org.sopt.buddys.domain.chat.service.result.ChatMessageListResult;
import org.sopt.buddys.domain.chat.service.result.ChatMessageListResult.ChatMessageResult;
import org.sopt.buddys.domain.user.entity.AuthProvider;
import org.sopt.buddys.domain.user.entity.User;
import org.sopt.buddys.global.exception.GlobalExceptionHandler;
import org.sopt.buddys.global.security.annotation.LoginUser;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

class ChatRoomControllerTest {

  private static final long LOGIN_USER_ID = 1L;

  private ChatMessageService chatMessageService;
  private ChatUserBlockService chatUserBlockService;
  private ChatUserReportService chatUserReportService;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    ChatRoomService chatRoomService = mock(ChatRoomService.class);
    chatMessageService = mock(ChatMessageService.class);
    chatUserBlockService = mock(ChatUserBlockService.class);
    chatUserReportService = mock(ChatUserReportService.class);

    mockMvc = MockMvcBuilders
        .standaloneSetup(new ChatRoomController(
            chatRoomService, chatMessageService, chatUserBlockService, chatUserReportService
        ))
        .setCustomArgumentResolvers(new TestLoginUserArgumentResolver())
        .setControllerAdvice(new GlobalExceptionHandler())
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

    // when
    ResultActions result = mockMvc.perform(get("/api/v1/chat-rooms/{chatRoomId}/messages", chatRoomId)
        .param("cursorSentAt", "2026-07-07T14:30:00Z")
        .param("cursorMessageId", cursorMessageId.toString())
        .param("size", "30"));

    // then
    result
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

  @DisplayName("메시지 목록 첫 페이지 조회는 커서가 없으면 null을 서비스에 전달한다")
  @Test
  void getMessages_withoutCursor_passesNullCursorToService() throws Exception {
    // given
    Long chatRoomId = 1L;

    when(chatMessageService.getMessages(
        eq(LOGIN_USER_ID),
        eq(chatRoomId),
        isNull(),
        isNull(),
        eq(30)
    )).thenReturn(new ChatMessageListResult(
        List.of(),
        null,
        null,
        false
    ));

    // when
    ResultActions result = mockMvc.perform(get("/api/v1/chat-rooms/{chatRoomId}/messages", chatRoomId)
        .param("size", "30"));

    // then
    result
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.messages").isArray())
        .andExpect(jsonPath("$.data.messages.length()").value(0))
        .andExpect(jsonPath("$.data.hasNext").value(false));

    verify(chatMessageService).getMessages(
        eq(LOGIN_USER_ID),
        eq(chatRoomId),
        isNull(),
        isNull(),
        eq(30)
    );
  }

  @DisplayName("채팅방 차단 요청이 오면 서비스에 위임한다")
  @Test
  void blockChatPartner_delegatesToService() throws Exception {
    // given
    Long chatRoomId = 1L;

    // when
    ResultActions result = mockMvc.perform(post("/api/v1/chat-rooms/{chatRoomId}/block", chatRoomId));

    // then
    result.andExpect(status().isOk());
    verify(chatUserBlockService).blockChatPartner(LOGIN_USER_ID, chatRoomId);
  }

  @DisplayName("신고 요청에 바디가 없으면 사유 없이 서비스에 위임한다")
  @Test
  void reportChatPartner_withoutBody_delegatesWithNullReason() throws Exception {
    // given
    Long chatRoomId = 1L;

    // when
    ResultActions result = mockMvc.perform(post("/api/v1/chat-rooms/{chatRoomId}/report", chatRoomId));

    // then
    result.andExpect(status().isOk());
    verify(chatUserReportService).reportChatPartner(LOGIN_USER_ID, chatRoomId, null);
  }

  @DisplayName("신고 요청에 사유가 포함되면 그 사유를 그대로 서비스에 전달한다")
  @Test
  void reportChatPartner_withReason_delegatesWithReason() throws Exception {
    // given
    Long chatRoomId = 1L;

    // when
    ResultActions result = mockMvc.perform(post("/api/v1/chat-rooms/{chatRoomId}/report", chatRoomId)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"reason": "부적절한 언행"}
            """));

    // then
    result.andExpect(status().isOk());
    verify(chatUserReportService).reportChatPartner(LOGIN_USER_ID, chatRoomId, "부적절한 언행");
  }

  @DisplayName("신고 사유가 500자를 초과하면 400을 반환하고 서비스는 호출되지 않는다")
  @Test
  void reportChatPartner_reasonTooLong_returnsBadRequest() throws Exception {
    // given
    Long chatRoomId = 1L;
    String tooLongReason = "a".repeat(501);

    // when
    ResultActions result = mockMvc.perform(post("/api/v1/chat-rooms/{chatRoomId}/report", chatRoomId)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"reason\": \"" + tooLongReason + "\"}"));

    // then
    result
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("GLB-E001"));
    verifyNoInteractions(chatUserReportService);
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
