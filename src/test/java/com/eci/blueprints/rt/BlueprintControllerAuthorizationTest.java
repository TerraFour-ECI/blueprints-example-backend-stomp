package com.eci.blueprints.rt;

import com.eci.blueprints.rt.dto.DrawEvent;
import com.eci.blueprints.rt.dto.Point;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class BlueprintControllerAuthorizationTest {

  private SimpMessagingTemplate template;
  private BlueprintController controller;

  @BeforeEach
  void setup() {
    template = mock(SimpMessagingTemplate.class);
    controller = new BlueprintController(template);
    ReflectionTestUtils.setField(controller, "enforceOwner", true);
  }

  @Test
  void shouldBroadcastWhenAuthorMatchesPrincipal() {
    Principal principal = new UsernamePasswordAuthenticationToken("juan", "n/a");
    DrawEvent evt = new DrawEvent("juan", "bp-1", new Point(10, 20));

    controller.onDraw(evt, principal);

    verify(template, times(1)).convertAndSend(eq("/topic/blueprints.juan.bp-1"), any(Object.class));
  }

  @Test
  void shouldRejectWhenPrincipalTargetsAnotherAuthor() {
    Principal principal = new UsernamePasswordAuthenticationToken("juan", "n/a");
    DrawEvent evt = new DrawEvent("maria", "bp-1", new Point(10, 20));

    controller.onDraw(evt, principal);

    verify(template, never()).convertAndSend(anyString(), any(Object.class));
  }

  @Test
  void shouldAllowAdminPrincipalForForeignAuthor() {
    Principal principal = new UsernamePasswordAuthenticationToken(
        "admin",
        "n/a",
        List.of(new SimpleGrantedAuthority("ROLE_RT_ADMIN"))
    );
    DrawEvent evt = new DrawEvent("maria", "bp-1", new Point(10, 20));

    controller.onDraw(evt, principal);

    verify(template, times(1)).convertAndSend(eq("/topic/blueprints.maria.bp-1"), any(Object.class));
  }

  @Test
  void shouldRejectInvalidPayload() {
    Principal principal = new UsernamePasswordAuthenticationToken("juan", "n/a");
    DrawEvent evt = new DrawEvent("juan", "bp-1", null);

    controller.onDraw(evt, principal);

    verify(template, never()).convertAndSend(anyString(), any(Object.class));
  }

  @Test
  void shouldAllowWhenOwnerEnforcementDisabled() {
    ReflectionTestUtils.setField(controller, "enforceOwner", false);
    DrawEvent evt = new DrawEvent("maria", "bp-1", new Point(11, 22));

    controller.onDraw(evt, null);

    verify(template, times(1)).convertAndSend(eq("/topic/blueprints.maria.bp-1"), any(Object.class));
  }

  @Test
  void shouldReturnSampleBlueprintViaGet() {
    var response = controller.get("juan", "bp-1");
    assertEquals("juan", response.author());
    assertEquals("bp-1", response.name());
    assertEquals(2, response.points().size());
  }
}
