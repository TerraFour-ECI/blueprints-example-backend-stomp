package com.eci.blueprints.rt;

import com.eci.blueprints.rt.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.List;

@Controller
public class BlueprintController {

  private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintController.class);

  private final SimpMessagingTemplate template;

  public BlueprintController(SimpMessagingTemplate template) {
    this.template = template;
  }

  @MessageMapping("/draw")
  public void onDraw(DrawEvent evt) {
    if (!isValid(evt)) {
      LOGGER.warn("Rejected draw event due to invalid payload: {}", evt);
      return;
    }

    LOGGER.info(
        "draw event received author={} name={} x={} y={}",
        evt.author(),
        evt.name(),
        evt.point().x(),
        evt.point().y());

    var upd = new BlueprintUpdate(evt.author(), evt.name(), List.of(evt.point()));
    template.convertAndSend("/topic/blueprints." + evt.author() + "." + evt.name(), upd);
    LOGGER.info("broadcasted update to /topic/blueprints.{}.{}", evt.author(), evt.name());
  }

  @ResponseBody
  @GetMapping("/api/blueprints/{author}/{name}")
  public BlueprintUpdate get(@PathVariable String author, @PathVariable String name) {
    return new BlueprintUpdate(author, name, List.of(new Point(10,10), new Point(40,50)));
  }

  private boolean isValid(DrawEvent evt) {
    if (evt == null || evt.point() == null) return false;
    if (evt.author() == null || evt.author().isBlank()) return false;
    if (evt.name() == null || evt.name().isBlank()) return false;
    return Double.isFinite(evt.point().x()) && Double.isFinite(evt.point().y());
  }
}
