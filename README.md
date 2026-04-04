# STOMP Backend - Render-Safe and Production-Style Documentation

<div align="center">

![Java](https://img.shields.io/badge/Java-21-007396?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-16a34a?style=for-the-badge&logo=springboot&logoColor=white)
![STOMP](https://img.shields.io/badge/STOMP-Topic_Broadcast-0ea5e9?style=for-the-badge)
![Maven](https://img.shields.io/badge/Maven-Verify_CI-c2410c?style=for-the-badge&logo=apachemaven&logoColor=white)

Spring Boot realtime backend for Lab P4 with topic-based collaborative drawing.

</div>

---

## Table of contents

- [Purpose](#purpose)
- [Core capabilities](#core-capabilities)
- [Architecture](#architecture)
- [Message flow](#message-flow)
- [Run guide](#run-guide)
- [Build verification](#build-verification)
- [Frontend integration](#frontend-integration)
- [Screenshot evidence kit](#screenshot-evidence-kit)

---

## Purpose

This project implements the STOMP model for realtime collaboration:

- browser clients connect through WebSocket endpoint
- clients publish draw events
- backend routes updates to topic subscribers

---

## Core capabilities

- WebSocket endpoint: `/ws-blueprints`
- Publish endpoint: `/app/draw`
- Topic stream: `/topic/blueprints.{author}.{name}`
- Payload relay for live canvas synchronization

---

## Architecture

```mermaid
flowchart LR
  FE["React Frontend"] -->|"WebSocket STOMP"| WS["Endpoint /ws-blueprints"]
  FE -->|"SEND /app/draw"| CTRL["MessageMapping draw"]
  CTRL -->|"convertAndSend"| TOPIC["Topic /topic/blueprints.author.name"]
  TOPIC -->|"MESSAGE updates"| FE
```

### Why this Mermaid is render-safe

- Quoted node labels avoid parser conflicts.
- No special symbols in node IDs.
- Connection labels are wrapped in quotes.

---

## Message flow

```mermaid
sequenceDiagram
  autonumber
  participant A as Frontend Tab A
  participant B as Frontend Tab B
  participant S as Spring STOMP Backend

  A->>S: CONNECT /ws-blueprints
  B->>S: CONNECT /ws-blueprints
  A->>S: SUBSCRIBE /topic/blueprints.juan.blueprint-1
  B->>S: SUBSCRIBE /topic/blueprints.juan.blueprint-1
  A->>S: SEND /app/draw {author,name,point}
  S-->>A: MESSAGE /topic/blueprints.juan.blueprint-1
  S-->>B: MESSAGE /topic/blueprints.juan.blueprint-1
```

---

## Run guide

```bash
mvn spring-boot:run
```

Defaults:

- HTTP base: `http://localhost:8080`
- WebSocket endpoint: `/ws-blueprints`

---

## Build verification

```bash
mvn -B verify
```

---

## Frontend integration

Use in front-end `.env.local`:

```bash
VITE_STOMP_BASE=http://localhost:8080
```

Then choose **STOMP (Spring)** in the front-end transport selector.

---

## Screenshot evidence kit

| File name | Recommended capture |
|---|---|
| `stomp-01-spring-startup.png` | Spring startup logs with WebSocket config |
| `stomp-02-stomp-connect.png` | Browser network showing STOMP connection |
| `stomp-03-topic-subscribe.png` | Subscription frame to topic |
| `stomp-04-draw-send-frame.png` | SEND frame to /app/draw |
| `stomp-05-topic-message-frame.png` | MESSAGE frame received from topic |
| `stomp-06-two-tabs-sync.png` | Two tabs showing synchronized canvas |
| `stomp-07-maven-verify-pass.png` | Maven verify successful output |
| `stomp-08-sonar-pass.png` | SonarCloud workflow passed |

---

## License

MIT [LICENSE](LICENSE)
