# Sylphian Network: Minecraft Plugins

This repository contains all custom Minecraft plugins developed for the **Sylphian Network**, a Minecraft server network associated with the [Sylphian](https://sylphian.net) community forum.

The project is a Gradle multi-module build targeting **Paper 26.1.2** and **Java 25**.

---

## Plugins

| Module                     | Platform | Description                                                                                                                                                                    |
|----------------------------|----------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `Sylphian-Items`           | Paper    | Provides a thread-safe cross-plugin item registry (namespaced provider model), an ItemBuilder utility.                                                                         |
| `Sylphian-Economy`         | Paper    | Player currency backed by the central database; cross-plugin API for deposit, withdraw, and transfer, with event-driven balance updates.                                       |
| `Sylphian-Crates`          | Paper    | Universal crate system with weighted reward pools and a player pick selection GUI, designed to hook into any other Paper plugin via its API                                    |
| `Sylphian-Database`        | Paper    | Central database handler and source of truth for all database connections, providing connection pooling and migrations to every plugin that depends on it                      |
| `Sylphian-Fishing`         | Paper    | Custom fishing system with biome, depth, and time-of-day restrictions, rarity tiers, mutations, bait zones, and a fish encyclopaedia                                           |
| `Sylphian-Cooking`         | Paper    | Custom cooking system with multi-ingredient recipes (up to 5), per-block independent state stored in PDC, and a custom GUI triggered by right-clicking any furnace or campfire |
| `Sylphian-Profile`         | Paper    | Manages player sessions and account information, serving as the source of truth for per-player data across the network                                                         |
| `Sylphian-Scoreboard`      | Paper    | Centralised scoreboard manager, all scoreboard access and updates across plugins go through here                                                                               |
| `Sylphian-Verify-Paper`    | Paper    | Verification layer for single-server setups, handling initial account linking and player details when the database is available                                                |
| `Sylphian-Verify-Velocity` | Velocity | Verification layer for proxy setups, works alongside Sylphian-Verify-Paper to support both standalone and proxy+server configurations                                          |
