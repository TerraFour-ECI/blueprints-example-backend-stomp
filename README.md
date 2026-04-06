# Stomp Spring Backend for BluePrints P4

Spring Boot 3 + STOMP over WebSocket for BluePrints Realtime collaboration.

<div align="center">

![Java](https://img.shields.io/badge/Java-21-007396?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-16a34a?style=for-the-badge&logo=springboot&logoColor=white)
![STOMP](https://img.shields.io/badge/STOMP-Topic_Broadcast-0ea5e9?style=for-the-badge)
![Maven](https://img.shields.io/badge/Maven-Verify-c2410c?style=for-the-badge&logo=apachemaven&logoColor=white)

</div>

---

## 🎯 Purpose

This backend enables STOMP-based realtime collaboration by providing:

- WebSocket endpoint for STOMP clients.
- Message mapping for draw events.
- Topic-based broadcasting by blueprint channel.

It integrates with frontend repo: [TerraFour-ECI/arsw-blueprints-api-realtime-sockets-lab](https://github.com/TerraFour-ECI/arsw-blueprints-api-realtime-sockets-lab).

---

## 🧩 Core contracts

- WebSocket endpoint: `/ws-blueprints`
- Publish destination: `/app/draw`
- Subscribe destination: `/topic/blueprints.{author}.{name}`

**Recommended channel convention**
- `blueprints.{author}.{name}`

---

## 🏗️ Architecture (Advanced Mermaid)

```mermaid
flowchart LR
  FE[Realtime Frontend :5174] -->|WebSocket STOMP connect| WS[/ws-blueprints]
  FE -->|SEND /app/draw\n{author,name,point}| MAP[@MessageMapping("/draw")]
  MAP -->|convertAndSend| TOPIC[/topic/blueprints.author.name]
  TOPIC -->|MESSAGE update| FE2[Peer Tab :5174]
  FE -->|CRUD API calls| SEC[Security API :8080]

  classDef c1 fill:#dbeafe,stroke:#2563eb,stroke-width:2px,color:#1e3a8a;
  classDef c2 fill:#dcfce7,stroke:#16a34a,stroke-width:2px,color:#14532d;
  classDef c3 fill:#fee2e2,stroke:#ef4444,stroke-width:2px,color:#7f1d1d;
  class FE,FE2 c1;
  class WS,MAP,TOPIC c2;
  class SEC c3;
```

---

## 🚀 Run

```bash
mvn spring-boot:run
# http://localhost:8080
# WS endpoint: /ws-blueprints
```

For integrated local flows, this service can run on **8081** while CRUD API remains on **8080**.

---

## 🧪 Frontend integration

In [TerraFour-ECI/arsw-blueprints-api-realtime-sockets-lab](https://github.com/TerraFour-ECI/arsw-blueprints-api-realtime-sockets-lab), configure:

```bash
VITE_API_BASE=http://localhost:8080
VITE_STOMP_BASE=http://localhost:8081
```

Related repositories in the integrated flow:
- JWT frontend: [TerraFour-ECI/arsw-blueprints-api-react-lab](https://github.com/TerraFour-ECI/arsw-blueprints-api-react-lab) (`5173`)
- Security backend: [TerraFour-ECI/arsw-blueprints-api-security-lab](https://github.com/TerraFour-ECI/arsw-blueprints-api-security-lab) (`8080`)

Typical client flow:

```js
client.publish({
  destination: '/app/draw',
  body: JSON.stringify({ author, name, point: { x, y } })
})

client.subscribe(`/topic/blueprints.${author}.${name}`, (msg) => {
  // apply incremental points and repaint canvas
})
```

---

## ⚙️ Notes for robust setup

- Keep `/app` as application prefix and `/topic` for broker destinations.
- Allow dev CORS origins (`http://localhost:5174`, and optionally `http://localhost:5173` for login handoff flows).
- Validate inbound draw payloads before broadcasting.

### How this was implemented in this repository
- Draw event processing and topic broadcast live in `BlueprintController.onDraw`.
- Payload validation was added in `BlueprintController.isValid` to reject malformed events.
- Observability was added with structured logs for draw receive and topic broadcast.
- WebSocket CORS is configurable in `WebSocketConfig` using `app.websocket.allowed-origins`.
- Basic health checks are exposed through Spring Actuator (`/actuator/health`) and port config in `application.yml`.
- JWT authorization is enforced on STOMP `CONNECT` and `SUBSCRIBE` through `JwtRoomAuthorizationInterceptor`.
- Token validation is performed against the security backend using `app.security.jwt.validation-url`.
- Topic ownership is enforced (`blueprints.{author}.{name}`) and draw publish authorization is checked in `BlueprintController.onDraw` using authenticated principal.

---

## 📊 Delivery and Rubric Alignment

- **Functionality**: topic-based isolation and live replication validated in two-tab tests.
- **Technical quality**: clear endpoint contracts and integration instructions.
- **Observability/DX**: startup and messaging evidence included in screenshots.
- **Analysis**: STOMP destination model documented with integrated port strategy (`8081` with security API on `8080`).

---

## 🧪 Automated JWT Authorization Tests

This backend includes automated tests for JWT authorization and author/topic enforcement.

Test classes:
- `src/test/java/com/eci/blueprints/rt/JwtRoomAuthorizationInterceptorTest.java`
- `src/test/java/com/eci/blueprints/rt/BlueprintControllerAuthorizationTest.java`

Covered cases:
- ✅ Valid JWT on STOMP `CONNECT`.
- ✅ Invalid JWT rejected during token validation.
- ✅ Foreign-author topic subscription rejected.
- ✅ Author-matching and admin-authorized publish behavior verified.

Run tests:

```bash
mvn clean test
```

Security baseline in this flow:
- Payload validation for inbound draw events.
- Restricted production origins.
- JWT-based authorization by topic/blueprint ownership.

Practical status in this implementation:
- ✅ Topic isolation by `blueprints.{author}.{name}` is active.
- ✅ Event validation and logging are active.
- ✅ Health endpoint is available through Actuator.
- ✅ JWT topic-level authorization is active (CONNECT/SUBSCRIBE/SEND constraints).

Runtime JWT configuration (`application.yml`):
- `app.security.jwt.enabled=true`
- `app.security.jwt.enforce-owner=true`
- `app.security.jwt.validation-url=http://localhost:8080/api/blueprints`
- `app.security.jwt.admin-users=""` (optional comma-separated admins)

---

## 🩺 Troubleshooting

- **No STOMP messages received:** verify destination prefixes and exact topic string.
- **Connection refused:** confirm the correct port (`8080` default or `8081` integrated flow).
- **UI updates only locally:** ensure both tabs subscribe to the same author/name topic.
- **CORS/preflight issues:** check Spring WebSocket and HTTP CORS configuration.

---

## 📸 Evidence gallery

### 01 - Spring startup
Application boot and endpoint initialization.

![stomp-01-spring-startup](images/stomp-01-spring-startup.png)

### 02 - STOMP connection established
WebSocket/STOMP client connection evidence.

![stomp-02-stomp-connect](images/stomp-02-stomp-connect.png)

### 03 - Topic subscription
Frontend subscribed to the blueprint topic.

![stomp-03-topic-subscribe](images/stomp-03-topic-subscribe.png)

### 04 - Draw SEND frame
Frontend publishes event to `/app/draw`.

![stomp-04-draw-send-frame](images/stomp-04-draw-send-frame.png)

### 05 - MESSAGE frame received
Broadcast from backend to subscribers.

![stomp-05-topic-message-frame](images/stomp-05-topic-message-frame.png)

### 06 - Two-tab synchronization
Visual synchronization proof in STOMP mode.

![stomp-06-two-tabs-sync](images/stomp-06-two-tabs-sync.png)

### 07 - Maven verify pass
Local build verification evidence.

![stomp-07-maven-verify-pass](images/stomp-07-maven-verify-pass.png)

### 08 - Sonar pass
CI quality analysis evidence.

![stomp-08-sonar-pass](images/stomp-08-sonar-pass.png)

---

## 📄 License

MIT [LICENSE](LICENSE)
