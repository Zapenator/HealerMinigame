# HealerMinigame

A standalone Paper plugin that turns CivLabs' healer **revive minigame** into a
solo **training** exercise — start it on demand, get timed, and race for the
fastest revive on a server leaderboard.

> **Origin & credit:** The revive minigame originated from **CivLabs'** MiniEvents
> plugins. This project extracts just that minigame into a self-contained plugin
> with no dependency on the (closed-source) MiniEvents code. Only *how the
> minigame is accessed* was changed — the mechanics are unchanged from the
> original. All credit for the minigame goes to CivLabs.
> Original source: https://github.com/Emergent-Civilization/MiniEvents-Specialization
> See [NOTICE](NOTICE) for details.

## How it works

Run the command and a "Healer Training" chest opens, seeded with a random mix of
red **injury** items and green **healthy organ** items:

- Click an **injury** → it becomes a bandage, and the green progress bar advances.
- Click a **healthy organ** → you're punished with 3 fresh injuries.
- Clear every injury to complete the revive. Your time is recorded, and if it
  beats your previous best you're told your new rank.

## Commands

No permissions are required — every player can use these.

| Command | Aliases | Description |
| --- | --- | --- |
| `/healer start` | `play`, `train` | Begin a timed training run. |
| `/healer leaderboard` | `lb`, `top`, `board` | Show the fastest recorded times. |
| `/healer personalBest` | `pb`, `best`, `mytime` | Show your best time and rank. |

Best times are stored per player in `plugins/HealerMinigame/leaderboard.yml`.

## Requirements

- Paper 1.21.x server (built and tested against 1.21.11)
- Java 21

## Building

The project uses Gradle. From the project root:

```bash
./gradlew build
```

The plugin jar is produced under `build/libs/`.

> **Note:** the scaffold's `run-paper` plugin (3.1.0) requires Gradle 9.7+. If
> your Gradle wrapper is older, either build through an IDE with a bundled 9.7+
> Gradle or bump `gradle/wrapper/gradle-wrapper.properties`.

## Running a test server

Drop the built jar into a Paper server's `plugins/` folder and start the server,
or use the `run/` folder if present (`run/start.bat` on Windows) which contains a
ready-to-go Paper server with the plugin installed.
