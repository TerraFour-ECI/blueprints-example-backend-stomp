package com.eci.blueprints.rt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketConfigTest {

  private JwtRoomAuthorizationInterceptor interceptor;
  private WebSocketConfig config;

  @BeforeEach
  void setup() {
    interceptor = mock(JwtRoomAuthorizationInterceptor.class);
    config = new WebSocketConfig(interceptor);
    ReflectionTestUtils.setField(config, "allowedOrigins", "http://localhost:5174,http://localhost:5173");
  }

  @Test
  void shouldRegisterStompEndpointWithConfiguredOrigins() {
    StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
    StompWebSocketEndpointRegistration endpointRegistration = mock(StompWebSocketEndpointRegistration.class);
    when(registry.addEndpoint("/ws-blueprints")).thenReturn(endpointRegistration);

    config.registerStompEndpoints(registry);

    verify(endpointRegistration).setAllowedOriginPatterns("http://localhost:5174", "http://localhost:5173");
  }

  @Test
  void shouldConfigureBrokerPrefixes() {
    MessageBrokerRegistry registry = mock(MessageBrokerRegistry.class);

    config.configureMessageBroker(registry);

    verify(registry).enableSimpleBroker("/topic", "/queue");
    verify(registry).setApplicationDestinationPrefixes("/app");
    verify(registry).setUserDestinationPrefix("/user");
  }

  @Test
  void shouldRegisterJwtInterceptorInInboundChannel() {
    ChannelRegistration registration = mock(ChannelRegistration.class);

    config.configureClientInboundChannel(registration);

    verify(registration).interceptors(interceptor);
  }
}
