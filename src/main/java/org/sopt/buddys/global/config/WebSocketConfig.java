package org.sopt.buddys.global.config;

import lombok.RequiredArgsConstructor;
import org.sopt.buddys.global.exception.StompErrorHandler;
import org.sopt.buddys.global.security.interceptor.StompAuthChannelInterceptor;
import org.sopt.buddys.global.websocket.ActiveWebSocketSessionRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
  private final StompErrorHandler stompErrorHandler;
  private final ActiveWebSocketSessionRegistry sessionRegistry;

  @Value("${cors.allowed-origins}")
  private String allowedOrigins;

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry.addEndpoint("/ws")
        .setAllowedOriginPatterns(allowedOrigins);
    registry.setErrorHandler(stompErrorHandler);
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/sub", "/user");
    registry.setApplicationDestinationPrefixes("/pub");
    registry.setUserDestinationPrefix("/user");
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(stompAuthChannelInterceptor);
  }

  @Override
  public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
    registration.addDecoratorFactory(handler -> new WebSocketHandlerDecorator(handler) {
      @Override
      public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessionRegistry.register(session);
        try {
          super.afterConnectionEstablished(session);
        } catch (Exception e) {
          sessionRegistry.unregister(session.getId());
          throw e;
        }
      }

      @Override
      public void afterConnectionClosed(
          WebSocketSession session,
          CloseStatus closeStatus
      ) throws Exception {
        sessionRegistry.unregister(session.getId());
        super.afterConnectionClosed(session, closeStatus);
      }
    });
  }
}
