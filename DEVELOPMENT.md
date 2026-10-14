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
│   │   ├── EternalPotions.java             — main entrypoint, events, commands
│   │   ├── EternalState.java               — SavedData for King thrones
│   │   ├── BrewingStandAccess.java         — duck interface for brewTime + fuel
│   │   ├── BrewWaypoints.java              — live brew tracker + waypoint registry
│   │   ├── BrewWaypointTransmitter.java    — one waypoint transmitter per brew
│   │   ├── effect/
│   │   │   └── EternalEffects.java         — four custom MobEffect registrations
│   │   ├── item/
│   │   │   ├── ModItems.java               — item registration
│   │   │   └── EternalPotionItem.java      — drink logic, effect selection
│   │   ├── network/
│   │   │   ├── EternalNetworking.java      — payload registration + receivers
│   │   │   ├── ConfigPresencePayload.java  — S2C config-phase presence probe
│   │   │   ├── ClientVersionPayload.java   — C2S play-phase version report
│   │   │   ├── KingsDataPayload.java       — S2C King data packet
│   │   │   └── BrewTimerPayload.java       — S2C brew timer packet
│   │   └── mixin/
│   │       ├── BrewingStandMixin.java      — brew timer + GUI lock (2 classes)
│   │       ├── EternalResistanceMixin.java
│   │       ├── LocatorBarGateMixin.java    — forces the locator bar on for every player
│   │       ├── MilkPreserveMixin.java
│   │       ├── TotemSaveMixin.java
│   │       ├── VanillaEffectBlockMixin.java
│   │       └── VanillaEffectNerfMixin.java — halves vanilla Speed / Strength / Regen / Resistance
│   └── resources/
│       ├── fabric.mod.json
│       ├── eternal-potions.mixins.json
│       ├── assets/eternal-potions/         — lang, models, textures, particles
│       └── data/eternal-potions/           — recipes
└── client/
    ├── java/osaidii/eternalpotions/
    │   ├── client/
    │   │   ├── EternalPotionsClient.java   — client entrypoint, /kings, version report
    │   │   ├── KingsScreen.java            — leaderboard GUI
    │   │   ├── EternalPotionTooltip.java   — rainbow tooltip
    │   │   └── BrewTimerClientCache.java   — client-side timer cache keyed by waypoint UUID
    │   └── mixin/
    │       └── LocatorBarTimerMixin.java   — draws the brew timer above each waypoint sprite
    └── resources/
        └── eternal-potions.client.mixins.json — client-only mixin config
```

---

## Architecture Overview

The mod is split into **main** (server + common) and **client** source sets via Loom's `splitEnvironmentSourceSets()`. The server-side code owns all game logic; the client only handles rendering and the `/kings` command.

**Six core systems:**

1. **Effect system** — four custom `MobEffect` subclasses registered to `BuiltInRegistries.MOB_EFFECT`. Each effect uses attribute modifiers (`addAttributeModifiers` / `removeAttributeModifiers`) or tick-based logic (`applyEffectTick`). Two mixins handle the parts that can't be expressed through the effect API: `EternalResistanceMixin` for damage reduction, and `VanillaEffectNerfMixin` for halving the vanilla equivalents.

2. **King system** — a `SavedData` instance stored on the overworld's `DataStorage`, keyed by `eternal-potions:kings`. Tracks a UUID + display name per effect. The server broadcasts a `KingsDataPayload` to all clients on every change.

3. **Brewing system** — a mixin on `BrewingStandBlockEntity` intercepts `serverTick`, detects Eternal Shard in slot 3, and overrides the 400-tick vanilla brew with a 12000-tick (10-minute) brew. Slot centering consolidates potions to slot 1 and locks the GUI via a second mixin on `AbstractContainerMenu.clicked`.

4. **Waypoint system** — `BrewWaypoints` tracks every active brew by `GlobalPos`. Each brew registers a `BrewWaypointTransmitter` with the server's `ServerWaypointManager`, so it appears on the client's locator bar. Each transmitter has a deterministic UUID derived from its dimension and block position. Transmitters are untracked when the brew ends or the stand is destroyed. `LocatorBarGateMixin` forces `ServerWaypointManager.isLocatorBarEnabledFor` to always return true, so the bar is visible to all players without a recovery compass.

5. **Timer display system** — the server broadcasts `BrewTimerPayload` once per second, carrying `(UUID, ticksLeft)` pairs. The client caches them in `BrewTimerClientCache`. A client-side mixin on `LocatorBar.lambda$extractRenderState$1` draws the timer text above each waypoint sprite at the same screen coordinates vanilla uses. The timer is skipped if the sprite would fall outside the bar's visible width.

6. **Version gate system** — the server sends `ConfigPresencePayload` during the config phase. Clients that can't receive it are kicked with a Modrinth download link. Clients that can receive it go on to send `ClientVersionPayload` during the play phase. If the versions don't match, the server disconnects them with a message telling them exactly which version to install. Disconnecting in the play phase produces a clean "Disconnected" screen on the client, unlike config-phase disconnects which display as raw socket errors.

---

## Key Classes

### `EternalPotions`

The `ModInitializer`. Registers:
- Effects, items, particles.
- `CreativeModeTabEvents` for creative tab entries.
- `ServerConfigurationConnectionEvents.BEFORE_CONFIGURE` — kicks clients without the mod.
- `ServerPlayConnectionEvents.JOIN` — refreshes King names, sends the welcome message on first join, sends the current brew timers.
- `ServerLivingEntityEvents.AFTER_DEATH` — dethrones, removes effects, drops shard.
- `ServerTickEvents.END_SERVER_TICK` — welcome queue, particles, ghost-king cleanup, brew waypoint tick, `broadcastKings` and `broadcastBrewTimers` every 20 ticks.
- `PlayerBlockBreakEvents.BEFORE` — prevents breaking a brewing stand mid-brew.
- The `/eternal` command tree via `CommandRegistrationCallback`.

Public static helpers:
- `eternalInstance(Holder<MobEffect>, int amplifier)` — builds the standard infinite-duration effect instance.
- `effectName(Holder<MobEffect>)` — translatable effect name component.
- `getModVersion()` — reads the version string from Fabric Loader.
- `getState(MinecraftServer)` — fetches or creates the `EternalState` SavedData.
- `broadcastKings(MinecraftServer)` — sends the King list to all players.
- `broadcastBrewTimers(MinecraftServer)` — sends the active brew timers to all players.
- `checkThrones(MinecraftServer, EternalState)` — announces when all four thrones are filled.
- `parseEffect(String)` — command argument → effect holder.

Private static state:
- `WELCOME_QUEUES` — tracks which players are mid-welcome-message and which page is next.
- `STAND_START_RETRY` — remembers the last position each OP tried to `/eternal stand start` in an unloaded chunk, so a second attempt at the same spot force-loads instead of failing again.

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

The transmitter holds a **deterministic UUID**, computed from `dimension + block position` via `UUID.nameUUIDFromBytes`. This UUID is what ties the waypoint packet on the client to the timer value in `BrewTimerPayload`. Both sides derive the same UUID without needing to send it separately.

`makeWaypointConnectionWith(ServerPlayer)` returns an anonymous `BlockConnection` whose `connect()` sends `ClientboundTrackedWaypointPacket.addWaypointPosition(id, icon, pos)` to the receiving player, and whose `disconnect()` sends `removeWaypoint(id)`. This is how the waypoint actually reaches the client.

### `BrewTimerPayload`

`CustomPacketPayload` record carrying a `List<Entry>`, where each `Entry` is `(UUID id, int ticksLeft)`. Registered S2C. Broadcast once per second from `EternalPotions.broadcastBrewTimers`. Received on the client in `EternalPotionsClient` and stored in `BrewTimerClientCache`.

The UUID in each entry is the same deterministic UUID the transmitter computes. That's how the client-side mixin matches a drawing waypoint to its timer.

### `BrewTimerClientCache`

Client-only static cache. `update(List<Entry>)` replaces the whole map. `get(TrackedWaypoint)` extracts the UUID from the waypoint's `Either<UUID, String>` identifier and looks up the timer. `clear()` resets on disconnect.

### `ConfigPresencePayload`

Empty `CustomPacketPayload` record. Registered S2C in the **configuration phase**. Its only purpose is to exist — the server checks `ServerConfigurationNetworking.canSend(handler, ConfigPresencePayload.TYPE)` to determine if the connecting client has the mod. Clients without the mod can't receive it, so they're kicked with a Modrinth download link before any vanilla registry sync error can appear.

### `ClientVersionPayload`

`CustomPacketPayload` record carrying the client's mod version string. Registered C2S in the **play phase**. Sent by the client on `ClientPlayConnectionEvents.JOIN`. The server's receiver compares it against `EternalPotions.getModVersion()` and disconnects on mismatch.

The reason this runs in the play phase rather than the config phase is UX: config-phase disconnects display as raw "Internal Exception" errors on the client, while play-phase disconnects display the actual message on a proper Disconnected screen.

### `KingsDataPayload`

`CustomPacketPayload` record. Stream codec writes a varint size, then for each entry a UTF string for effect ID and King name. Uses `RegistryFriendlyByteBuf`.

### `BrewingStandAccess`

Duck interface implemented by `BrewingStandMixin` to expose private fields of `BrewingStandBlockEntity` without reflective access.

```java
public interface BrewingStandAccess {
    int eternalPotions$getBrewTime();
    void eternalPotions$setBrewTime(int value);
    void eternalPotions$setFuel(int value);
}
```

Used by:
- `BrewingStandMenuMixin` — reads `brewTime` to decide if the GUI should lock.
- `/eternal stand start` — sets `fuel` to 20 so the stand can begin brewing immediately.

---

## Mixins

Server-side mixins are listed in `eternal-potions.mixins.json`. Client-only mixins are listed in `eternal-potions.client.mixins.json`.

**Server (main config):**

| Mixin | Target | Purpose |
|---|---|---|
| `BrewingStandMixin` | `BrewingStandBlockEntity` | Eternal brew timer, slot centering, extraction lock, exposes duck interface |
| `BrewingStandMenuMixin` | `AbstractContainerMenu` | GUI click lock during brew |
| `EternalResistanceMixin` | `LivingEntity` | Damage reduction from Eternal Resistance; also halves vanilla Resistance |
| `LocatorBarGateMixin` | `ServerWaypointManager` | Forces `isLocatorBarEnabledFor` to always return `true` |
| `MilkPreserveMixin` | `LivingEntity.removeAllEffects` | Preserves Eternal effects through milk |
| `TotemSaveMixin` | `LivingEntity.checkTotemDeathProtection` | Preserves Eternal effects on totem pop |
| `VanillaEffectBlockMixin` | `LivingEntity.addEffect` and `forceAddEffect` | Blocks vanilla Speed/Regen/Resistance/Strength when Eternal equivalent is active |
| `VanillaEffectNerfMixin` | `MobEffect.addAttributeModifiers` / `removeAttributeModifiers` / `applyEffectTick` | Halves vanilla Speed, Strength, Regeneration attribute modifiers and tick rate |

**Client (client config, `environment: client` in `fabric.mod.json`):**

| Mixin | Target | Purpose |
|---|---|---|
| `LocatorBarTimerMixin` | `LocatorBar.lambda$extractRenderState$1` | Draws the timer text above each waypoint sprite |

**Important notes:**

- Server-side mixin config lives at `src/main/resources/eternal-potions.mixins.json`. Its `package` is `osaidii.eternalpotions.mixin`.
- Client-only mixin config lives at `src/client/resources/eternal-potions.client.mixins.json`. Its `package` is also `osaidii.eternalpotions.mixin` — the client mixin class lives at `src/client/java/osaidii/eternalpotions/mixin/LocatorBarTimerMixin.java`. This odd path is deliberate: Fabric needs the two configs to share a package so Mixin can resolve names consistently, but the class itself is only compiled in the client source set.
- A client mixin config **must be registered with `"environment": "client"`** in `fabric.mod.json`, otherwise the server tries to load it and fails because client classes aren't on the server classpath.
- The client config file must have **no BOM**. Windows editors add one by default. Write it with `Set-Content -Encoding ASCII` or `UTF8Encoding($false)` from PowerShell.
- Mixins must not contain public static methods. Use `@Unique` for internal state or package-private duck interfaces (`BrewingStandAccess`) for external access.
- Multiple mixin classes can live in one `.java` file but must each be listed in the appropriate `mixins.json`.

---

## Networking

Four active custom payloads across three phases:

**Config phase (S2C):**

- **`ConfigPresencePayload`** — sent during `ServerConfigurationConnectionEvents.BEFORE_CONFIGURE`. Its registered existence on the client is what the server checks with `ServerConfigurationNetworking.canSend(...)`. Clients that don't answer are kicked with a Modrinth download link.

**Play phase (C2S):**

- **`ClientVersionPayload`** — sent by the client on join. Carries the client's mod version. The server's receiver compares it against the server's version and disconnects on mismatch with a two-line "install version X.Y.Z" message.

**Play phase (S2C):**

- **`KingsDataPayload`** — sent on join, on any throne change, and on the periodic broadcast tick.

- **`BrewTimerPayload`** — sent once per second while at least one brew is active. Carries `(UUID, ticksLeft)` pairs keyed by the same deterministic UUID that `BrewWaypointTransmitter` uses for its waypoint.

To send a payload: `ServerPlayNetworking.send(player, payload)`. To receive on the client: `ClientPlayNetworking.registerGlobalReceiver(TYPE, handler)`.

**Important:** every registered payload must have a matching receiver on the receiving side. An unknown payload arrives as a `DiscardedPayload`, and any code that tries to cast it to the real type will crash with `ClassCastException`.

**Codec alignment:** if a payload's codec changes between versions, the old client will fail to decode the new server's payload with `DecoderException: ... was larger than I expected, found N bytes extra`. This is why the version check exists — a mismatched client should be caught before the codec mismatch actually matters.

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

When the welcome flag is set on first join, `EternalPotions` calls `server.saveEverything(false, false, true)` to force the state to disk immediately. Without this, a restart before the next autosave could lose the flag, and the welcome message would fire again.

---

## Configuration

There is no runtime config file and no compile-time master toggle. Behaviour is fixed to "enabled everywhere the mod is loaded." Change constants in source and rebuild if you need to alter values.

Tunable constants (compile-time):

| Constant | Location | Purpose |
|---|---|---|
| `SPEED_BY_LEVEL` | `EternalEffects.EternalSpeedEffect` | Speed values per amplifier level |
| `WELCOME_DELAY_TICKS` | `EternalPotions` | Ticks between welcome pages (default 60) |
| `TIMER_COLOR`, `TEXT_SCALE` | `LocatorBarTimerMixin` | Colour and size of the locator-bar timer |
| `SPRITE_SIZE` | `LocatorBarTimerMixin` | Vanilla locator bar dot size (9) |

---

## Building

Requires:
- Java 25 (Adoptium or equivalent)
- Gradle 9 via the included wrapper

```bash
./gradlew clean build
```

Produces `build/libs/eternal-potions-1.1.0.jar`.

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

**Effects:**
```
/eternal effect give @s speed              — grant Eternal Speed I
/eternal effect give @s speed              — grant II
/eternal effect give @s speed              — grant III (crowns you)
/eternal effect take @s speed              — drop one level
/eternal effect take @s speed              — drop another
```

**Kings:**
```
/eternal king crown @s speed               — force-crown yourself
/eternal king dethrone speed               — remove the King
/kings                                     — open the leaderboard GUI
```

**Stands:**
```
/eternal stand start ~ ~ ~                 — spawn a pre-loaded brewing stand
/eternal stand stop ~ ~ ~                  — cancel a brew and destroy the stand
```

**Items:**
```
/eternal item @s shard 5                   — give yourself 5 Eternal Shards
/eternal item @s potion 1                  — give yourself an Eternal Potion
```

**Particles and shard drop:**
```
/kill @s                                   — test shard drop on death
/particle eternal-potions:crown_particle ~ ~1 ~
```

**Brewing test:** place a brewing stand, add a water bottle, blaze powder, and an Eternal Shard. Wait 10 minutes. A waypoint appears on the locator bar; walk 300+ blocks away to see it (vanilla hides waypoints in loaded chunks near you).

**Effect-block test:**
```
/effect give @s eternal-potions:eternal_speed infinite 0
/effect give @s minecraft:speed 30 0    — should be blocked
```

**Vanilla nerf test:**
```
/effect give @s minecraft:speed 60 0    — should feel about half as strong
/effect give @s minecraft:strength 60 0 — should hit for about half
```

**Totem test:** hold a totem, `/kill @s`. Effects and throne survive.

**Milk test:** drink an Eternal Potion, then drink milk. Effects persist.

**Version gate tests:**
- Join with a client that doesn't have the mod → kicked in the config phase with a Modrinth link.
- Join with a client that has a different version → disconnected in the play phase with an "install version X.Y.Z" message.

### Cleaning the Gradle cache

If you see `bad class file` or `ClosedByInterruptException`:

```powershell
Remove-Item -Recurse -Force "$env:USERPROFILE\.gradle\caches\modules-2"
Remove-Item -Recurse -Force "$env:USERPROFILE\.gradle\caches\fabric-loom"
Remove-Item -Recurse -Force ".gradle"
.\gradlew --stop
.\gradlew clean build
```

### Dumping a class's method list

When you need to know the real signature of a Minecraft method in 26.3:

```powershell
$jar = "C:\Users\<you>\.gradle\caches\fabric-loom\26.3\minecraft-client.jar"
javap -p -cp $jar net.minecraft.server.level.ServerPlayer
```

To dump the bytecode of a specific method:

```powershell
javap -p -c -cp $jar net.minecraft.server.waypoints.ServerWaypointManager > "$env:USERPROFILE\Desktop\SWM.txt"
Get-Content "$env:USERPROFILE\Desktop\SWM.txt" | Select-String -SimpleMatch "methodName" -Context 0,40
```

Nested classes need a backtick before the `$` in PowerShell:

```powershell
javap -p -cp $jar "net.minecraft.world.waypoints.WaypointTransmitter`$BlockConnection"
```

### Mixin config encoding

Client mixin configs must not have a UTF-8 BOM. Write them like this:

```powershell
$json = @'
{
  "required": true,
  "package": "osaidii.eternalpotions.mixin",
  "compatibilityLevel": "JAVA_25",
  "client": ["LocatorBarTimerMixin"],
  "injectors": { "defaultRequire": 1 }
}
'@
Set-Content -Path "src\client\resources\eternal-potions.client.mixins.json" -Value $json -Encoding ASCII
```

If you see `IllegalArgumentException: The specified resource ... was invalid or could not be read`, it's almost always a BOM.

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
| `PayloadTypeRegistry.playC2S()` | `PayloadTypeRegistry.serverboundPlay()` |
| `PayloadTypeRegistry.configurationS2C()` | `PayloadTypeRegistry.clientboundConfiguration()` |
| `MobEffect.shouldApplyEffectTickThisTick` | removed; check ticks inside `applyEffectTick` |
| `serverPlayer.displayClientMessage(msg, true)` | `serverPlayer.sendOverlayMessage(msg)` |
| `ResourceKey.location()` | removed |
| `serverPlayer.drop(stack, false)` | use `ItemEntity` + `level.addFreshEntity` |

`MobEffect.applyEffectTick` returns `boolean` in 26.3. `LivingEntity.addEffect` is `final` — use `forceAddEffect` for command-driven additions.

### Waypoint infrastructure

- `WaypointTransmitter` interface: `isTransmittingWaypoint()`, `makeWaypointConnectionWith(ServerPlayer)`, `waypointIcon()`.
- `WaypointTransmitter.BlockConnection` — extends `Connection` with `distanceManhattan()` and a default `isBroken()`.
- `ServerLevel.getWaypointManager()` returns `ServerWaypointManager` with `trackWaypoint` / `untrackWaypoint`.
- `ServerWaypointManager.isLocatorBarEnabledFor(ServerPlayer)` checks only the `GameRules.LOCATOR_BAR` gamerule.
- `Waypoint.Icon` has a public mutable `style` field, set to `WaypointStyleAssets.DEFAULT`.
- `ClientboundTrackedWaypointPacket.addWaypointPosition(UUID, Waypoint.Icon, Vec3i)` — the constructor that ships a waypoint from server to client.
- `ClientboundTrackedWaypointPacket.removeWaypoint(UUID)` — the constructor that removes it.
- The client-side gate is `Hud.nextContextualInfoState()`, which checks `ClientWaypointManager.hasWaypoints()`. No compass required — the client shows the bar whenever it has any tracked waypoint.
- `Hud$ContextualInfo` enum values: `EMPTY`, `EXPERIENCE`, `LOCATOR`, `JUMPABLE_VEHICLE`.

### LocatorBar render method

`LocatorBar.lambda$extractRenderState$1` is the per-waypoint draw callback. Its signature:

```
(Entity, Level, PartialTickSupplier, GuiGraphicsExtractor, int barTop, TrackedWaypoint)
```

`barTop` is the Y coordinate of the top edge of the bar, **not** the sprite's X. The sprite's X is computed inside the method as:

```
centerX = ceil((guiWidth - 9) / 2)
offset  = floor(yaw * 173 / 120)
spriteX = centerX + offset
spriteY = barTop - 2
```

To draw above the sprite, compute the same values in the mixin. Rotate-dependent movement comes from `yaw`, obtained via `waypoint.yawAngleToCamera(level, camera, partialTick)`. Waypoints whose yaw is outside `[-60, 60]` are not drawn, so the timer should skip them too — the same early-return check applies automatically because the mixin runs at `RETURN` of the lambda, which only fires when drawing actually happens.

### Config-phase vs play-phase disconnects

Config-phase disconnects (via `ServerConfigurationNetworking`'s response sender) close the socket before the client is ready to display a Disconnect screen. The client shows a raw `Internal Exception: ... an established connection was aborted` message instead of the actual text.

Play-phase disconnects (via `serverPlayer.connection.disconnect(Component)`) are rendered on the proper Disconnected screen with the message you passed. This is why the version check runs on join instead of during the configuration handshake, even though the configuration phase would technically be earlier.

### Version string

`EternalPotions.getModVersion()` reads the version from Fabric Loader:

```java
FabricLoader.getInstance()
    .getModContainer(MOD_ID)
    .map(c -> c.getMetadata().getVersion().getFriendlyString())
    .orElse("unknown");
```

The string ultimately comes from `src/main/resources/fabric.mod.json`'s `"version"` field, which `build.gradle` substitutes from `project.version` at build time. Both sides derive their version from the same source, so no code needs to be updated when the version bumps.

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

Apache License 2.0 — see `LICENSE` for the full text. The bundled `FABRIC LICENSE` covers Fabric-specific portions.
