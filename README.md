# 📡 STOMP Backend - Final Evidence README

<div align="center">

![Java](https://img.shields.io/badge/Java-21-007396?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-16a34a?style=for-the-badge&logo=springboot&logoColor=white)
![STOMP](https://img.shields.io/badge/STOMP-Topic_Broadcast-0ea5e9?style=for-the-badge)
![Maven](https://img.shields.io/badge/Maven-Verify-c2410c?style=for-the-badge&logo=apachemaven&logoColor=white)

Topic-based realtime backend used by the central P4 frontend.

</div>

---

## 🎯 Purpose

This backend handles STOMP/WebSocket collaboration:

- websocket connection endpoint
- draw event publishing endpoint
- topic broadcasts for blueprint sessions

---

## 🧩 Core contracts

- WebSocket endpoint: `/ws-blueprints`
- Publish destination: `/app/draw`
- Subscribe topic: `/topic/blueprints.{author}.{name}`

---

## 🏗️ Architecture (render-safe Mermaid)

```mermaid
flowchart LR
  FE["Realtime Frontend :5174"] -->|"WebSocket STOMP"| WS["Endpoint /ws-blueprints"]
  FE -->|"SEND /app/draw"| CTRL["MessageMapping draw"]
  CTRL -->|"convertAndSend"| TOPIC["Topic /topic/blueprints.author.name"]
  TOPIC -->|"MESSAGE updates"| FE
```

---

## ▶️ Run

```bash
mvn spring-boot:run
```

Default service URL: `http://localhost:8080`

In your integrated flow you can run this backend on `8081` and point the frontend accordingly.

---

## 📸 Evidence gallery

### 01 - Spring startup
Application boot and endpoint initialization.

![stomp-01-spring-startup](images/stomp-01-spring-startup.png)

### 02 - STOMP connection established
WebSocket/STOMP client connection evidence.

![stomp-02-stomp-connect](images/stomp-02-stomp-connect.png)

### 03 - Topic subscription
Frontend subscribed to blueprint topic.

![stomp-03-topic-subscribe](images/stomp-03-topic-subscribe.png)

### 04 - Draw SEND frame
Frontend publish event to `/app/draw`.

![stomp-04-draw-send-frame](images/stomp-04-draw-send-frame.png)

### 05 - MESSAGE frame received
Broadcast from backend to subscribers.

![stomp-05-topic-message-frame](images/stomp-05-topic-message-frame.png)

### 06 - Two-tab sync
Visual synchronization proof with STOMP mode.

![stomp-06-two-tabs-sync](images/stomp-06-two-tabs-sync.png)

### 07 - Maven verify pass
Local build/verification evidence.

![stomp-07-maven-verify-pass](images/stomp-07-maven-verify-pass.png)

### 08 - Sonar pass
CI quality analysis evidence.

![stomp-08-sonar-pass](images/stomp-08-sonar-pass.png)

---

## 🔗 Integration note

Set this in realtime frontend `.env.local`:

```bash
VITE_STOMP_BASE=http://localhost:8081
```

---

## 📄 License

MIT [LICENSE](LICENSE)
