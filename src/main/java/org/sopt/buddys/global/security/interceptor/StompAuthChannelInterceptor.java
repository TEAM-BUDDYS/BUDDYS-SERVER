package org.sopt.buddys.global.security.interceptor;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.sopt.buddys.domain.chat.entity.ChatRoomMemberId;
import org.sopt.buddys.domain.chat.repository.ChatRoomMemberRepository;
import org.sopt.buddys.global.security.jwt.JwtProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

  private static final String BEARER_PREFIX = "Bearer ";
  private static final Pattern CHAT_ROOM_SUBSCRIBE_DESTINATION = Pattern.compile(
      "^/sub/chat-rooms/(\\d+)$"
  );

  private final JwtProvider jwtProvider;
  private final ChatRoomMemberRepository chatRoomMemberRepository;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
        message,
        StompHeaderAccessor.class
    );

    if (accessor == null) {
      return message;
    }

    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
      authenticateConnect(accessor);
      return message;
    }

    if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
      authorizeSubscribe(accessor);
    }

    return message;
  }

  private void authenticateConnect(StompHeaderAccessor accessor) {
    String authorization = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
    if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
      throw new BadCredentialsException("WebSocket access token is required.");
    }

    String accessToken = authorization.substring(BEARER_PREFIX.length());
    Long userId = jwtProvider.extractUserId(accessToken)
        .orElseThrow(() -> new BadCredentialsException("Invalid WebSocket access token."));

    Authentication authentication = new UsernamePasswordAuthenticationToken(
        userId,
        null,
        Collections.emptyList()
    );
    accessor.setUser(authentication);
  }

  private void authorizeSubscribe(StompHeaderAccessor accessor) {
    Matcher matcher = CHAT_ROOM_SUBSCRIBE_DESTINATION.matcher(
        String.valueOf(accessor.getDestination())
    );

    if (!matcher.matches()) {
      return;
    }

    Long chatRoomId = Long.valueOf(matcher.group(1));
    Long userId = getAuthenticatedUserId(accessor);

    if (!chatRoomMemberRepository.existsById(new ChatRoomMemberId(chatRoomId, userId))) {
      throw new AccessDeniedException("Cannot subscribe to chat room.");
    }
  }

  private Long getAuthenticatedUserId(StompHeaderAccessor accessor) {
    if (accessor.getUser() instanceof Authentication authentication
        && authentication.getPrincipal() instanceof Long userId) {
      return userId;
    }

    throw new BadCredentialsException("WebSocket authentication is required.");
  }
}
