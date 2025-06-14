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

## Planned/Ideas
- Leaderboard
   - Daily/Weekly/Monthly/Yearly/All Time, idk something like that?
   - {username}/{bot}/{version}
      - So a Bot could appear multiple times, but not multiple times per version
- The Sim should be able to run locally, against example bots or other user bots
   - Local runs don't count for statistics/leaderboards
   - Should be fairly easy, because the sim is just bevy under a different feature flag, so the main sim already runs the WASM code and such. it just needs to load them in the browser
   - Great for Testing/Debugging
- Frontend hosted on GitHub Pages, Account Management, Coding, Watching of replays, Bot Tests, Lobby Management, Chatting in the lobby (?)
- Should lobbies even be a thing? Do we need synchronous Lobbies? Wouldn't Async lobbies work good enough? (or no lobbies at all...)

## Inspiration
- [GLADIABOTS - AI Combat Arena](https://store.steampowered.com/app/871930/GLADIABOTS__AI_Combat_Arena/)
- [Screeps: Arena](https://store.steampowered.com/app/1137320/Screeps_Arena/)
