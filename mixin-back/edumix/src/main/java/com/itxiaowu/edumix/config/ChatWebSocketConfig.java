package com.itxiaowu.edumix.config;

import com.itxiaowu.edumix.security.DatabaseUserDetailsService;
import com.itxiaowu.edumix.security.JwtService;
import com.itxiaowu.edumix.security.TokenBlacklist;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/** 私信 WebSocket 配置：注册握手端点、消息代理前缀，并在握手时校验 JWT。 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class ChatWebSocketConfig implements WebSocketMessageBrokerConfigurer {
  private final JwtService jwtService;
  private final TokenBlacklist tokenBlacklist;
  private final DatabaseUserDetailsService users;

  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry
        .addEndpoint("/ws/chat")
        .setAllowedOrigins("http://localhost:5173", "http://127.0.0.1:5173");
  }

  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    registry.enableSimpleBroker("/queue");
    registry.setUserDestinationPrefix("/user");
  }

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(
        new ChannelInterceptor() {
          @Override
          public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            if (accessor == null) return message;
            if (accessor.getCommand() == StompCommand.CONNECT) {
              String header = accessor.getFirstNativeHeader("Authorization");
              if (header == null || !header.startsWith("Bearer "))
                throw new AccessDeniedException("私信连接需要登录");
              try {
                Claims claims = jwtService.parse(header.substring(7));
                if (tokenBlacklist.isRevoked(
                    claims.get("uid", Long.class), claims.get("tv", Integer.class)))
                  throw new AccessDeniedException("登录状态已失效");
                UserDetails user = users.loadUserByUsername(claims.getSubject());
                if (!user.isEnabled()
                    || user.getAuthorities().stream()
                        .noneMatch(a -> "ROLE_TEACHER".equals(a.getAuthority())))
                  throw new AccessDeniedException("仅教师可使用私信");
                accessor.setUser(
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
              } catch (Exception e) {
                throw new AccessDeniedException("私信连接认证失败", e);
              }
            } else if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
              if (accessor.getUser() == null
                  || !"/user/queue/chat".equals(accessor.getDestination()))
                throw new AccessDeniedException("只能订阅自己的私信通道");
            } else if (accessor.getCommand() == StompCommand.SEND) {
              throw new AccessDeniedException("请通过 HTTP 接口发送私信");
            }
            return message;
          }
        });
  }
}
