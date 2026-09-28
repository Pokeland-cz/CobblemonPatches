# Changelog

## [1.2.0] - 27-09-2026

> [!WARNING]
> **Testing Required**: Please test this build thoroughly in a testing/staging environment before deploying to
> production servers.

### Optimizations & Anti-Lag

- **Safe Pokémon Deactivation on Despawn**: Untracked player Pokémon and disconnected entities are now safely queued for deferred cleanup and transitioned cleanly to an inactive state at the end of the server tick, preventing orphaned active entity states, memory leaks, and potential duplication exploits.
- **Thread-Safe Despawn Processing**: Migrated entity despawn queuing to lock-free concurrent collections (`ConcurrentLinkedQueue`) drained strictly at the end of the full server tick (`END_SERVER_TICK`), eliminating race conditions and desyncs between dimensions.
- **Optimized Battle Flee Resolution**: Improved the flee pipeline for wild Pokémon battles, eliminating redundant dispatch loops and resolving fled battles immediately and cleanly.
- **Configurable Autosave Batch Size**: Introduced `autosaveStoresPerTick` (default `2`) in `cobblemonpatches.json`, allowing server administrators to fine-tune dirty store saving throughput to match their hardware and player count.
- **Robust Block Ticker Distribution**: Switched coordinate distribution calculations in PC and Pasture block tickers to `Math.floorMod`, ensuring smooth, uniform tick staggering across all coordinate boundaries.

### Bug Fixes & Stability

- **Server Lifecycle Memory Leak Fix**: Added a `SERVER_STOPPED` lifecycle listener that cleanly releases static server instances and cached `DynamicOps`, preventing memory leaks across server reloads and restarts.
- **Store Cache Consistency**: Automatically invalidates cached Pokémon UUID entries whenever a storage instance is re-initialized or rebuilt, eliminating ghost Pokémon lookups.
- **Fault-Tolerant Autosave Pipeline**: Added isolated error handling during dirty store serialization so that individual store save failures can never crash the server tick.
- **Battle Faint vs. Flee Resolution**: Fixed an issue where fainting a wild Pokémon in battle could erroneously trigger wild entity removal flee handling instead of normal battle resolution.
- **Empty Actor Flee Guard**: Corrected flee distance evaluations so that battles with empty or uninitialized fleeable actor lists no longer trigger false-positive flee events.
- **Battle Inactivity Watchdog**: Integrated an automated 120-second inactivity watchdog that cleanly resolves and terminates hung or locked battles, broadcasting a configurable chat notification and restoring player control without requiring server restarts.
- **Prevent Orphaned Pokémon Duplication Exploits**: Fixed an exploit where switching servers (e.g. via `/spawn`) during battle animations left player Pokémon entities stranded in the world, allowing duplicate held items to be taken. Battle conclusion now purges and deactivates active battle Pokémon belonging to offline players, `onStoppedTrackingBy` unconditionally queues disconnecting player Pokémon for despawn, and a periodic tick guard eliminates lingering orphaned party Pokémon.
- **Async Entity Removal Thread Safety**: Added `EntityRemoveAsyncMixin` to automatically marshal off-thread `Entity#remove` and `Entity#discard` calls (such as from Netty IO disconnect handlers in trainer/NPC mods like Radical Cobblemon Trainers) onto the main server thread, preventing `AsyncCatcher` errors and chunk entity manager race conditions.

## [1.1.9] - 17-09-2026

> [!WARNING]
> **Testing Required**: Please test this build thoroughly in a testing/staging environment before deploying to
> production servers.
### Optimizations & Anti-Lag

- **Eliminated Autosave Lag Spikes**: Previously, the server saved all player Pokémon and PC storage in a single massive burst, causing noticeable server freezes and TPS drops every autosave interval. Storage is now saved smoothly across individual ticks in the background with zero lag spikes.
- **Faster Pokémon Spawning**: The game no longer recalculates biome and block conditions from scratch tens of thousands of times per second. Spawning rules are now remembered in fast memory, dramatically speeding up spawning calculations and freeing up server CPU.
- **Optimized Pokémon Collisions & Cramming**: Wild Pokémon no longer perform constant owner and team searches when bumping into each other or players. This significantly reduces lag when many wild Pokémon group together in dense areas.
- **Smoother Pokémon Despawning**: Wild Pokémon now check player distances efficiently instead of continuously scanning every player in the world 20 times a second, reducing server lag in busy dimensions.
- **Lag-Free PC & Pasture Blocks**: PC and Pasture blocks in loaded bases no longer scan for nearby players 20 times a second just to turn on their screen lights. Checks now run smoothly once a second and are staggered across game ticks so bases with multiple PCs stay lag-free.
- **Faster Pokémon Data Loading & Less Memory Usage**: Streamlined how Pokémon data is read and saved, bypassing unnecessary data conversion steps for modern Pokémon files and reducing memory allocation and garbage collection pauses.
- **Smoother NPC Visibility**: NPC visibility checks are now remembered per game tick, eliminating lag when players look at or interact with trainers and quest NPCs.

### Bug Fixes & Stability

- **Battle Freeze & Softlock Fixes**:
  - **AI & Wild Pokémon Turns**: Fixed battles getting permanently frozen when wild Pokémon or NPC trainers make a move.
  - **Battling Pokémon Despawning**: Fixed active battling Pokémon unexpectedly disappearing mid-battle and breaking the fight.
  - **Ghost Battles**: Fixed players getting permanently stuck in a "already in battle" state after disconnecting or finishing a match.
  - **Automatic Inactivity Recovery**: Added an automatic 120-second watchdog that safely concludes battles that get stuck or frozen, without needing a server restart.
- **Safe Pokémon Recall**: Fixed a server crash that could occur when recalling Pokémon while the server is loading or shutting down.

## [1.1.8] - 11-09-2026

> [!WARNING]
> **Testing Required**: Please test this build thoroughly in a testing/staging environment before deploying to
> production servers.

### Optimizations & Anti-Lag

- **Eliminated Server Freezes from NPC Skins**: When NPCs with custom player skins appear in the world (such as Gym Leaders, shopkeepers, quest NPCs, or town villagers), the server used to completely freeze for 1 to 2 seconds while downloading the skin from the internet. Skin loading now happens silently in the background with zero lag spikes.
- **Smart Skin Memory (Instant Reuse)**: Once a player skin is downloaded for an NPC, it is saved in fast memory. If the same skin is used by multiple NPCs (like identical shopkeepers or trainers), the skin is applied instantly without any repeated web downloads.
- **Lag-Free Skin Commands & Quests**: Using `/applyplayertexture` or changing NPC skins during dialogue trees and quest scripts no longer causes server hitching or TPS drops.
- **Protection Against Slow Skin Servers**: Added automatic timeouts so that even if Mojang's skin servers are slow, laggy, or temporarily offline, your server will never hang or freeze.

## [1.1.7] - 10-09-2026

> [!WARNING]
> **Testing Required**: Please test this build thoroughly in a testing/staging environment before deploying to
> production servers.

### Added

- **Move Suggestions in Commands**: Added autocomplete suggestions for the `moves` property in commands like `/pokegive`, `/pokespawn`, and `/pokemonedit`. Players can now easily tab-complete up to 4 moves separated by commas (e.g. `moves=tackle,growl,scratch`).
- **Server-Only Support**: Works 100% server-side. Players connecting with vanilla Cobblemon will automatically get full move suggestions without needing any client-side mods installed.

### Optimizations

- **Autocompletion Performance**: Optimized move list caching and text parsing to ensure instant suggestion popups with zero server lag.

## [1.1.6] - 06-09-2026

> [!WARNING]
> **Testing Required**: Please test this build thoroughly in a testing/staging environment before deploying to
> production servers.

### Bug Fixes

- **Scoreboard Team Desync**: Fixed a client and server crash (`IllegalStateException`) caused when removing players or
  entities from scoreboard teams that were already cleared or desynchronized.

## [1.1.5] - 06-09-2026

> [!WARNING]
> **Testing Required**: Please test this build thoroughly in a testing/staging environment before deploying to
> production servers.

### Optimizations

- **Type Gem Growth**: Significantly improved world generation speed and server performance by optimizing how Type Gem
  Cores grow clusters, eliminating lag and memory overhead during chunk generation and random ticks.

## [1.1.4] - 02-09-2026

> [!WARNING]
> **Testing Required**: Please test this build thoroughly in a testing/staging environment before deploying to
> production servers.

### Security & Anti-Lag

- **Snowballs**: Fixed server lag caused by infinite lingering snowballs (automatically removes snowballs that are stuck
  or flying for too long).
- **Anti-Exploit System**: Added a structured module to organize and handle anti-lag, anti-dupe, and server security
  patches.

### Optimizations

- **Campfires**: Significantly reduced server lag caused by campfires by skipping redundant recipe and cooking checks
  every tick.
- **Showdown IDs**: Improved performance when calculating Pokémon battle IDs by caching them.
- **Pokémon Aspects**: Optimized how Pokémon visual aspects and forms are processed to lower memory usage.
- **Moves & EVs**: Optimized Pokémon move lists and EV stat synchronization to reduce lag and memory allocation during
  saves and syncs.
- **Profiling**: Improved method naming to make performance tracking easier when profiling with Spark.

### Changed

- **Cobblemon 1.8.0 Compatibility**: Updated compatibility and dependencies to support Cobblemon 1.8.0 (Minecraft
  1.21.1).
- **Pasture Block Improvements**: Refactored implementation for better stability and efficiency.

### Bug Fixes

- **Null Entity Handling**: Fixed issues with null entity cases during tracking start/end operations.
- **Thread Safety**: Ensured entity tracking operations run on the correct server thread.

### Removed

- **Berry Block Entity Mixin**: Removed the berry block entity mixin and its performance optimizations.
- **Gilded Chest Mixin**: Removed the mixin handling gilded chest duplication.

## [1.1.3] - 21-08-2026

### Improvements upon existing patches

- **PastureBlocks**: PC positions of tethered Pokémon are now retrieved directly instead of manually searching the
  entire PC.

## [1.1.2] - 25-06-2026

### Bug Fixes

- **ChunkTicketManager**: Fixed a crash (NullPointerException) that occurred when a player disconnected or changed
  dimensions from an unregistered chunk.

## [1.1.1] - 26-01-2026

Compatibility with Cobblemon 1.7.2 and 1.7.3

### Bug Fixes

- **Fossil Machine**: Fixed a crash when inserting enchanted items into the machine in Survival.

- **Fossil Machine**: Fixed a rare server crash during fossil operations.

## [1.1.0] - 2025-12-19

### Bug Fixes

- **ServerCommandSource**: Removed command source caching to prevent potential memory leaks.

## [1.0.0] - 2025-12-01

### Bug Fixes

- **Saccharine Tree**: Fixed a server crash caused by asynchronous entity removal.
- **Empty Poké Ball**: Fixed a crash (NullPointerException) when capturing Pokémon in edge cases.
- **Entity Collections**: Fixed crashes while ticking Cobblemon entities in FastUtil collections.

### Optimizations

- **Pasture Blocks**: Optimized Pasture Block ticking by caching PC positions to avoid scanning the entire PC.
- **Showdown ID**: Added caching to speed up Pokémon ID lookups.
- **Berry Blocks**: Improved berry handling by caching plant types and reducing unnecessary block updates.
- **PC Storage**: Optimized PC box iteration to reduce memory duplication and lag.
