# backend-stomp-spring

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

It integrates with frontend repo: [DECSIS-ECI/Lab_P4_BluePrints_RealTime-Sokets](https://github.com/DECSIS-ECI/Lab_P4_BluePrints_RealTime-Sokets).

---

## 🧩 Core contracts

- WebSocket endpoint: `/ws-blueprints`
- Publish destination: `/app/draw`
- Subscribe destination: `/topic/blueprints.{author}.{name}`

**Recommended channel convention**
- `blueprints.{author}.{name}`

---

## 🏗️ Architecture

```text
React Frontend --(STOMP over WS)--> /ws-blueprints
React Frontend --(SEND /app/draw)--> @MessageMapping("/draw")
Spring Broker --(MESSAGE)--> /topic/blueprints.{author}.{name}
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

In [DECSIS-ECI/Lab_P4_BluePrints_RealTime-Sokets](https://github.com/DECSIS-ECI/Lab_P4_BluePrints_RealTime-Sokets), configure:

```bash
VITE_API_BASE=http://localhost:8080
VITE_STOMP_BASE=http://localhost:8081
```

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
