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

```
src/
├── main/
│   ├── java/osaidii/eternalpotions/
│   │   ├── EternalPotions.java           — main entrypoint, events, commands
│   │   ├── EternalState.java             — SavedData for King thrones
│   │   ├── BrewingStandAccess.java       — duck interface for brewTime
│   │   ├── effect/
│   │   │   └── EternalEffects.java       — four custom MobEffect registrations
│   │   ├── item/
│   │   │   ├── ModItems.java             — item registration
│   │   │   └── EternalPotionItem.java    — drink logic, effect selection
│   │   ├── network/
│   │   │   ├── EternalNetworking.java    — payload registration
│   │   │   └── KingsDataPayload.java     — S2C King data packet
│   │   └── mixin/
│   │       ├── BrewingStandMixin.java    — brew timer + GUI lock (2 classes)
│   │       ├── EternalResistanceMixin.java
│   │       ├── MilkPreserveMixin.java
│   │       ├── TotemSaveMixin.java
│   │       └── VanillaEffectBlockMixin.java
│   └── resources/
│       ├── fabric.mod.json
│       ├── eternal-potions.mixins.json
│       ├── assets/eternal-potions/       — lang, models, textures, particles
│       └── data/eternal-potions/         — recipes
└── client/
    └── java/osaidii/eternalpotions/client/
        ├── EternalPotionsClient.java     — client entrypoint, /kings
        ├── KingsScreen.java              — leaderboard GUI
        ├── EternalPotionTooltip.java     — rainbow tooltip
        └── ServerModCheck.java           — server-presence check
```

---

## Architecture Overview

The mod is split into **main** (server + common) and **client** source sets via Loom's `splitEnvironmentSourceSets()`. The server-side code owns all game logic; the client only handles rendering and the `/kings` command.

**Three core systems:**

1. **Effect system** — four custom `MobEffect` subclasses registered to `BuiltInRegistries.MOB_EFFECT`. Each effect uses attribute modifiers (`addAttributeModifiers` / `removeAttributeModifiers`) or tick-based logic (`applyEffectTick`). One dedicated mixin (`EternalResistanceMixin`) handles damage reduction since it can't be expressed as an attribute modifier.

2. **King system** — a `SavedData` instance stored on the overworld's `DataStorage`, keyed by `eternal-potions:kings`. Tracks a UUID + display name per effect. The server broadcasts a `KingsDataPayload` to all clients on every change.

3. **Brewing system** — a mixin on `BrewingStandBlockEntity` intercepts `serverTick`, detects Eternal Shard in slot 3, and overrides the 400-tick vanilla brew with a 12000-tick (10-minute) brew. Slot centering consolidates potions to slot 1 and locks the GUI via a second mixin on `AbstractContainerMenu.clicked`.

---

## Key Classes

### `EternalPotions`

The `ModInitializer`. Registers:
- Effects, items, particles.
- `CreativeModeTabEvents` for creative tab entries.
- `ServerPlayConnectionEvents.JOIN` — refreshes King names on join, applies pending respawn effects.
- `ServerLivingEntityEvents.AFTER_DEATH` — dethrones, removes effects, drops shard.
- `ServerTickEvents.END_SERVER_TICK` — particles, ghost-king cleanup, broadcast.
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

Extends `SavedData`. Holds three maps (`kings`, `kingNames`, `pendingRespawns`) and one boolean (`allThronesAnnounced`). Uses a `RecordCodecBuilder` for serialization.

Key methods:
- `crownKing(effectId, uuid, name)` / `removeKing(effectId)` — throne mutations.
- `isSlotEmpty(effectId)` — throne availability check.
- `addPendingRespawn(uuid, effectId)` / `takePendingRespawns(uuid)` — offline-crowning queue.
- `setAllThronesAnnounced(boolean)` — persists the announcement flag across restarts.

### `EternalEffects`

Static initializer with four `Holder<MobEffect>` fields. Each effect is a private static nested class. Registered by name to `BuiltInRegistries.MOB_EFFECT` with IDs like `eternal-potions:eternal_speed`.

### `EternalPotionItem`

Overrides `use`, `onUseTick`, `finishUsingItem`, `getUseAnimation`, `getUseDuration`. The `onUseTick` plays the drinking sound every 8 ticks. `finishUsingItem` picks a random eligible effect from `BIG_FOUR`, applies it, and crowns the player if the new amplifier is 2.

### `KingsDataPayload`

`CustomPacketPayload` record. Stream codec writes a varint size, then for each entry a UTF string for effect ID and King name. Uses `RegistryFriendlyByteBuf`.

---

## Mixins

Listed in `eternal-potions.mixins.json` with `compatibilityLevel: JAVA_25`.

| Mixin | Target | Purpose |
|---|---|---|
| `BrewingStandMixin` | `BrewingStandBlockEntity` | Eternal brew timer, slot centering, extraction lock |
| `BrewingStandMenuMixin` | `AbstractContainerMenu` | GUI click lock during brew |
| `EternalResistanceMixin` | `LivingEntity` | Extra damage reduction from Eternal Resistance |
| `MilkPreserveMixin` | `LivingEntity.removeAllEffects` | Preserves Eternal effects through milk |
| `TotemSaveMixin` | `LivingEntity.checkTotemDeathProtection` | Preserves Eternal effects on totem pop |
| `VanillaEffectBlockMixin` | `LivingEntity.addEffect` and `forceAddEffect` | Blocks vanilla Speed/Regen/Resistance/Strength when Eternal equivalent is active |

**Important:** Mixins must not contain public static methods. Use `@Unique` for internal state or package-private duck interfaces (`BrewingStandAccess`) for external access. Multiple mixin classes can live in one `.java` file but must each be listed in `mixins.json`.

---

## Networking

Two custom payloads:

- **`KingsDataPayload`** (S2C) — sent on join, on any throne change, and on the periodic broadcast tick. Registered in `EternalNetworking.register()` via `PayloadTypeRegistry.clientboundPlay()`.

- **`ModPresencePayload`** (S2C existence probe) — reserved for the server-presence check. Currently unused but registered.

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
```

Stored in `server.overworld().getDataStorage()`. Survives server restarts, world saves, and dimension changes.

---

## Configuration

Static constants in `EternalPotions.java`:

| Constant | Default | Effect |
|---|---|---|
| `EVERYTHING_EVERYWHERE` | `true` | Master toggle. `false` re-enables singleplayer gating. |

Rebuild after changing. No runtime config file.

---

## Building

Requires:
- Java 25 (Adoptium or equivalent)
- Gradle 9 via the included wrapper

```bash
./gradlew clean build
```

Produces `build/libs/eternal-potions-1.0.0.jar`.

For a dev build with mapped names and debug symbols:

```bash
./gradlew build --info
```

---

## Development

### IntelliJ Setup

1. `File → Open` → select `build.gradle`.
2. Wait for Gradle sync.
3. Set Project SDK to Java 25.
4. Enable the Minecraft Development plugin (optional, for mixin support).

### Running

```bash
./gradlew runClient     # launches a dev client
./gradlew runServer     # launches a dev server (needs run/eula.txt)
```

`runServer` requires `run/eula.txt` containing `eula=true`.

### Testing in-game

```
/eternal give @s speed              — grant Eternal Speed I
/eternal give @s speed              — grant II
/eternal give @s speed              — grant III (crowns you)
/eternal king list                  — view thrones
/eternal king dethrone speed        — remove the King
/eternal take @s speed              — remove your effect
/kill @s                            — test shard drop on death
/kings                              — open the leaderboard GUI
/particle eternal-potions:crown_particle ~ ~1 ~
```

Brewing test: place a brewing stand, add a water bottle, blaze powder, and an Eternal Shard. Wait 10 minutes.

Effect-block test:
```
/effect give @s eternal-potions:eternal_speed infinite 0
/effect give @s minecraft:speed 30 0    — should be blocked
```

Totem test: hold a totem, `/kill @s`. Effects and throne survive.

Milk test: drink an Eternal Potion, then drink milk. Effects persist.

### Cleaning the Gradle cache

If you see `bad class file` or `ClosedByInterruptException`:

```powershell
Remove-Item -Recurse -Force "$env:USERPROFILE\.gradle\caches\modules-2"
Remove-Item -Recurse -Force "$env:USERPROFILE\.gradle\caches\fabric-loom"
Remove-Item -Recurse -Force ".gradle"
.\gradlew --stop
.\gradlew clean build
```

---

## 26.3 API Notes

Notable API changes from older Minecraft versions that this codebase relies on:

| Old | 26.3 |
|---|---|
| `ResourceLocation` | `Identifier` |
| `SavedData.Factory` | `SavedDataType` with `Codec` |
| `ItemGroupEvents` | `CreativeModeTabEvents.modifyOutputEvent` |
| `PlayerList.isOp(GameProfile)` | `PlayerList.isOp(NameAndId)` |
| `GuiGraphics` | `GuiGraphicsExtractor` |
| `Screen.render()` | `Screen.extractRenderState(...)` |
| `Minecraft.setScreen()` | `Minecraft.setScreenAndShow()` |
| `UseAnim` | `ItemUseAnimation` |
| `Player.server` | `player.level().getServer()` |
| `serverPlayer.serverLevel()` | `(ServerLevel) player.level()` |
| `PayloadTypeRegistry.playS2C()` | `PayloadTypeRegistry.clientboundPlay()` |
| `MobEffect.shouldApplyEffectTickThisTick` | removed; check ticks inside `applyEffectTick` |

`MobEffect.applyEffectTick` returns `boolean` in 26.3. `LivingEntity.addEffect` is `final` — use `forceAddEffect` for command-driven additions.

---

## Contributing

1. Fork the repository.
2. Create a feature branch (`git checkout -b feature/your-feature`).
3. Follow existing code style (tabs for indentation, uppercase constants, package-private mixin state).
4. Build locally with `./gradlew build` before opening a PR.
5. Open a pull request with a description of what the change does and how it was tested.

Bug reports: include the Minecraft version, Fabric Loader version, Fabric API version, full log, and reproduction steps.

---

## License

CC0 — public domain. See `FABRIC LICENSE` for the Fabric-specific portions.
