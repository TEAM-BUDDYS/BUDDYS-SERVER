package org.sopt.buddys.global.websocket;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@Component
public class ActiveWebSocketSessionRegistry {

  private final Map<String, WebSocketSession> sessions = new HashMap<>();
  private final Map<String, Long> sessionUsers = new HashMap<>();
  private final Map<Long, Set<String>> userSessions = new HashMap<>();

  public synchronized void register(WebSocketSession session) {
    sessions.put(session.getId(), session);
  }

  public synchronized void bindUser(String sessionId, Long userId) {
    if (!sessions.containsKey(sessionId)) {
      return;
    }

    Long previousUserId = sessionUsers.put(sessionId, userId);
    if (previousUserId != null && !previousUserId.equals(userId)) {
      removeUserSession(previousUserId, sessionId);
    }
    userSessions.computeIfAbsent(userId, ignored -> new HashSet<>()).add(sessionId);
  }

  public synchronized void unregister(String sessionId) {
    sessions.remove(sessionId);
    Long userId = sessionUsers.remove(sessionId);
    if (userId != null) {
      removeUserSession(userId, sessionId);
    }
  }

  public void disconnectUser(Long userId) {
    Set<WebSocketSession> sessionsToClose = removeUserSessions(userId);
    sessionsToClose.forEach(this::closeSession);
  }

  private synchronized Set<WebSocketSession> removeUserSessions(Long userId) {
    Set<String> sessionIds = userSessions.remove(userId);
    if (sessionIds == null || sessionIds.isEmpty()) {
      return Set.of();
    }

    Set<WebSocketSession> sessionsToClose = new HashSet<>();
    for (String sessionId : sessionIds) {
      sessionUsers.remove(sessionId);
      WebSocketSession session = sessions.remove(sessionId);
      if (session != null) {
        sessionsToClose.add(session);
      }
    }
    return sessionsToClose;
  }

  private void removeUserSession(Long userId, String sessionId) {
    Set<String> sessionIds = userSessions.get(userId);
    if (sessionIds == null) {
      return;
    }
    sessionIds.remove(sessionId);
    if (sessionIds.isEmpty()) {
      userSessions.remove(userId);
    }
  }

  private void closeSession(WebSocketSession session) {
    if (!session.isOpen()) {
      return;
    }

    try {
      session.close(CloseStatus.POLICY_VIOLATION);
    } catch (IOException e) {
      log.warn("[WebSocket] 탈퇴 사용자 세션 종료 실패 sessionId={}", session.getId(), e);
    }
  }
}
