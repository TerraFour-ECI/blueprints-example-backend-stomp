package com.eci.blueprints.rt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtRoomAuthorizationInterceptorTest {

  private HttpServer tokenValidationServer;
  private String validationUrl;

  @BeforeEach
  void setup() throws Exception {
    tokenValidationServer = HttpServer.create(new InetSocketAddress(0), 0);
    tokenValidationServer.createContext("/api/blueprints", exchange -> {
      String auth = exchange.getRequestHeaders().getFirst("Authorization");
      boolean isValid = false;
      if (auth != null && auth.startsWith("Bearer ")) {
        String token = auth.substring("Bearer ".length());
        String[] parts = token.split("\\.");
        isValid = parts.length >= 3 && "valid-signature".equals(parts[2]);
      }

      if (isValid) {
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
      } else {
        byte[] body = "{\"error\":\"invalid\"}".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(401, body.length);
        exchange.getResponseBody().write(body);
      }
      exchange.close();
    });
    tokenValidationServer.start();
    validationUrl = "http://localhost:" + tokenValidationServer.getAddress().getPort() + "/api/blueprints";
  }

  @AfterEach
  void tearDown() {
    if (tokenValidationServer != null) {
      tokenValidationServer.stop(0);
    }
  }

  @Test
  void shouldAcceptValidTokenOnConnect() {
    JwtRoomAuthorizationInterceptor interceptor = interceptor();
    String token = createJwt("juan", true);

    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
    accessor.setNativeHeader("Authorization", "Bearer " + token);
    accessor.setLeaveMutable(true);
    Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

    Message<?> result = interceptor.preSend(message, null);
    StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
    assertNotNull(resultAccessor.getUser());
  }

  @Test
  void shouldRejectInvalidTokenOnConnect() {
    JwtRoomAuthorizationInterceptor interceptor = interceptor();
    String token = createJwt("juan", false);

    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
    accessor.setNativeHeader("Authorization", "Bearer " + token);
    accessor.setLeaveMutable(true);
    Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

    assertThrows(IllegalStateException.class, () -> interceptor.preSend(message, null));
  }

  @Test
  void shouldRejectSubscribeForForeignAuthor() {
    JwtRoomAuthorizationInterceptor interceptor = interceptor();

    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setDestination("/topic/blueprints.maria.bp-1");
    accessor.setUser(new UsernamePasswordAuthenticationToken("juan", "n/a"));
    accessor.setLeaveMutable(true);
    Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

    assertThrows(MessageDeliveryException.class, () -> interceptor.preSend(message, null));
  }

  @Test
  void shouldAllowSubscribeForMatchingAuthor() {
    JwtRoomAuthorizationInterceptor interceptor = interceptor();

    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setDestination("/topic/blueprints.juan.bp-1");
    accessor.setUser(new UsernamePasswordAuthenticationToken("juan", "n/a"));
    accessor.setLeaveMutable(true);
    Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

    assertDoesNotThrow(() -> interceptor.preSend(message, null));
  }

  private JwtRoomAuthorizationInterceptor interceptor() {
    JwtRoomAuthorizationInterceptor interceptor = new JwtRoomAuthorizationInterceptor(new ObjectMapper());
    ReflectionTestUtils.setField(interceptor, "jwtEnabled", true);
    ReflectionTestUtils.setField(interceptor, "enforceOwner", true);
    ReflectionTestUtils.setField(interceptor, "validationUrl", validationUrl);
    ReflectionTestUtils.setField(interceptor, "adminUsersRaw", "");
    return interceptor;
  }

  private String createJwt(String subject, boolean validMarker) {
    String header = b64Url("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
    long now = Instant.now().getEpochSecond();
    String payload = b64Url("{\"sub\":\"" + subject + "\",\"iat\":" + now + ",\"exp\":" + (now + 300) + "}");
    String signature = validMarker ? "valid-signature" : "invalid-signature";
    return header + "." + payload + "." + signature;
  }

  private String b64Url(String value) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }
}
