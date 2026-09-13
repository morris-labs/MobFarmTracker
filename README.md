# Mob Farm Tracker

A NeoForge mod for Minecraft 1.21.1 that tracks the mob population, spawn
rate, and death rate around each player, and shows the results on a HUD
overlay and an in-game detail screen.

Built for tuning mob farms: point it at a farm fed by spawners or Dreadful
Dirt, and see whether it's actually keeping up, and whether kills are coming
from the farm's mechanism or from mobs crushing each other in a hopper.

## Features

- **Population count** — how many mobs are within a configurable chunk
  radius of you, using the same square region Minecraft uses for view and
  simulation distance.
- **Spawn and death rates** — per-second rates over a rolling window, split
  into cramming deaths (mobs crushed by overcrowding) and every other cause,
  so you can tell your farm's kill mechanism apart from cramming losses.
- **Average time to death** — how long a mob type survives after spawning,
  averaged over the same rolling window.
- **Per-mob-type breakdown** — a detail screen listing every tracked entity
  type with its own count and rates, sortable by count.
- **Hostile / passive / both filter** — track only hostile mobs, only
  passive and neutral mobs, or everything.
- **Adjustable in-game** — radius, sample window, and the mob filter are all
  changeable from the detail screen; no config file edit or restart needed.
- **On/off toggle** — a keybind and a HUD button pause tracking without
  removing the mod.

## Requirements

- Minecraft 1.21.1
- [NeoForge](https://neoforged.net/) 21.1.247 or later
- Install the mod on **both** the client and the server. Tracking runs on
  the server, because only the server knows a mob's real cause of death;
  results are sent to each client for display.

## Installation

1. Download the mod JAR from the
   [Releases](../../releases) page, or build it yourself (see
   [Building from source](#building-from-source)).
2. Copy the JAR into the `mods` folder of your Minecraft client and of the
   server that hosts your farm.

## Usage

The HUD overlay appears in the top-left corner as soon as you join a world,
showing the current count and the spawn, kill, and cramming rates.

| Action | Default key |
| --- | --- |
| Open the detail screen | Unbound — set one under **Options > Controls > Key Binds > Mob Farm Tracker** |
| Toggle tracking on or off | Unbound — set one under the same menu |

The detail screen lists every tracked mob type with its count, spawn rate,
kill rate, cramming rate, and average lifespan. From there you can also
change your tracking radius, sample window, and mob filter — these apply
immediately and are remembered for your player until the server restarts.

## Configuration

Server operators can set defaults in `config/mobfarmtracker-common.toml`,
generated the first time the server starts:

| Setting | Default | Description |
| --- | --- | --- |
| `radiusChunks` | `5` | Default tracking radius, in chunks. |
| `rateWindowSeconds` | `10` | Default length of the rolling window used for per-second rates. |
| `populationSampleIntervalTicks` | `5` | How often, in ticks, to rescan the world for the current population. Lower values are more responsive but cost more server time per scan. Prefer `5` (4 scans/second) over `1` (20 scans/second) unless you've confirmed your hardware handles it at scale. |

Players can override the first two settings, plus the mob filter, from the
in-game detail screen. Per-player settings live in memory only and reset to
these defaults when the server restarts.

## Building from source

Requires JDK 21.

```sh
./gradlew build
```

The output JAR is written to `build/libs/`.

To test in a development environment:

```sh
./gradlew runClient
./gradlew runServer
```

## License

[MIT](LICENSE)
