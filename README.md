# Eternal Potions

[![Minecraft](https://img.shields.io/badge/Minecraft-26.3-blue)](https://minecraft.net)
[![Fabric](https://img.shields.io/badge/Fabric-0.19.5-orange)](https://fabricmc.net)
[![Java](https://img.shields.io/badge/Java-25-red)](https://adoptium.net)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)

A Fabric mod for Minecraft 26.3 that adds a permanent-buff potion and a four-throne King system for PvP servers.

---
## Discord

Join our Minecraft SMP's Discord Server that uses this mod:  
[https://discord.gg/FbUauw9rgT](https://discord.gg/FbUauw9rgT)
  

---

## Table of Contents

- [What This Mod Adds](#what-this-mod-adds)
- [Installation](#installation)
- [Getting Started](#getting-started)
- [The Four Effects](#the-four-effects)
- [Vanilla Effects Are Nerfed](#vanilla-effects-are-nerfed)
- [The King System](#the-king-system)
- [Locator Bar & Brew Timer](#locator-bar--brew-timer)
- [Commands](#commands)
- [Recipes](#recipes)
- [Brewing](#brewing)
- [Protection Rules](#protection-rules)
- [Tips & Strategy](#tips--strategy)
- [FAQ](#faq)
- [Development or Contributing](#development-or-contributing)
- [Disclosure](#disclosure)
- [License](#license)

---

## What This Mod Adds

- **Eternal Shard** — the key ingredient used to brew Eternal Potions.
- **The Eternal Potion** — a consumable that grants a random permanent buff.
- **Four custom effects**, each with three levels. Level III is unique — only one player per effect can hold it at a time.
- **A King system** that tracks who holds each Level III effect. Kings get special particle effects, an announcement when crowned, and lose their throne on death.
- **A `/kings` menu** that shows the current holders of each throne.
- **Live waypoints on the locator bar** for every active brew, with a **timer above each icon** showing time remaining.
- **Version checking** — clients with the wrong mod version are told exactly which version to install.

Both items are **fireproof**, so dying in lava won't destroy them.

---

## Installation

1. Install **Fabric Loader 0.19.5** for **Minecraft 26.3**.
2. Download and install **Java 25** if you don't have it.
3. Drop `fabric-api-0.162.0+26.3.jar` into your `mods/` folder.
4. Drop `eternal-potions-1.1.0.jar` into your `mods/` folder.
5. Launch Minecraft with the Fabric profile.

---

## Getting Started

1. **Craft an Eternal Shard.** See [Recipes](#recipes) below. It uses rare ingredients — this is not an early-game item.
2. **Brew an Eternal Potion.** Put a water bottle, blaze powder, and the Eternal Shard into a brewing stand. See [Brewing](#brewing).
3. **Drink it.** You get a random Eternal effect at Level I.
4. **Drink more.** Each additional potion levels up one of your existing effects, or rolls a new one if you don't have all four yet.
5. **Reach Level III** on any effect to become its **King**.

Your effects are permanent. They stay with you through death, respawn, milk, and totems — but only the King keeps their throne through a death.

---

## The Four Effects

Each effect has three levels. Higher levels **replace** lower ones — they don't stack.

| Effect | Level I | Level II | Level III (King) |
|---|---|---|---|
| **Eternal Speed** | +15% movement speed | +25% movement speed | +40% movement speed |
| **Eternal Strength** | +3 attack damage | +6 attack damage | +9 attack damage |
| **Eternal Regeneration** | Heal 1 HP every 50 ticks | Heal 1 HP every 25 ticks | Heal 1 HP every 12 ticks |
| **Eternal Resistance** | 20% damage reduction | 40% damage reduction | 60% damage reduction |

All four effects are **permanent** once acquired.

**Vanilla versions of these effects are blocked** while you have the Eternal equivalent. You cannot stack vanilla Speed with Eternal Speed, for example — the mod silently cancels the vanilla effect and tells you why.

---

## Vanilla Effects Are Nerfed

To make Eternal Potions feel worth grinding for, the vanilla versions of Speed, Strength, Regeneration, and Resistance have been **cut roughly in half** while this mod is loaded.

| Vanilla Effect | Normal Value | Nerfed Value |
|---|---|---|
| **Speed** | +20% per level | +10% per level |
| **Strength** | +3 damage per level | +1.5 damage per level |
| **Regeneration** | Heal every 50 / 25 / 12 ticks | Heal every 100 / 50 / 25 ticks |
| **Resistance** | 20% per level | 10% per level |

Eternal Potions still give their **full, un-nerfed** values. An Eternal Potion is therefore roughly **2× stronger** than the equivalent vanilla potion — and it never runs out.

---

## The King System

Each effect has exactly **one throne** at Level III. Only one player can be King of a given effect at a time.

### Becoming King

- **Drink an Eternal Potion** while you already have an effect at Level II. If the throne is empty, you're crowned.
- If the throne is taken, you stay at Level II. The King keeps their throne.
- An OP can force-crown you with `/eternal king crown`.

### Losing Your Throne

- **You die.** Your throne empties, your effects are removed, and you drop an Eternal Shard.
- **An OP dethrones you** with `/eternal king dethrone`.
- **Someone else crowns themselves** using `/eternal king crown` (this dethrones the current King).

### When You're King

- You get a **crown particle effect** above your head.
- Your HUD effect shows `Eternal Speed III` (or whichever effect you hold).
- The server broadcasts a message to everyone when you're crowned.
- If you fill all four thrones alone, the server makes a special announcement.

### Offline Kings

Kings **keep their throne while offline**. When they log back in, they still hold it.

### Throne Persistence

Thrones survive **server restarts**. If the server crashes or shuts down, the King data is saved and restored when it comes back up.

---

## Locator Bar & Brew Timer

Every brewing stand currently brewing an Eternal Potion shows up on the **locator bar** — the strip at the top of the screen, above the XP bar.

### What you see

- A **waypoint icon** appears on the locator bar for each active brew.
- The waypoint is **visible at any distance**, in any dimension, and **no recovery compass is required**.
- A **live timer** in `M:SS` format sits above each waypoint, showing time left in the brew.
- The timer **moves with the icon** as you rotate — the sprite slides left and right along the bar, and the timer follows it.
- When the icon would fall off the visible bar, both the icon and the timer hide.
- When the brew ends or the stand is destroyed, the waypoint is removed from the bar entirely.

### Why it matters

The 10-minute brew locks you in place. Anyone tracking the server can see where you are. The locator bar tells you — and everyone else — exactly how long you have until the potion is ready and where to hunt or defend.

---

## Commands

All `/eternal` commands require **OP** (permission level 2).

### Player Commands

| Command | What it does |
|---|---|
| `/kings` | Opens the Kings leaderboard menu. Client-side only. |

### Stand Commands

| Command | What it does |
|---|---|
| `/eternal stand stop <pos>` | Stops an Eternal brew, drops its contents, and destroys the stand. |
| `/eternal stand start <pos>` | Spawns a brewing stand at the position with blaze powder, a water bottle, and an Eternal Shard pre-loaded. Auto-starts the brew. If the chunk isn't loaded, the first attempt is refused — a second attempt at the same position force-loads the chunk. |

### Effect Commands

| Command | What it does |
|---|---|
| `/eternal effect give <player> <effect>` | Grants one level of an effect. Granting a third time crowns the player. |
| `/eternal effect take <player> <effect>` | Drops one level at a time: **III → II → I → gone**. Refuses on Kings (use `/eternal king dethrone`). Refuses if the target doesn't have the effect. |

### King Commands

| Command | What it does |
|---|---|
| `/eternal king crown <player> <effect>` | Force-crowns a player. Dethrones the current King if any. |
| `/eternal king dethrone <effect>` | Removes the King of an effect. They drop to Level II. |

### Item Commands

| Command | What it does |
|---|---|
| `/eternal item <player> <shard\|potion> <quantity>` | Grants the target 1 to 64 of either Eternal item. |

**Effect names accepted:** `speed`, `regeneration` (or `regen`), `resistance` (or `resis`), `strength`.

---

## Recipes

### Eternal Shard (Crafting Table)

[ O ][ H ][ O ]  
[ E ][ K ][ S ]  
[ O ][ T ][ O ]  


- **O** = Ominous Trial Key
- **H** = Heart of the Sea
- **E** = Ender Eye
- **K** = Echo Shard
- **S** = Totem of Undying
- **T** = Enchanted Golden Apple

This is deliberately expensive. Each shard requires significant progression.

### The Eternal Potion (Brewing Stand)

- **Ingredient:** Eternal Shard
- **Input:** Any Potion (Water Bottle, Awkward, or any other potion)
- **Output:** The Eternal Potion

---

## Brewing

Brewing an Eternal Potion is not like vanilla brewing.

### How it works

1. Place an Eternal Shard in the **ingredient slot** (top).
2. Place a **potion** in any of the three bottle slots (bottom).
3. Add **blaze powder** as fuel.
4. The brew begins. It takes **10 minutes** (12000 ticks).

### During the brew

- The brewing stand is **locked**. Nobody can open it, take items out, insert items, or break the block.
- All potions in the three slots are **consolidated to the center slot** at the start of the brew. If you put potions in multiple slots, the extras are discarded.
- A server-wide message announces the brew when it starts.
- A waypoint appears on the **locator bar** for everyone, with a live timer counting down.
- You cannot remove the shard mid-brew.

### Stopping a brew

An OP can run `/eternal stand stop <x> <y> <z>` to cancel a brew and destroy the stand. This drops the contents, so the shard and potion aren't lost — but the stand is gone. The waypoint clears from every player's locator bar.

### After the brew

The shard is consumed. The potion becomes **The Eternal Potion**. Take it out and drink it.

---

## Protection Rules

Your Eternal effects are protected against most ways of losing them:

- **Milk** doesn't remove Eternal effects. You keep them after drinking.
- **Totems of Undying** don't just save you — they preserve your Eternal effects on pop.
- **Admin `/effect clear`** on you does remove Eternal effects. That's intentional.
- **Death** removes your Eternal effects, but a shard is **always** dropped where you died. Both Eternal items are fireproof, so lava and fire don't destroy them. Only the void can lose the drop.

Vanilla Speed, Regeneration, Resistance, and Strength are **blocked** while you have the corresponding Eternal effect. You cannot stack them.

---

## Tips & Strategy

- **Prioritize one effect.** Reaching Level III of a single effect is faster than spreading yourself thin, and it makes you a King.
- **The resistance King is very hard to kill.** 60% damage reduction on top of armor makes you extremely tanky. Target them first in PvP.
- **The strength King is a burst threat.** +9 attack damage is more than a diamond sword's base damage. Do not melee them without a plan.
- **Losing your throne on death is brutal.** A King who dies in a fight loses the throne and drops a shard — the winner can pick it up and brew more.
- **Shards are farmable.** Kill a player who has any Eternal effect and they drop a shard 100% of the time. This makes PvP profitable.
- **Watch the locator bar.** Every active brew shows up there with a live timer. If you see a brew you didn't start, someone is nearby and vulnerable.
- **Stay near a brewing stand.** The 10-minute brew locks the stand and announces its position. Enemies will find it. Brew in a secure location.
- **Vanilla potions are half as strong.** If you're relying on vanilla Speed, Strength, Regen, or Resistance, you're at a disadvantage. Get an Eternal Potion.

---

## FAQ

**Q: Will my effects disappear if I drink milk?**
A: No. Milk is specifically protected against removing Eternal effects.

**Q: What happens if I die while holding a totem?**
A: The totem saves you AND preserves all your Eternal effects, including your throne if you're a King.

**Q: Can I have two Kings of the same effect?**
A: No. Each effect has exactly one throne.

**Q: What happens if I reach Level III of an effect whose throne is already taken?**
A: You stay at Level II. You do not become a King.

**Q: Can I be King of more than one effect?**
A: Yes. A single player can hold multiple thrones simultaneously.

**Q: Is there a way to remove my own Eternal effects?**
A: Ask an OP to run `/eternal effect take <you> <effect>`. There's no player-side way to remove them other than dying.

**Q: Does the brew finish if I unload the chunk?**
A: No. The brew only progresses while the chunk containing the brewing stand is loaded. Keep the area loaded or use a chunk loader.

**Q: Will the mod work with other mods?**
A: It should work with most. If another mod adds effects with the same names or blocks the same vanilla effects, conflicts are possible.

**Q: Why are vanilla potions weaker now?**
A: To make Eternal Potions feel rewarding. Vanilla Speed, Strength, Regeneration, and Resistance are each cut to roughly half their normal values while this mod is installed.

**Q: What happens if I join with the wrong mod version?**
A: You'll be disconnected with a message telling you exactly which version to install. Update the mod and try again.

**Q: Do I need a recovery compass to see the brew waypoints?**
A: No. The locator bar is forced on for everyone while the mod is loaded. You'll see brew waypoints without any compass.

---

## Development or Contributing

Please refer to this file if you would like to do either:

https://github.com/Osaidii/Eternal-Potions/blob/main/DEVELOPMENT.md

---

## Disclosure

The Code for this mod was written by AI under heavy supervision by humans. The design was totally human made.

---

## License

Apache License 2.0 — Open Source. See `LICENSE` for the full text.
