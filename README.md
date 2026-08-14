# EpicHoppers

EpicHoppers is a Bukkit/Spigot/Paper server plugin that both optimizes
hoppers and adds upgradeable behavior to them: players level up placed
hoppers via an in-game GUI, gaining range, items-per-transfer, item
filtering (whitelist/blacklist/void), hopper-to-hopper item teleportation,
and optional per-level "modules" (block-break-above, auto-crafting, item
suction). Main command: `/epichoppers` (alias `/eh`); soft-depends on
Vault (economy upgrades) and WildStacker.

![N|Solid](https://i.imgur.com/jKtE7ZM.png)

## Provenance and licensing

This repo is a genuine GitHub fork of
[electro2560/EpicHoppers](https://github.com/electro2560/EpicHoppers),
itself a mirror of the original 2017-era Songoda source
(`com.songoda.epichoppers`). Two things worth knowing:

- A separately maintained same-lineage plugin exists at
  `Songoda-Plugins/EpicHoppers` under **CC BY-NC-ND 4.0** (NoDerivatives).
  **No code from it has been copied or adapted here** — all porting in this
  repo is original work against this fork's own pre-existing source.
- This repo keeps its original custom license (permissive use, no
  redistribution/resale) — see `LICENSE`.

## Modernization work

- **The dead Arconix dependency is gone.** The original build depended on
  Songoda's Arconix utility library (config handling, text formatting, GUI
  glass helpers, location serialization, particle broadcast), which is
  unobtainable from any live Maven repository. Every call site was replaced
  with small hand-written equivalents against vanilla Bukkit/Paper API.
- Rebuilt on modern Gradle (9.x wrapper, Java toolchains), `api-version`
  raised to current, and an extensive JUnit unit-test suite added: hopper/
  filter/level managers, all modules, every subcommand, listeners,
  teleport/enchantment/hop handlers, YAML storage, locale handling, boost
  data, and the public API.
- Full history, per-milestone status, and the honest feature matrix are in
  `PLAN.md`; architecture notes in `CLAUDE.md`.

## Supported Paper versions

One codebase, no version branches. Default build targets the newest stable
Paper API (currently **26.2**); the same source compiles cleanly against
older API lines:

```sh
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
./gradlew build                                             # 26.2 (default)
./gradlew build -PpaperApiVersion=1.21.11-R0.1-SNAPSHOT
./gradlew build -PpaperApiVersion=1.20.1-R0.1-SNAPSHOT
./gradlew build -PpaperApiVersion=1.19.4-R0.1-SNAPSHOT
./gradlew build -PpaperApiVersion=1.18.2-R0.1-SNAPSHOT
```

All five targets are verified builds. The jar lands in `build/libs/`.

## Testing

1. **Unit tests** — `./gradlew check` (JUnit 5, see suite under
   `src/test/java`).
2. **Headless Paper boot smoke test** (opt-in — needs a real Paper server
   jar):

   ```sh
   ./gradlew paperBootTest -PpaperServerJar=/path/to/paper-26.2-111.jar
   ```

   Boots a real headless Paper server with the packaged jar in `plugins/`
   and asserts: the jar loads, `onEnable` doesn't throw, `/epichoppers` is
   registered in the live command map and doesn't throw when invoked, the
   plugin shows in `plugins`, and `onDisable` exits cleanly. Without a
   server jar the task reports `SKIPPED (this is a skip, not a pass)`.
   Transcript: `build/paper-boot/paper-boot-test.log`.
