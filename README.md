# STOMP Backend for BluePrints Real-Time Collaboration

<div align="center">

![Java](https://img.shields.io/badge/Java-21-007396?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![WebSocket](https://img.shields.io/badge/WebSocket-STOMP-0EA5E9?style=for-the-badge)
![Maven](https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)

Reference Spring Boot backend used by the P4 front-end for topic-based real-time synchronization.

</div>

---

## Core capabilities

- STOMP-over-WebSocket endpoint (`/ws-blueprints`).
- Topic-based collaboration per blueprint (`/topic/blueprints.{author}.{name}`).
- Message mapping endpoint for draw events (`/app/draw`).

---

## Architecture

```mermaid
flowchart LR
  FE[React Front-end] -->|WebSocket/STOMP| WS[/ws-blueprints]
  FE -->|publish /app/draw| CTRL[@MessageMapping draw]
  CTRL -->|convertAndSend| TOPIC[/topic/blueprints.author.name]
  TOPIC --> FE
```

### Message sequence

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

## Run locally

```bash
mvn spring-boot:run
```

Defaults:

- HTTP: `http://localhost:8080`
- WS endpoint: `/ws-blueprints`

---

## Build verification

```bash
mvn -B verify
```

---

## Front-end integration

In front-end `.env.local`:

```bash
VITE_STOMP_BASE=http://localhost:8080
```

In the UI, select `STOMP (Spring)` transport.

---

## Screenshot evidence suggestions

1. `stomp-01-app-startup.png`
   - Spring Boot startup logs and mapped WebSocket endpoint.
2. `stomp-02-topic-subscription.png`
   - Browser/network evidence of successful STOMP subscribe.
3. `stomp-03-live-sync-two-tabs.png`
   - Two tabs showing replicated points with STOMP mode.
4. `stomp-04-maven-and-sonar-pass.png`
   - Successful Maven verify + SonarCloud workflow.

---

## License

MIT [LICENSE](LICENSE)
