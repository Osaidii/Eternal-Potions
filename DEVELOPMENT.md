# Development


## Table of Contents

- [Project Structure](#project-structure)
- [Architecture Overview](#architecture-overview)
- [Key Classes](#key-classes)
- [Mixins](#mixins)
- [Networking](#networking)
- [Data Persistence](#data-persistence)
- [Configuration](#configuration)
- [Building](#building)
- [Development](#development)
- [26.3 API Notes](#263-api-notes)
- [Contributing](#contributing)
- [License](#license)

---

## Project Structure

src/  
├── main/  
│ ├── java/osaidii/eternalpotions/  
│ │ ├── EternalPotions.java — main entrypoint, events, commands  
│ │ ├── EternalState.java — SavedData for King thrones  
│ │ ├── BrewingStandAccess.java — duck interface for brewTime  
│ │ ├── BrewWaypoints.java — live brew tracker + waypoint registry  
│ │ ├── BrewWaypointTransmitter.java — one waypoint transmitter per brew  
│ │ ├── effect/  
│ │ │ └── EternalEffects.java — four custom MobEffect registrations  
│ │ ├── item/  
│ │ │ ├── ModItems.java — item registration  
│ │ │ └── EternalPotionItem.java — drink logic, effect selection  
│ │ ├── network/  
│ │ │ ├── EternalNetworking.java — payload registration (+ nested ConfigPresencePayload)  
│ │ │ ├── KingsDataPayload.java — S2C King data packet  
│ │ │ └── BrewTimerPayload.java — S2C brew timer packet (on disk, not yet wired)  
│ │ └── mixin/  
│ │ ├── BrewingStandMixin.java — brew timer + GUI lock (2 classes)  
│ │ ├── EternalResistanceMixin.java  
│ │ ├── MilkPreserveMixin.java  
│ │ ├── TotemSaveMixin.java  
│ │ ├── VanillaEffectBlockMixin.java  
│ │ └── VanillaEffectNerfMixin.java — halves vanilla Speed / Strength / Regen / Resistance  
│ └── resources/  
│ ├── fabric.mod.json  
│ ├── eternal-potions.mixins.json  
│ ├── assets/eternal-potions/ — lang, models, textures, particles  
│ └── data/eternal-potions/ — recipes  
└── client/  
└── java/osaidii/eternalpotions/client/  
├── EternalPotionsClient.java — client entrypoint, /kings  
├── KingsScreen.java — leaderboard GUI  
└── EternalPotionTooltip.java — rainbow tooltip  


---

## Architecture Overview

The mod is split into **main** (server + common) and **client** source sets via Loom's `splitEnvironmentSourceSets()`. The server-side code owns all game logic; the client only handles rendering and the `/kings` command.

**Four core systems:**

1. **Effect system** — four custom `MobEffect` subclasses registered to `BuiltInRegistries.MOB_EFFECT`. Each effect uses attribute modifiers (`addAttributeModifiers` / `removeAttributeModifiers`) or tick-based logic (`applyEffectTick`). Two mixins handle the parts that can't be expressed through the effect API: `EternalResistanceMixin` for damage reduction, and `VanillaEffectNerfMixin` for halving the vanilla equivalents.

2. **King system** — a `SavedData` instance stored on the overworld's `DataStorage`, keyed by `eternal-potions:kings`. Tracks a UUID + display name per effect. The server broadcasts a `KingsDataPayload` to all clients on every change.

3. **Brewing system** — a mixin on `BrewingStandBlockEntity` intercepts `serverTick`, detects Eternal Shard in slot 3, and overrides the 400-tick vanilla brew with a 12000-tick (10-minute) brew. Slot centering consolidates potions to slot 1 and locks the GUI via a second mixin on `AbstractContainerMenu.clicked`.

4. **Waypoint system** — `BrewWaypoints` tracks every active brew by `GlobalPos`. Each brew registers a `BrewWaypointTransmitter` with the server's `ServerWaypointManager`, so it appears on the client's locator bar. Transmitters are untracked when the brew ends or the stand is destroyed.

---

## Key Classes

### `EternalPotions`

The `ModInitializer`. Registers:
- Effects, items, particles.
- `CreativeModeTabEvents` for creative tab entries.
- `ServerPlayConnectionEvents.JOIN` — refreshes King names on join, sends the welcome message on first join.
- `ServerLivingEntityEvents.AFTER_DEATH` — dethrones, removes effects, drops shard.
- `ServerTickEvents.END_SERVER_TICK` — welcome queue, particles, ghost-king cleanup, brew waypoint tick, broadcast.
- `PlayerBlockBreakEvents.BEFORE` — prevents breaking a brewing stand mid-brew.
- The `/eternal` command tree via `CommandRegistrationCallback`.

Public static helpers:
- `eternalInstance(Holder<MobEffect>, int amplifier)` — builds the standard infinite-duration effect instance.
- `effectName(Holder<MobEffect>)` — translatable effect name component.
- `getState(MinecraftServer)` — fetches or creates the `EternalState` SavedData.
- `broadcastKings(MinecraftServer)` — sends the King list to all players.
- `checkThrones(MinecraftServer, EternalState)` — announces when all four thrones are filled.
- `parseEffect(String)` — command argument → effect holder.

### `EternalState`

Extends `SavedData`. Holds three maps (`kings`, `kingNames`, `pendingRespawns`), a `welcomedPlayers` list, and one boolean (`allThronesAnnounced`). Uses a `RecordCodecBuilder` for serialization.

Key methods:
- `crownKing(effectId, uuid, name)` / `removeKing(effectId)` — throne mutations.
- `isSlotEmpty(effectId)` — throne availability check.
- `addPendingRespawn(uuid, effectId)` / `takePendingRespawns(uuid)` — offline-crowning queue.
- `hasBeenWelcomed(uuid)` / `markWelcomed(uuid)` — first-join message tracking.
- `setAllThronesAnnounced(boolean)` — persists the announcement flag across restarts.

### `EternalEffects`

Static initializer with four `Holder<MobEffect>` fields. Each effect is a private static nested class. Registered by name to `BuiltInRegistries.MOB_EFFECT` with IDs like `eternal-potions:eternal_speed`.

Values:
- Eternal Speed: +15% / +25% / +40% movement (custom — lower than vanilla to keep FOV sane)
- Eternal Strength: +3 / +6 / +9 attack damage (same as vanilla Strength)
- Eternal Regeneration: heal every 50 / 25 / 12 ticks (same as vanilla Regeneration)
- Eternal Resistance: 20% / 40% / 60% reduction (same as vanilla Resistance)

### `EternalPotionItem`

Overrides `use`, `onUseTick`, `finishUsingItem`, `getUseAnimation`, `getUseDuration`. `onUseTick` plays the drinking sound every 8 ticks. `finishUsingItem` picks a random eligible effect from `BIG_FOUR`, applies it, and crowns the player if the new amplifier is 2.

### `BrewWaypoints` and `BrewWaypointTransmitter`

`BrewWaypoints` is a static registry keyed by `GlobalPos`. It holds two maps — one for the live tick count of each active brew, one for the waypoint transmitter. `startBrew` is a no-op if the position is already tracked. `tick` is called from the server tick loop for each level, pruning any transmitter whose brew has finished.

`BrewWaypointTransmitter` implements `WaypointTransmitter`. Its `isTransmittingWaypoint()` asks `BrewWaypoints` whether its position still has an active brew. `waypointIcon()` uses `WaypointStyleAssets.DEFAULT`.

### `KingsDataPayload`

`CustomPacketPayload` record. Stream codec writes a varint size, then for each entry a UTF string for effect ID and King name. Uses `RegistryFriendlyByteBuf`.

### `BrewTimerPayload`

`CustomPacketPayload` record. Currently on disk but **not wired into any live code path**. Will be used by the locator-bar timer renderer once the client mixin is written. Do not register it in `EternalNetworking` until the client receiver is also in place — sending it without a matching receiver crashes the connection with a `DiscardedPayload` cast exception.

---

## Mixins

Listed in `eternal-potions.mixins.json` with `compatibilityLevel: JAVA_25`.

| Mixin | Target | Purpose |
|---|---|---|
| `BrewingStandMixin` | `BrewingStandBlockEntity` | Eternal brew timer, slot centering, extraction lock |
| `BrewingStandMenuMixin` | `AbstractContainerMenu` | GUI click lock during brew |
| `EternalResistanceMixin` | `LivingEntity` | Damage reduction from Eternal Resistance; also halves vanilla Resistance |
| `MilkPreserveMixin` | `LivingEntity.removeAllEffects` | Preserves Eternal effects through milk |
| `TotemSaveMixin` | `LivingEntity.checkTotemDeathProtection` | Preserves Eternal effects on totem pop |
| `VanillaEffectBlockMixin` | `LivingEntity.addEffect` and `forceAddEffect` | Blocks vanilla Speed/Regen/Resistance/Strength when Eternal equivalent is active |
| `VanillaEffectNerfMixin` | `MobEffect.addAttributeModifiers` / `removeAttributeModifiers` / `applyEffectTick` | Halves vanilla Speed, Strength, Regeneration attribute modifiers and tick rate |

**Important:** Mixins must not contain public static methods. Use `@Unique` for internal state or package-private duck interfaces (`BrewingStandAccess`) for external access. Multiple mixin classes can live in one `.java` file but must each be listed in `mixins.json`.

---

## Networking

Two active custom payloads:

- **`KingsDataPayload`** (S2C) — sent on join, on any throne change, and on the periodic broadcast tick. Registered in `EternalNetworking.register()` via `PayloadTypeRegistry.clientboundPlay()`.

- **`ConfigPresencePayload`** (S2C config-phase) — sent during `ServerConfigurationConnectionEvents.BEFORE_CONFIGURE`. Its registered existence on the client is what the server checks with `ServerConfigurationNetworking.canSend(...)`. Clients that don't answer are kicked with a Modrinth download link.

One payload on disk, not wired:

- **`BrewTimerPayload`** — will carry `(BlockPos, ticksLeft)` entries for the client-side locator-bar timer. **Do not register it or send it until the client receiver exists** — doing so causes `ClassCastException` on the client because unknown payloads are decoded as `DiscardedPayload`.

To send a payload: `ServerPlayNetworking.send(player, payload)`. To receive on the client: `ClientPlayNetworking.registerGlobalReceiver(TYPE, handler)`.

---

## Data Persistence

`EternalState` is serialized as NBT via `SavedDataType`:

```java
new SavedDataType<>(
    Identifier.fromNamespaceAndPath(MOD_ID, "kings"),
    EternalState::new,
    EternalState.CODEC,
    null
)

