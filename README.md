# coding-battle-backend

---

# Lobby & Simulation Demo

This is a **Spring Boot** project demonstrating how to create lobbies and run simulations in real-time using *
*Server-Sent Events (SSE)**.  
Players can join a lobby, watch for events, and then see **turn-by-turn** simulation updates once the simulation starts.

## Features

1. **Lobby Management**
    - Create a lobby with a unique ID.
    - Store players in each lobby.
2. **Server-Sent Events**
    - A single SSE endpoint per lobby (`/lobbies/{id}/events`).
    - Sends events for both *lobby* and *simulation* status (e.g., *WAITING*, *SIMULATION_STARTED*, *TURN_UPDATE*,
      *SIMULATION_FINISHED*).
3. **Turn-Based Simulation**
    - Basic logic increments a turn counter every second until a finishing condition is met (e.g., turn >= 5).
    - Streams updates to **all** connected clients in real-time.

---

## Project Structure

```
src
└── main
    ├── java
    │   └── com.example
    │       ├── model
    │       │   ├── Lobby.java
    │       │   ├── Player.java
    │       │   ├── GameState.java
    │       │   ├── LobbyEvent.java
    │       │   └── LobbyEventType.java
    │       ├── service
    │       │   ├── LobbyService.java
    │       │   └── SimulationService.java
    │       └── controller
    │           ├── LobbyController.java
    │           └── SimulationController.java
    └── resources
        └── application.properties
```

### Key Packages

- **`model`**: Data classes (`Lobby`, `GameState`, etc.).
    - `LobbyEventType` – an enum for different event types (WAITING, SIMULATION_STARTED, etc.).
    - `LobbyEvent` – a wrapper for SSE messages (contains `type` and `payload`).
- **`service`**: Business logic.
    - `LobbyService` – handles creation/storage of lobbies and the SSE “sink” for each lobby.
    - `SimulationService` – orchestrates the simulation (turn increments), pushes events, and manages background tasks.
- **`controller`**: REST endpoints.
    - `LobbyController` – manages lobby CRUD and **the SSE endpoint** for each lobby.
    - `SimulationController` – starts the simulation for a given lobby.

---

## Getting Started

1. **Clone** the repository:
   ```bash
   git clone https://github.com/your-repo/lobby-sse-demo.git
   cd lobby-sse-demo
   ```

2. **Build and Run** with Maven:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```
   The application should start on `http://localhost:8080/`.

---

## Usage Example

Below are quick `curl` commands to demonstrate creating a lobby, subscribing to events, and starting a simulation.

### 1. Create a Lobby

```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -d '[{"id":"p1","name":"Alice"},{"id":"p2","name":"Bob"}]' \
  http://localhost:8080/lobbies
```

- **Response**: JSON with the newly created lobby, e.g.:
  ```json
  {
    "id": "a8f4ca6e-777b-4632-b612-903ae16dc127",
    "players": [
      {"id":"p1","name":"Alice"},
      {"id":"p2","name":"Bob"}
    ]
  }
  ```
- Copy the **`id`** value (this is also the simulation ID).

### 2. Subscribe to Events

**All** players or observers can subscribe to SSE via:

```bash
curl http://localhost:8080/lobbies/a8f4ca6e-777b-4632-b612-903ae16dc127/events
```

- You should see `WAITING` events in the terminal (one event repeated, due to replay).
- Any new events will appear in the same stream as they happen.

### 3. Start the Simulation

```bash
curl -X POST http://localhost:8080/simulation/a8f4ca6e-777b-4632-b612-903ae16dc127/start
```

- This triggers **SIMULATION_STARTED** in the SSE feed.
- Afterward, **TURN_UPDATE** events appear every second until turn >= 5, followed by **SIMULATION_FINISHED**.

### 4. Visit While Simulation is Running

If a new user subscribes (step 2) **after** the simulation has started, they immediately receive the **latest** event
and continue with subsequent updates in real-time.

---

## How It Works

1. **Lobby Creation**
    - `POST /lobbies` creates a new `Lobby`.
    - Internally, `LobbyService` also creates a **`Sinks.Many<LobbyEvent>`** to broadcast events related to that lobby.

2. **Single SSE Subscription**
    - `GET /lobbies/{lobbyId}/events` connects the client to the **same** sink used by the lobby.
    - The **most recent** event is re-played for new subscribers (thanks to `.replay().latest()`).
    - Clients remain connected and receive future events until they disconnect or the server closes the stream.

3. **Starting the Simulation**
    - `POST /simulation/{lobbyId}/start` triggers the simulation.
    - `SimulationService` emits a **SIMULATION_STARTED** event, then runs a turn loop.
    - Each turn triggers a **TURN_UPDATE** event with the latest `GameState`. After a finishing condition (`turn >= 5`),
      a **SIMULATION_FINISHED** event is sent.

4. **Real-Time Updates**
    - All clients connected to `/events` see new events simultaneously.
    - If someone arrives late, they catch up from the **latest** event.

---

## Extending the Project

- **Database Integration**: Store lobby, player, and game state in a database for persistence across restarts.
- **Custom Game Logic**: Replace the simple turn-based “increment until 5” logic in `SimulationService` with actual game
  rules.
- **Authentication & Authorization**: Secure endpoints so only lobby members can connect/subscribe.
- **UI / Frontend**: Provide a React/Angular/Vue app that opens the SSE connection and displays real-time data visually.

---

## Conclusion

This demo shows how to **unify** lobby + simulation events into **one** SSE stream per lobby, enabling users to see a
live feed of what’s happening in their game.  
Feel free to adapt and expand it for your own real-world turn-based or real-time applications.