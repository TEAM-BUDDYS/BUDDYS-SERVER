package org.sopt.buddys.global.websocket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.io.IOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

class ActiveWebSocketSessionRegistryTest {

  private final ActiveWebSocketSessionRegistry sessionRegistry =
      new ActiveWebSocketSessionRegistry();

  @DisplayName("사용자에게 연결된 모든 WebSocket 세션을 종료한다")
  @Test
  void disconnectUser_multipleSessions_closesAllSessions() throws IOException {
    // given
    Long userId = 1L;
    WebSocketSession firstSession = session("session-1");
    WebSocketSession secondSession = session("session-2");
    sessionRegistry.register(firstSession);
    sessionRegistry.register(secondSession);
    sessionRegistry.bindUser(firstSession.getId(), userId);
    sessionRegistry.bindUser(secondSession.getId(), userId);

    // when
    sessionRegistry.disconnectUser(userId);

    // then
    then(firstSession).should().close(CloseStatus.POLICY_VIOLATION);
    then(secondSession).should().close(CloseStatus.POLICY_VIOLATION);
  }

  @DisplayName("이미 해제된 WebSocket 세션은 사용자 탈퇴 시 다시 종료하지 않는다")
  @Test
  void disconnectUser_unregisteredSession_doesNotCloseSession() throws IOException {
    // given
    Long userId = 1L;
    WebSocketSession session = session("session-1");
    sessionRegistry.register(session);
    sessionRegistry.bindUser(session.getId(), userId);
    sessionRegistry.unregister(session.getId());

    // when
    sessionRegistry.disconnectUser(userId);

    // then
    then(session).should(never()).close(any(CloseStatus.class));
  }

  private WebSocketSession session(String sessionId) {
    WebSocketSession session = org.mockito.Mockito.mock(WebSocketSession.class);
    given(session.getId()).willReturn(sessionId);
    given(session.isOpen()).willReturn(true);
    return session;
  }
}
