package org.sopt.buddys.global.websocket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

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

    sessionRegistry.disconnectUser(userId);
    then(firstSession).should(times(1)).close(CloseStatus.POLICY_VIOLATION);
    then(secondSession).should(times(1)).close(CloseStatus.POLICY_VIOLATION);
  }

  @DisplayName("WebSocket 세션 종료에 실패하면 매핑을 유지해 다음 정리에서 다시 종료한다")
  @Test
  void disconnectUser_closeFails_retriesOnNextCleanup() throws IOException {
    // given
    Long userId = 1L;
    WebSocketSession session = session("session-1");
    sessionRegistry.register(session);
    sessionRegistry.bindUser(session.getId(), userId);
    doThrow(new IOException("close failed"))
        .doNothing()
        .when(session).close(CloseStatus.POLICY_VIOLATION);

    // when
    sessionRegistry.disconnectUser(userId);
    sessionRegistry.disconnectUser(userId);

    // then
    then(session).should(times(2)).close(CloseStatus.POLICY_VIOLATION);
  }

  @DisplayName("세션 상태 조회에서 예외가 나도 다른 세션을 종료하고 실패한 세션은 다음 정리에서 재시도한다")
  @Test
  void disconnectUser_isOpenFails_keepsMappingAndContinues() throws IOException {
    Long userId = 1L;
    WebSocketSession failingSession = session("session-failing");
    WebSocketSession healthySession = session("session-healthy");
    sessionRegistry.register(failingSession);
    sessionRegistry.register(healthySession);
    sessionRegistry.bindUser(failingSession.getId(), userId);
    sessionRegistry.bindUser(healthySession.getId(), userId);
    doThrow(new IllegalStateException("status unavailable"))
        .doReturn(true)
        .when(failingSession).isOpen();

    sessionRegistry.disconnectUser(userId);
    sessionRegistry.disconnectUser(userId);

    then(failingSession).should(times(1)).close(CloseStatus.POLICY_VIOLATION);
    then(healthySession).should(times(1)).close(CloseStatus.POLICY_VIOLATION);
  }

  @DisplayName("세션 종료에서 런타임 예외가 나도 매핑을 유지하고 다른 세션 종료를 계속한다")
  @Test
  void disconnectUser_closeThrowsRuntimeException_keepsMappingAndContinues() throws IOException {
    Long userId = 1L;
    WebSocketSession failingSession = session("session-failing");
    WebSocketSession healthySession = session("session-healthy");
    sessionRegistry.register(failingSession);
    sessionRegistry.register(healthySession);
    sessionRegistry.bindUser(failingSession.getId(), userId);
    sessionRegistry.bindUser(healthySession.getId(), userId);
    doThrow(new IllegalStateException("close failed"))
        .doNothing()
        .when(failingSession).close(CloseStatus.POLICY_VIOLATION);

    sessionRegistry.disconnectUser(userId);
    sessionRegistry.disconnectUser(userId);

    then(failingSession).should(times(2)).close(CloseStatus.POLICY_VIOLATION);
    then(healthySession).should(times(1)).close(CloseStatus.POLICY_VIOLATION);
  }

  @DisplayName("이미 닫힌 세션은 종료 요청 없이 매핑을 제거한다")
  @Test
  void disconnectUser_closedSession_removesMappingWithoutClosing() throws IOException {
    Long userId = 1L;
    WebSocketSession closedSession = session("session-closed");
    doReturn(false).when(closedSession).isOpen();
    sessionRegistry.register(closedSession);
    sessionRegistry.bindUser(closedSession.getId(), userId);

    sessionRegistry.disconnectUser(userId);
    sessionRegistry.disconnectUser(userId);

    then(closedSession).should(never()).close(any(CloseStatus.class));
    then(closedSession).should(times(1)).isOpen();
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
