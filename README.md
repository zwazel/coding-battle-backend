# Coding Battle Backend

This project powers the server side of a simple multiplayer "coding battle" prototype. It is a Spring Boot application that offers APIs for creating lobbies, compiling user code, and running lightweight turn based simulations. The goal is to provide a playground where different users can submit code, have it compiled to WebAssembly, and compete in small matches.

## Overview

* **Lobby Management** – create a lobby and invite other players via REST endpoints.
* **User Code Compilation** – upload source files which are compiled to WASM using language-specific compilers.
* **Simulation Service** – start a match in a lobby and receive updates in real time through Server‑Sent Events.

The backend stores its data in an H2 database (for development) and secures endpoints with a basic JWT setup. It is intended as a starting point for experimenting with multiplayer coding games.

## Running Locally

1. Ensure Java 17+ and Maven are installed.
2. From the project directory run:
   ```bash
   mvn spring-boot:run
   ```
3. The server starts on `http://localhost:8080/api`. Default users are seeded for convenience when running with the `dev` profile.

## Usage

Once running you can interact with the API using any HTTP client. Typical steps are:

1. **Create a lobby** – `POST /api/lobbies`
2. **Upload code** – `POST /api/usercode`
3. **Start a simulation** – `POST /api/simulation/{lobbyId}/start`
4. **Subscribe to events** – `GET /api/lobbies/{lobbyId}/events`

Refer to the controller classes for exact request/response details. This backend is meant to be extended with custom game logic and additional languages.

