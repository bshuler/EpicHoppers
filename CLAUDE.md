# CLAUDE.md — EpicHoppers

## What this is

EpicHoppers is a Bukkit/Spigot/Paper **server plugin** (not a client mod, not a
Fabric/Forge/NeoForge mod). It lets players level up placed hoppers via an
in-game GUI, gaining range, item-per-transfer amount, filtering, hopper-to-
hopper item teleportation, and optional "module" behaviors (block-break-above,
auto-crafting, item suction) as they upgrade with XP/economy.

This is a 2017-era Songoda plugin (`com.songoda.epichoppers`). See `PLAN.md`
for the full modernization plan, milestone status, and the honest
platform/version support matrix — read it before assuming any feature works
on a given target.

## Provenance and licensing (binding)

- **This repo is a genuine GitHub fork**, not a standalone snapshot (unlike
  this repo's sibling `EpicFurnaces`, which is a snapshot). `git remote -v`
  shows `origin` = `bshuler/EpicHoppers` and `upstream` =
  `electro2560/EpicHoppers`; `gh api repos/bshuler/EpicHoppers --jq
  '.parent.full_name'` confirms the fork parent is `electro2560/EpicHoppers`.
  `electro2560/EpicHoppers` is itself a plain mirror/fork of the original
  Songoda source (same package `com.songoda.epichoppers`, same 2017-era file
  layout) — not the actively-maintained lineage.
- A genuinely-maintained same-lineage plugin exists at
  `github.com/Songoda-Plugins/EpicHoppers` (same package ancestry, actively
  developed). It is licensed **CC BY-NC-ND 4.0** (NonCommercial,
  **NoDerivatives**). Because this repo is public, that license **forbids
  copying or adapting any of its code** here — it may only be used as
  architectural/conceptual reference (e.g. "how did the modern version
  restructure the hopper manager"), never as a source of pasted or
  lightly-modified code. No code has been copied from it; all porting in this
  repo is original work against this repo's own pre-existing fork source.
- The modern lineage also depends on `SongodaCore`, a large shared
  multi-Minecraft-version NMS abstraction library, and this plugin's original
  form depended on a sibling Songoda utility library, **Arconix**, for config
  handling, text formatting, GUI glass helpers, location serialization, and a
  particle-broadcast wrapper. Both are unobtainable from any live Maven
  repository and carry the same restrictive license family. **This port
  removes the Arconix dependency entirely**, replacing each call site with
  small hand-written equivalents against vanilla Bukkit/Paper API (see
  "Removed dependencies" below).
- This repo's own `LICENSE` (a custom permissive-but-no-redistribution
  license from the original 2018 author, Brianna O'Keefe) predates and is
  independent of the above; it already forbids redistributing/selling the
  plugin. Nothing in this modernization changes that.

## Architecture

Single Bukkit `JavaPlugin` (`EpicHoppersPlugin`), event-driven:

- `EpicHoppersPlugin` — plugin entry point, owns all manager singletons,
  registers listeners and (previously) protection-plugin hooks.
- `hopper/` — `EHopper` (a placed hopper's runtime state: level, filter,
  owner, sync/teleport target), `EHopperManager` (location→hopper map),
  `EFilter` (whitelist/blacklist/voidlist item filter), `levels/ELevel` /
  `ELevelManager` (level definitions: cost, range, amount, module list —
  loaded from config), `levels/modules/` (`ModuleAutoCrafting`,
  `ModuleBlockBreak`, `ModuleSuction` — optional per-level behaviors).
- `handlers/` — `HopHandler` (per-tick hopper→inventory/furnace item
  transfer loop, `hopperRunner()`), `TeleportHandler` (hopper-chain
  teleportation), `EnchantmentHandler` ("Sync Touch" enchanted-book lore
  encode/decode).
- `listeners/` — `BlockListeners` (place/break, max-hoppers-per-chunk limit,
  sync-touch-on-break), `EntityListeners` (mob-drop-to-chest sync-touch),
  `HopperListeners` (cancels vanilla `InventoryMoveItemEvent` so
  `HopHandler` owns all transfer logic), `InteractListeners` (click-to-open
  GUI dispatch), `InventoryListeners` (GUI click handling for
  overview/crafting/filter menus).
- `command/` — `CommandManager` + subcommands (`give`, `book`, `boost`,
  `epichoppers`, `reload`, `settings`), `AbstractCommand` base class.
- `utils/` — `Methods` (glass/particle/name-formatting helpers),
  `SettingsManager` (in-game settings-editor GUI + the `Setting` enum mapping
  legacy config keys to current ones), `MySQLDatabase` (optional MySQL
  storage backend), `Debugger`.
- `player/` — per-player transient state (`PlayerData`/`PlayerDataManager`,
  `MenuType`, `SyncType`).
- `boost/` — timed per-player hopper-output multiplier (`BoostData`/
  `BoostManager`), set via `/eh boost`.
- `storage/` — pluggable persistence abstraction (`Storage` base class,
  `StorageRow`/`StorageItem`), with `types/StorageYaml` (default,
  `data.yml`) and `types/StorageMysql` (optional, see porting notes).
- `Locale.java` — `.lang` file loader/generator (own small class, no
  external deps; unrelated to Arconix).
- `EpicHoppers-API/` — a small standalone interface module
  (`com.songoda.epichoppers.api`) meant for other plugins to depend on
  without pulling in the implementation. Interfaces only, no third-party
  imports — required no changes.
- `hooks/` — **excluded from the build**, relocated to `legacy-hooks/` (see
  below).

### Removed dependencies

| Removed | Why | Replacement |
|---|---|---|
| Arconix (`com.songoda.arconix:*`) | Dead private Maven repo (`repo.songoda.com`), CC BY-NC-ND-family license | Hand-written: `ChatColor.translateAlternateColorCodes('&', ...)` for text formatting, `DyeColor` + `Material.*_STAINED_GLASS_PANE` for GUI glass, a small local location-string serializer, a hidden-character encoder for lore-embedded metadata, `location.getWorld().spawnParticle(...)` directly instead of Arconix's `packetLibrary.getParticleManager().broadcastParticle(...)`, `Integer.parseInt` try/catch instead of Arconix's `AMath.isInt` |
| `org.json.simple` + `update()` update-checker | Called `http://update.songoda.com/...`, long dead | Removed outright |
| NMS-package version-sniffing in `checkVersion()` | Obsolete/breakable string-splitting on `org.bukkit.craftbukkit.vX_Y_RZ`; modern Paper is Mojang-mapped, no such package | Removed; `plugin.yml`'s `api-version` is the real compatibility gate |
| `xyz.wildseries.wildstacker.api.WildStackerAPI` import (no Maven coordinate in the original `pom.xml` at all — a build gap in the fork, not a dead dependency) | WildStacker rebranded groupId/package from `xyz.wildseries` to `com.bgsoftware` years ago | Repointed to the live coordinate `com.bgsoftware:WildStackerAPI:2026.2` from `https://repo.bg-software.com/repository/api/`; verified live via `maven-metadata.xml` and by unzipping the jar — `getItemAmount(Item)` has the same signature the calling code (`ModuleSuction`) already expected, so only the import path changed, no logic rewrite |
| `com.mysql.jdbc.Driver` legacy JDBC driver class name in `MySQLDatabase.java` | Old pre-`cj` driver class name; used only via `Class.forName(String)` (not an import), so it was never a compile blocker, only a latent runtime failure for anyone enabling MySQL storage | Updated the string to `com.mysql.cj.jdbc.Driver`. No MySQL connector dependency is declared here (never was) — a maintainer enabling `Database.Activate Mysql Support` must still supply a JDBC driver on the server's classpath themselves |

### Excluded: the 9 protection-plugin hooks

`hooks/Hook{ASkyBlock,Factions,GriefPrevention,Kingdoms,PlotSquared,
RedProtect,Towny,USkyBlock,WorldGuard}.java` each integrate with a specific
land-claim plugin at long-dead or version-incompatible Maven coordinates
(e.g. `WorldGuard 6.1.1-SNAPSHOT`, `Towny 0.92.0.0`, `PlotSquared 18.05.01`).
None of those coordinates resolve today. These files are **relocated to
`legacy-hooks/` (not compiled, not deleted)** and their registration calls
removed from `EpicHoppersPlugin.onEnable()`. This is a real, intentional
feature reduction — softdepend integration with those specific protection
plugins does not currently work. See `PLAN.md` for what it would take to
restore each one (repoint to each plugin's current Maven coordinates and
re-enable).

Note: `EpicHoppers` (unlike `EpicFurnaces`) is a fork of a repository named
`bshuler/GriefPrevention` elsewhere in this same `minecraft-mods` workspace
being independently modernized — that is a coincidence of naming (this
plugin's `HookGriefPrevention.java` hooks the *actual* GriefPrevention
Bukkit plugin, an unrelated third-party project by `TechFortress`/
`ryanhamshire`, pinned here at the dead coordinate
`com.github.TechFortress:GriefPrevention:16.7.1`), not a code relationship
between the two repos.

## Platforms

This is Bukkit-API software. "Platform" here means Bukkit-API server
implementations, not mod loaders:

- **Paper** (primary target) — and by API compatibility, **Purpur**.
- **Folia** — untested; `HopHandler`'s per-tick global-state hopper-transfer
  loop and `TeleportHandler`'s cross-chunk-chain teleportation both make
  assumptions (single global tick, freely reading/writing blocks/entities
  across chunk boundaries in one pass) that Folia's region-threaded model is
  specifically designed to break. Flagged as an open problem, not claimed
  working.
- **Spigot** — the plugin only uses stable Bukkit/Spigot API, no Paper-only
  calls, so it should run on plain Spigot too, just without any Paper-only
  optimizations (none are currently used).
- **Fabric / NeoForge / Forge are not applicable.** See `PLAN.md` milestone 3
  for the full reasoning (independently re-derived for this plugin's own
  hopper/item-transport/teleport/GUI subsystems, not copy-pasted from
  `EpicFurnaces`) and what was actually evaluated before reaching this
  conclusion.

See `PLAN.md` for the version matrix and per-version build status.

## Build

```bash
./gradlew build          # compiles, shades, produces build/libs/EpicHoppers-<version>.jar
./gradlew shadowJar       # same, explicit task name
```

- Gradle 9.x, Java 25 toolchain (auto-provisioned via the foojay resolver —
  do not install a system JDK for this). Java 25 is required because the
  latest `paper-api`'s Gradle module metadata declares it as a minimum. The
  only installed system JDK (Temurin 21) is untouched. Older-version builds
  (see `PLAN.md` milestone 4) may use an older toolchain if their
  corresponding `paper-api` allows it.
- Single consolidated module (the old broken two-module Maven layout
  — `EpicHoppers-API` + `EpicHoppers-Plugin` sharing one `pom.xml` with no
  `<modules>` declared — is replaced by one Gradle project; the API package
  is still a distinct, unchanged source package within it).
- Depends on `io.papermc.paper:paper-api` at the latest resolvable version
  from `repo.papermc.io/repository/maven-public/`. Do not trust a cached
  memory of "the latest MC version" — query
  `https://fill.papermc.io/v3/projects/paper` or the maven-metadata.xml for
  `paper-api` at build time; Minecraft is calendar-versioned now (26.x).
- Override the target `paper-api` version at build time:
  `./gradlew clean shadowJar -PpaperApiVersion=<coord>` (see `PLAN.md`
  milestone 4 for the exact coordinates used per MC version).

## Porting notes for whoever touches this next

- `SettingsManager.onInventoryClick` used `event.getInventory().getTitle()`,
  which no longer exists on modern Bukkit — switched to
  `InventoryClickEvent.getView().getTitle()`. Two call sites.
- `Methods.isSync(Player)` and `EntityListeners.onDrop` called
  `Player#getItemInHand()` directly — that method lives on
  `PlayerInventory`, not `Player`/`HumanEntity`, on modern Bukkit. Switched to
  `player.getInventory().getItemInMainHand()`.
  `BlockPlaceEvent#getItemInHand()` (used in `BlockListeners.onBlockPlace`)
  is a *different*, still-current method on the event class itself — left
  unchanged.
- The `Setting` enum in `SettingsManager.java` has an
  `Main.Upgrade Particle Type` default of `"WITCH_MAGIC"` — not a valid
  `Particle` enum constant on any Paper API version. Corrected the default
  to `"WITCH"`. This value is only read at runtime via
  `Particle.valueOf(String)`/`Methods.resolveParticle(String)` from a config
  default, not referenced as a compile-time enum constant, so it was never a
  compile blocker across any target version — but Bukkit renamed this
  particular constant around the Minecraft 1.20.5 API bump
  (`SPELL_WITCH` -> `WITCH`; also `SPELL` -> `EFFECT` elsewhere), so a plain
  `Particle.valueOf("WITCH")` would throw `IllegalArgumentException` at
  runtime on the older Paper API targets walked in `PLAN.md` milestone 4
  (1.18.2/1.19.4/1.20.1 only have `SPELL_WITCH`, not `WITCH` — confirmed via
  `javap` against each cached `paper-api` jar). Fixed by adding
  `Methods.resolveParticle(String name)`: tries the requested name first,
  then a small symmetric old&lt;-&gt;new alias table, mirroring the
  `resolveParticle` fallback helper already used in the sibling
  Spigot-InvUnload plugin. All three `Particle.valueOf(...)` call sites
  (`Methods.doParticles`, `Methods.broadcastParticle`, `EHopper.upgradeFinal`)
  now go through this helper, so the single `"WITCH"` config default
  resolves correctly on every Paper API version from 1.18.2 through 26.2
  without needing per-version config defaults.
- `Main.BlockBreak Particle Type` defaults to `"LAVA"`, which *is* a valid
  modern `Particle` constant — left as-is.
- `config.yml`/`SettingDefinitions.yml`'s runtime-generated defaults come
  from `SettingsManager.updateSettings()`/the `Setting` enum, not a shipped
  `config.yml` resource (this plugin has never shipped one, unlike
  `EpicFurnaces`) — first-run behavior generates it from the enum above.
- `AsyncPlayerChatEvent` (used in `SettingsManager.onChat`) is
  soft-deprecated on Paper in favor of the Adventure-based
  `io.papermc.paper.event.player.AsyncChatEvent`. Left as-is: the legacy
  event still compiles and fires on current Paper; migrate to the Paper
  event only if/when Spigot compatibility is deliberately dropped.
- `WildStackerAPI` integration (`ModuleSuction`) is a genuine soft-dependency
  gated by `Bukkit.getPluginManager().isPluginEnabled("WildStacker")` at
  runtime — it compiles against `com.bgsoftware:WildStackerAPI:2026.2` as
  `compileOnly` and does nothing if WildStacker isn't installed. Unlike the
  9 protection hooks, this one has a live, current Maven coordinate, so it
  was repointed rather than relocated to `legacy-hooks/`.
- Two call sites used the legacy 3-argument `ItemStack(Material, int, short)`
  / `(Material, int, byte)` constructor together with `Block#getData()` or a
  hand-computed byte data value: `BlockListeners`'s silk-touch harvest
  (`new ItemStack(e.getBlock().getType(), 1, e.getBlock().getData())`) and
  `SettingsManager.openSettingsManager`'s category icon
  (`new ItemStack(Material.WHITE_WOOL, 1, (byte) (slot - 9))`). Block
  data-value subtypes don't exist in modern Bukkit's material system, so
  both were simplified to the 2-argument `ItemStack(Material, int)`
  constructor. The `SettingsManager` one already carried a pre-existing
  `//ToDo: Make this function as it was meant to.` comment marking that
  category-icon behavior as already non-functional in the original 2017
  source — left as a `//ToDo`, not a regression introduced by this port.
- `ConfigWrapper` (Arconix) is replaced by a small new hand-written
  `utils/YamlDataFile` class (constructor `(JavaPlugin, String fileName)`,
  `getConfig()`/`saveConfig()`) rather than three separate ad-hoc
  replacements, since `Storage`'s `dataFile`, `SettingsManager`'s `defs`, and
  `EpicHoppersPlugin`'s `hooksFile` all only ever call `getConfig()`/
  `saveConfig()` on their respective config-wrapper field — one reusable
  class covers all three call sites with no other code changes needed.
- `plugin.yml`'s `softdepend` originally listed all 9 now-relocated
  protection plugins plus `WildStacker`/`Vault`; trimmed down to
  `[WildStacker, Vault]` — the two that still have live integration code —
  since softdepend on a plugin whose hook was relocated to `legacy-hooks/`
  is stale metadata, not a real dependency. Also switched `version: 3.1` to
  the `${version}` Gradle-templated placeholder to match `build.gradle.kts`'s
  `processResources` filtering (already wired for this token from an earlier
  pass) instead of a hardcoded literal that would drift from
  `gradle.properties`'s `pluginVersion`.
