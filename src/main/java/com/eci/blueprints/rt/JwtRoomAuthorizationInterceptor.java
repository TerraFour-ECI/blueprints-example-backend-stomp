package com.eci.blueprints.rt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JwtRoomAuthorizationInterceptor implements ChannelInterceptor {

  private static final Logger LOGGER = LoggerFactory.getLogger(JwtRoomAuthorizationInterceptor.class);

  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  @Value("${app.security.jwt.enabled:true}")
  private boolean jwtEnabled;

  @Value("${app.security.jwt.enforce-owner:true}")
  private boolean enforceOwner;

  @Value("${app.security.jwt.validation-url:http://localhost:8080/api/blueprints}")
  private String validationUrl;

  @Value("${app.security.jwt.admin-users:}")
  private String adminUsersRaw;

  public JwtRoomAuthorizationInterceptor(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.restClient = RestClient.builder()
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    if (accessor == null || accessor.getCommand() == null) {
      return message;
    }

    try {
      accessor.setLeaveMutable(true);
    } catch (IllegalStateException ignored) {
      // Some tests/builders may provide immutable headers; continue with available mutability.
    }

    if (!jwtEnabled) {
      return message;
    }

    StompCommand command = accessor.getCommand();

    if (StompCommand.CONNECT.equals(command)) {
      String token = extractBearer(accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION));
      if (token == null) {
        throw new MessageDeliveryException("Missing Bearer token in STOMP CONNECT");
      }

      validateTokenWithSecurityApi(token);
      Map<String, Object> claims = decodeClaims(token);
      String subject = String.valueOf(claims.getOrDefault("sub", "")).trim();
      if (subject.isEmpty()) {
        throw new MessageDeliveryException("JWT subject is missing");
      }

      Instant expiresAt = resolveExpiry(claims.get("exp"));
      if (expiresAt != null && Instant.now().isAfter(expiresAt)) {
        throw new MessageDeliveryException("JWT is expired");
      }

      Set<String> adminUsers = parseAdminUsers();
      boolean isAdmin = adminUsers.contains(subject);

      List<GrantedAuthority> authorities = new ArrayList<>();
      authorities.add(new SimpleGrantedAuthority("ROLE_RT_USER"));
      if (isAdmin) {
        authorities.add(new SimpleGrantedAuthority("ROLE_RT_ADMIN"));
      }

      Authentication auth = new UsernamePasswordAuthenticationToken(subject, "n/a", authorities);
      accessor.setUser(auth);
      LOGGER.info("stomp connect authorized user={} admin={}", subject, isAdmin);
      return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
    }

    if (StompCommand.SUBSCRIBE.equals(command)) {
      enforceDestinationAuthorization(accessor);
      return message;
    }

    return message;
  }

  private void enforceDestinationAuthorization(StompHeaderAccessor accessor) {
    String destination = accessor.getDestination();
    if (destination == null || !destination.startsWith("/topic/blueprints.")) {
      return;
    }

    Authentication auth = accessor.getUser() instanceof Authentication a ? a : null;
    if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
      throw new MessageDeliveryException("Missing authenticated principal for SUBSCRIBE");
    }

    String author = extractAuthorFromTopic(destination);
    if (author == null || author.isBlank()) {
      throw new MessageDeliveryException("Invalid blueprint topic destination");
    }

    boolean isAdmin = auth.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch("ROLE_RT_ADMIN"::equals);

    if (enforceOwner && !isAdmin && !author.equals(auth.getName())) {
      throw new MessageDeliveryException("Not authorized to subscribe to this blueprint topic");
    }

    LOGGER.info("stomp subscribe authorized user={} destination={}", auth.getName(), destination);
  }

  private void validateTokenWithSecurityApi(String token) {
    try {
      restClient.get()
          .uri(validationUrl)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
          .retrieve()
          .toBodilessEntity();
    } catch (Exception ex) {
      throw new IllegalStateException("JWT validation against security API failed", ex);
    }
  }

  private Map<String, Object> decodeClaims(String token) {
    try {
      String[] parts = token.split("\\.");
      if (parts.length < 2) {
        throw new IllegalArgumentException("Invalid JWT format");
      }

      byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
      String json = new String(decoded, StandardCharsets.UTF_8);
      return objectMapper.readValue(json, new TypeReference<>() {});
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to decode JWT claims", ex);
    }
  }

  private Instant resolveExpiry(Object expClaim) {
    if (expClaim == null) return null;

    if (expClaim instanceof Number number) {
      return Instant.ofEpochSecond(number.longValue());
    }

    try {
      return Instant.ofEpochSecond(Long.parseLong(String.valueOf(expClaim)));
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  private String extractBearer(String rawAuthorization) {
    if (rawAuthorization == null || rawAuthorization.isBlank()) {
      return null;
    }

    String[] parts = rawAuthorization.trim().split(" ", 2);
    if (parts.length != 2 || !"bearer".equalsIgnoreCase(parts[0])) {
      return null;
    }

    return parts[1].trim();
  }

  private String extractAuthorFromTopic(String destination) {
    String prefix = "/topic/blueprints.";
    if (!destination.startsWith(prefix)) return null;

    String remainder = destination.substring(prefix.length());
    String[] parts = remainder.split("\\.", 2);
    if (parts.length < 2) return null;
    return parts[0];
  }

  private Set<String> parseAdminUsers() {
    return List.of(adminUsersRaw.split(",")).stream()
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toSet());
  }
}
