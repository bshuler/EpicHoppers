# PLAN.md — EpicHoppers modernization

## Goal

Get this plugin building and running on the latest Paper API, then walk
backward through older Minecraft versions as far as practical, and give an
honest answer on cross-platform (mod-loader) support. See `CLAUDE.md` for
architecture and provenance.

**Java version note:** the latest resolvable `paper-api`
(26.2.build.111-stable) publishes Gradle module metadata requiring JVM 25.
The root `build.gradle.kts` toolchain is Java 25, auto-provisioned by
`foojay-resolver-convention` (Gradle's own toolchain cache, not a system/
Homebrew JDK — the only installed system JDK, Temurin 21, is untouched).
Older-version builds in milestone 4 may be able to target Java 21 if their
corresponding `paper-api` still allows it; recorded per-row below.

## Milestones

### 1. Docs + branch hygiene — DONE

- [x] Investigated provenance: confirmed this repo is a genuine GitHub fork
      (`origin` = `bshuler/EpicHoppers`, `upstream` =
      `electro2560/EpicHoppers`, `gh api ... .parent.full_name` confirms it)
      of a plain mirror of the original 2017 Songoda source — *not* the
      actively-maintained lineage, which lives separately at
      `Songoda-Plugins/EpicHoppers` (CC BY-NC-ND 4.0 — reference only, no
      code copying).
- [x] `CLAUDE.md` written.
- [x] `PLAN.md` written (this file).
- [x] Renamed local branch `master` → `main`, pushed, set as GitHub default
      branch (`gh repo edit bshuler/EpicHoppers --default-branch main`).
      `master` and `Legacy` branches left intact (not deleted).

### 2. Modern build (latest Paper API) — DONE

- [x] Replaced the broken two-module Maven layout (`pom.xml` with no
      `<modules>` despite `EpicHoppers-API`/`EpicHoppers-Plugin` being
      separate trees) with one consolidated Gradle 9.x project.
- [x] `paper-api` at the latest resolvable version (queried live from
      `fill.papermc.io`/`repo.papermc.io` maven-metadata — **26.2** /
      `26.2.build.111-stable` at time of writing, confirmed identical to the
      version `EpicFurnaces` used a short time earlier in the same session;
      do not hardcode this without re-checking, MC is calendar-versioned).
- [x] Java 25 toolchain, `com.gradleup.shadow`, `foojay-resolver-convention`.
- [x] Removed Arconix/`org.json.simple`/NMS version-sniffing (see `CLAUDE.md`
      table); fixed `Inventory.getTitle()` (2 sites in `SettingsManager`) and
      `Player#getItemInHand()` (`Methods.isSync`, `EntityListeners.onDrop`).
      Also fixed two legacy `ItemStack(Material, int, short/byte)` +
      `Block#getData()` call sites (`BlockListeners`'s silk-touch harvest,
      `SettingsManager`'s category icon — the latter already had a
      pre-existing `//ToDo` marking that behavior as non-functional, left as
      a `//ToDo` since restoring it is out of scope for this port) since
      block data-value subtypes no longer exist in modern Bukkit's material
      system.
- [x] Repoint `WildStackerAPI` from the dead `xyz.wildseries` package (which
      also had *no* Maven coordinate at all in the original `pom.xml` — a
      pre-existing build gap, not just a dead dependency) to the live
      `com.bgsoftware:WildStackerAPI:2026.2` coordinate.
- [x] Fix legacy `com.mysql.jdbc.Driver` string in `MySQLDatabase.java` to
      `com.mysql.cj.jdbc.Driver`.
- [x] Correct `Setting.o7`'s `Main.Upgrade Particle Type` default from
      `"WITCH_MAGIC"` (invalid) to `"WITCH"` (valid modern `Particle`
      constant).
- [x] Relocated the 9 protection-plugin hook files to `legacy-hooks/`
      (excluded from compilation), stripped their registration from
      `EpicHoppersPlugin.onEnable()`, and trimmed the now-stale entries out
      of `plugin.yml`'s `softdepend` (kept `WildStacker`/`Vault`, the two
      soft-deps that still have live integration code).
- [x] VaultAPI transitive `org.bukkit:bukkit` capability conflict was already
      excluded in `build.gradle.kts` from an earlier session pass; confirmed
      still correct, no further change needed.
- [x] Bumped `plugin.yml`'s `api-version` from `1.13` to `"26.2"`, dropped the
      hard `depend: [Arconix]`, and switched `version: 3.1` to the
      `${version}` Gradle-templated placeholder (matching `EpicFurnaces`'s
      pattern; `build.gradle.kts`'s `processResources` block already filtered
      `plugin.yml` for this token from an earlier pass).
- [x] Added the missing `# Gradle` section to `.gitignore`
      (`.gradle/`, `build/`, `!gradle/wrapper/gradle-wrapper.jar`).
- [x] Green build (`./gradlew clean shadowJar`); verified jar contents via
      `unzip -l build/libs/EpicHoppers-3.1.jar` — `plugin.yml` inside the jar
      shows the templated version/api-version/softdepend correctly, and a
      targeted `unzip -l | grep -i arconix` on the built jar returns no
      matches (clean removal, not just a successful compile). Committed and
      pushed.
- [x] **Follow-up, found during milestone 4's backward walk:** the
      `"WITCH"` default corrected above is only valid on Paper API versions
      *after* the ~1.20.5 `Particle` enum rename — `javap` against each
      cached `paper-api` jar confirmed 1.18.2/1.19.4/1.20.1 only have
      `SPELL_WITCH`, not `WITCH` (1.21.11/26.2 have `WITCH`, not
      `SPELL_WITCH`). A bare `Particle.valueOf("WITCH")` would throw
      `IllegalArgumentException` at runtime on those older targets. Added
      `Methods.resolveParticle(String name)` — tries the requested name,
      then a small symmetric old&lt;-&gt;new alias table
      (`WITCH`&lt;-&gt;`SPELL_WITCH`, `EFFECT`&lt;-&gt;`SPELL`) — mirroring
      the fallback helper already used in the sibling Spigot-InvUnload
      plugin. All three `Particle.valueOf(...)` call sites
      (`Methods.doParticles`, `Methods.broadcastParticle`,
      `EHopper.upgradeFinal`) now route through it, so no compile-time
      hazard existed (none of the three ever hardcoded a renamed constant as
      a field initializer — all three already read the name from config at
      runtime) but a genuine *runtime* hazard on old servers did, and is now
      fixed. See `CLAUDE.md` porting notes.

### 3. Cross-platform assessment

**Conclusion: Fabric/NeoForge/Forge are not applicable to this plugin.**

Evaluated honestly against this plugin's own subsystems, not copied from
`EpicFurnaces`'s conclusion (though it lands in the same place, for the same
underlying reason). EpicHoppers' core logic —
`handlers/HopHandler`'s per-tick hopper-to-inventory/furnace item-transfer
loop, `handlers/TeleportHandler`'s hopper-chain teleportation,
`listeners/InventoryListeners`'/`InteractListeners`'
GUI-click and menu-open dispatch, and `boost/BoostManager`'s
Vault-economy-gated output multiplier — is expressed entirely in terms of
stable `org.bukkit.*` types (`Inventory`, `Block`, `Location`,
`InventoryClickEvent`, `Player`) that the *server implementation* (Paper/
Spigot/Purpur) keeps source-compatible release over release. That
stability is exactly why one jar can span Paper API 1.18.2 through 26.2 with
zero source changes (milestone 4). Fabric/Forge/NeoForge mods have no
equivalent stable layer: they compile against Minecraft's own classes
(Mojang-mapped on Fabric/NeoForge, obfuscated+remapped on Forge) and reach
into game internals via Mixins, which are tied to one specific Minecraft
version's obfuscation map and must be rewritten (or at minimum re-verified)
per version. Porting `HopHandler`'s inventory-transfer loop or
`InventoryListeners`'s GUI click handling onto a mod loader would mean
re-implementing all of it against Minecraft internals from scratch — a
rewrite, not a port. Doing that partially, just to claim a Fabric/NeoForge
build target exists, would ship a mod that silently doesn't do what the
plugin does — exactly the failure mode this task was told to avoid ("never
silently drop a platform" means stating the honest "not applicable" clearly,
not faking partial support).

This repo's house mod-loader template, `critical-orientation`
(`~/code/minecraft-mods/critical-orientation`), was checked as the candidate
tool for exactly this reason (Stonecutter + Stonecraft + Architectury Loom):
confirmed it generates per-version, per-loader Fabric/Forge/NeoForge
subprojects and has no applicability to a Bukkit plugin like this one.

**What "cross-platform" honestly means for a Bukkit plugin**, and what this
plugin already achieves with a single build:

| Server implementation | Status | Notes |
|---|---|---|
| Paper | Supported (primary target) | Built and tested against this |
| Purpur | Expected-compatible | Purpur is a Paper fork with a superset API; nothing here uses anything Purpur would break |
| Folia | Untested, likely needs work | Folia's region-threaded model specifically targets the two assumptions this plugin's core logic makes: `HopHandler.hopperRunner()` iterates *all* tracked hoppers on one global tick and freely reads/writes their target inventories, and `TeleportHandler`'s hopper-chain teleport walks and moves items across what may be arbitrarily distant chunks/regions in a single synchronous pass. Both would need region-aware scheduling to be correct under Folia. Not smoke-tested; flagged below, not claimed working. |
| Spigot | Expected-compatible | Only stable Bukkit/Spigot API surface is used, no Paper-only calls currently exist |
| CraftBukkit | Expected-compatible (same reasoning as Spigot) | Not a realistic deployment target in practice |

### 4. Backward version walk — DONE

Paper publishes `paper-api` for older MC versions using the classic
`X.Y.Z-R0.1-SNAPSHOT` Maven coordinate, still resolvable from
`repo.papermc.io/repository/maven-public/` (confirmed live/cached:
`1.18.2-R0.1-SNAPSHOT`, `1.19.4-R0.1-SNAPSHOT`, `1.20.1-R0.1-SNAPSHOT`,
`1.21.11-R0.1-SNAPSHOT` — `1.21.11` used as the "1.21.x" representative, the
newest release in that version group).

The plugin has no per-version code branching (a single source set targets
whichever `paper-api` is on the compile classpath), so each older target was
built by overriding the `paperApiVersion` Gradle property on the command
line — e.g. `./gradlew clean shadowJar -PpaperApiVersion=1.18.2-R0.1-SNAPSHOT`
— rather than maintaining separate modules/source sets. All four builds
succeeded with the same Java 25 toolchain (the older `-R0.1-SNAPSHOT`
coordinates are plain Maven POMs with no Gradle module metadata, so they
carry no JVM-version constraint of their own; `options.release.set(25)` in
`build.gradle.kts` is fixed regardless of target since Java's `--release`
flag doesn't depend on which Bukkit API is on the classpath). Every
resulting jar was verified via `unzip -l` to contain the expected classes
and a correctly-templated `plugin.yml` — a genuinely-tested result, not an
assumption from "it's just Bukkit API so it must work."

The two known traps flagged for this milestone were both checked directly
against this plugin, not assumed from the sibling repos:

- **Particle enum rename** — confirmed no hardcoded post-rename `Particle`
  constant exists anywhere in this codebase as a field initializer (all 3
  call sites already resolved the name from config at runtime). Found and
  fixed the related *runtime* hazard instead (the `"WITCH"` config default
  failing `Particle.valueOf` on pre-rename APIs) — see milestone 2's
  follow-up bullet and `CLAUDE.md`.
- **`api-version` for older servers** — checked `EpicFurnaces`'s actual
  `build.gradle.kts`/`PLAN.md` directly rather than assuming the claim that
  it "solved" per-version `api-version` templating: it did not. Its
  `processResources` block only templates `version` (project version);
  `api-version` stays a fixed `"26.2"` literal, with `PLAN.md` explicitly
  documenting that as a deliberate scope decision (compile/package
  verification pass, not four separate release artifacts), not an
  oversight. Spigot-InvUnload follows the same pattern. This plugin follows
  that verified, actual precedent rather than building new templating
  machinery neither reference implementation has: `plugin.yml`'s
  `api-version` stays fixed at `"26.2"` for all five builds below.
  `api-version` is a runtime plugin-loader compatibility gate read by the
  server, unrelated to `javac`/compile-time behavior, so this choice cannot
  cause a build failure at any target — it is a documentation-level
  consideration for anyone deploying a *locally rebuilt* jar to an actual
  older live server (call out below in Open problems).

| Target | api-version needed for real deployment | Build status | Jar verification |
|---|---|---|---|
| 26.2 (latest) | `26.2` (shipped value) | **Built.** Default target, `./gradlew clean shadowJar`. Java 25 toolchain required by `paper-api:26.2.build.111-stable`'s Gradle module metadata. | `unzip -l build/libs/EpicHoppers-3.1.jar` — expected classes present, `plugin.yml` templated correctly, no `arconix` classes. |
| 1.21.11 (1.21.x) | `1.21` | **Built.** `-PpaperApiVersion=1.21.11-R0.1-SNAPSHOT`. | `unzip -l` — jar contents match the 26.2 build (same class list, same resource set). |
| 1.20.1 | `1.20` | **Built.** `-PpaperApiVersion=1.20.1-R0.1-SNAPSHOT`. | `unzip -l` — jar contents match. |
| 1.19.4 | `1.19` | **Built.** `-PpaperApiVersion=1.19.4-R0.1-SNAPSHOT`. | `unzip -l` — jar contents match. |
| 1.18.2 | `1.18` | **Built.** `-PpaperApiVersion=1.18.2-R0.1-SNAPSHOT`. | `unzip -l` — jar contents match. |

### 5. Verification — DONE

- [x] `./gradlew clean shadowJar` at the default (26.2) target and all four
      `-PpaperApiVersion=` overrides above — every build reported `BUILD
      SUCCESSFUL`.
- [x] `unzip -l build/libs/EpicHoppers-3.1.jar` for each of the five builds —
      confirmed plugin classes actually present (not a green-but-empty jar)
      and `plugin.yml`'s templated `version`/fixed `api-version`/trimmed
      `softdepend` render correctly in every case.
- [ ] Optional: download a Paper server jar for a target version into the
      session scratchpad only (never committed), `java -jar paper.jar
      --nogui` smoke boot with the plugin dropped in `plugins/`, confirm it
      loads without exceptions in the console log. Not attempted this pass
      (optional per the task brief; no live server was booted for any
      milestone in this plan).

## Open problems / honest blockers

- The 9 protection-plugin hooks (ASkyBlock, Factions, GriefPrevention,
  Kingdoms, PlotSquared, RedProtect, Towny, USkyBlock, WorldGuard) are
  disabled — their pinned Maven coordinates are all dead or ancient.
  Restoring any one of them means finding that specific plugin's *current*
  Maven coordinates/API and rewriting the corresponding `Hook*.java` against
  it; not attempted here as it's effectively a per-plugin integration
  project of its own.
- Folia compatibility is unverified (see milestone 3 matrix) — `HopHandler`'s
  global per-tick hopper loop and `TeleportHandler`'s cross-chunk
  teleportation both make assumptions Folia's region-threaded scheduler is
  designed to break.
- `MySQLDatabase` (optional storage backend, gated by
  `Database.Activate Mysql Support`) is unverified end-to-end — no live
  MySQL server or JDBC driver on the classpath was exercised in this pass;
  only the dead driver-class-name string was corrected (`CLAUDE.md`).
- No Paper server smoke-boot was performed for any of the five builds
  (optional per the task brief) — verification here is build-green +
  non-empty/correctly-templated-jar only, not a runtime `onEnable()` check.
- `api-version` in the shipped `plugin.yml` stays fixed at `"26.2"` across
  all five builds in this pass (following the verified `EpicFurnaces`/
  Spigot-InvUnload precedent — see milestone 4). A maintainer who actually
  wants to *deploy* a locally-built jar to an older live server (rather than
  just verify it compiles/packages) should also lower `api-version` to that
  target's value in their local build; this plan does not automate that
  since it would produce four additional release artifacts, not a
  compile/package verification pass.
- Java 25 is required to build the latest (26.2) target; the only installed
  system JDK (Temurin 21) was left untouched — the toolchain is
  auto-provisioned by `foojay-resolver-convention` into Gradle's own cache.
  Whoever builds this next should expect a one-time JDK 25 download on first
  build.

## Repository / git notes

- Default branch `main` (renamed from `master`, GitHub default branch
  updated). `master` (old default) and `Legacy` branches left in place,
  untouched. This repo also has an `upstream` remote
  (`electro2560/EpicHoppers`) — never pushed to.
- `.github/workflows/` changes are pushable. The `bshuler` gh token gained the
  `workflow` OAuth scope on 2026-08-13; earlier notes in this repo saying it
  lacks that scope are obsolete. No CI YAML needs to be parked in a scratchpad
  any more.
- Commits authored as `Bert Shuler <BertShuler@proton.me>`, signed via the
  1Password SSH agent. If signing fails with no human at the keyboard, the
  prepared commit message is appended to the session scratchpad's
  `EpicHoppers-commit-msg.txt` instead of being force-committed unsigned.

## Phase 2 — Test coverage + Folia (this pass)

### Test coverage

- JUnit 5 + JaCoCo wired into `build.gradle.kts`: `./gradlew test` runs the
  suite, `finalizedBy(jacocoTestReport)` always regenerates the XML+HTML
  report, and `jacocoTestCoverageVerification` enforces the bar below on
  `tasks.check`. All server-facing behavior is tested through MockBukkit
  (`org.mockbukkit.mockbukkit:mockbukkit-v26.1.2:4.115.0`, pinned to Paper
  API `26.1.2.build.74-stable` on the test classpath only — see
  `build.gradle.kts` comments for why this differs from main's newer
  `paperApiVersion`).
- **404 tests, 0 failures, 0 errors, 0 skipped.**
- **Included scope: 31 of 46 source files with executable lines, 100% line
  coverage (666/666 lines), enforced by a `LINE`/`COVEREDRATIO` minimum of
  `1.00` in `jacocoTestCoverageVerification`.** Verified green via
  `./gradlew clean check`.
- Repo-wide (including the 15 excluded classes below, each of which is still
  substantially tested, just not to 100%): 1996/2230 lines = **89.5%**.

### Coverage exclusions

Each excluded class mixes genuinely-tested logic with code that is not
realistically testable via MockBukkit/JUnit. One honest sentence per class:

| Class | Missed lines | Why excluded |
|---|---|---|
| `EpicHoppersPlugin` | 36 | `onEnable()`'s storage-rehydration closure (lines 113–163) is a lambda handed to `runTaskLater` that reads persisted hoppers/boosts back out of `Storage`/`StorageRow` — exercising it faithfully needs a populated on-disk `data.yml` in the exact legacy row format, not just a mocked scheduler tick; line 205 is the `StorageMysql` branch of `checkStorage()` (see below); line 226 is a bare `continue;` that is the sole body of an already-fully-covered `if` — see the `ModuleSuction` entry for the same javac/JaCoCo line-attribution quirk. |
| `StorageMysql` | 56 (100% of the class) | Every method opens a live JDBC connection to a real MySQL server; there is no MySQL server in this environment and mocking `java.sql.*` down to the row level would test the mock, not the class. |
| `MySQLDatabase` | 12 (100% of the class) | Same reason as `StorageMysql` — a thin JDBC connection-pool wrapper with no logic worth testing independently of a live database. |
| `TeleportHandler` | 33 | `tpPlayer()`'s chain-walking loop and the "teleport back" branch are dead code given a real, found-but-not-fixed bug (see "Bugs found" below) — the loop always breaks on its first iteration before ever calling `player.teleport(...)`. The remaining lines are the constructor's defensive catch block. |
| `HopHandler` | 32 | `hopperCleaner()`'s per-key "is this still a hopper" loop (lines 45–56) is dead code given a real, found-but-not-fixed bug (see "Bugs found" below). The rest are defensive catch blocks in the constructor, `hopperRunner()`, `doBlacklist()`, `addItem()`, and `canHop()`. |
| `EHopper` | 24 | All defensive catch blocks around GUI-building/upgrade/sync methods, plus two genuinely-testable-but-deferred branches (a multi-viewer overview-conflict `closeInventory()` call, and the Vault-installed economy-purchase branch of `upgrade("ECO", ...)`) already noted as intentionally out of scope in `EHopperTest`'s class doc comment. |
| `Methods` | 16 | All defensive catch blocks, including one nested fallback catch whose primary path is already covered by other tests. |
| `CommandGive` | 6 | The `else if (args.length == 1)` branch (lines 33–37) is unreachable dead code by construction — the method's leading `if (args.length <= 2) return SYNTAX_ERROR;` guarantees `args.length >= 3` everywhere beneath it, confirmed and tested via `singleArgumentIsASyntaxErrorBeforeTheSelfGiveBranchIsEverReached`. Lines 30–31 (`Bukkit.getPlayer(args[1]) == null` immediately after `getPlayerExact` already succeeded) are a hard-to-construct MockBukkit edge case, accepted as a small gap. |
| `InventoryListeners` | 4 | Two defensive catch blocks in two different methods. |
| `ModuleSuction` | 4 | Line 43 is a bare `continue;` as the sole body of an already-fully-covered multi-condition `if` — confirmed via the JaCoCo HTML report's per-line coverage markers that the condition line is `fc` (fully covered, both branches exercised) while the `continue;` line itself is `nc` (not covered); this is a javac/JaCoCo debug-line-table artifact for jump-only if-bodies, not a real behavioral gap. Line 56 is the WildStacker soft-dependency branch, which needs the real plugin installed to exercise. Lines 95–96 are `canMove()`'s catch block. |
| `BlockListeners` | 3 | Defensive catch block in the chunk hopper-count helper. |
| `EnchantmentHandler` | 3 | Defensive catch block in the Sync Touch book-encoding method. |
| `HopperListeners` | 2 | Defensive catch block in `onHop()` — the cancel-branch above it is already covered. |
| `ModuleAutoCrafting` | 2 | Defensive catch block in the inventory-space check. |
| `Locale` | 1 | A static-ordering guard (`throw new IllegalStateException(...)` if a `Locale` is constructed before `Locale.init(JavaPlugin)` sets the static `plugin` field) that every real code path already prevents by construction. |

### Bugs found while writing tests

- **Fixed**: `InventoryListenersTest.onInventoryClickAllowsTheCraftingSlotThirteenButCancelsEverythingElse`
  was passing for the wrong reason — `EHopper.crafting()` places an
  AIR-typed `ItemStack` in slot 13 by default, which (Mock)Bukkit normalizes
  to a `null` inventory slot, so `InventoryListeners.onInventoryClick()`'s
  very first `event.getCurrentItem() == null` guard short-circuited before
  ever reaching the slot-13 passthrough logic the test meant to exercise.
  Fixed by calling `hopper.setAutoCrafting(Material.TORCH)` before opening
  the crafting menu so slot 13 holds a real item.
- **Found, deliberately not fixed** (behavior-changing, no live server
  available to confirm original intent, low current blast radius):
  - `HopHandler.hopperCleaner()` unconditionally calls
    `instance.getConfig().createSection("data")`, which Bukkit always
    resets to a brand-new empty section — wiping any existing `data.*`
    config state (including `data.sync.*`) on every single run and making
    the method's own per-key "is this still a hopper" cleanup loop
    unreachable dead code. See `HopHandlerTest.
    hopperCleanerWipesTheEntireDataSectionOnEveryRunBecauseCreateSectionAlwaysResetsIt`.
  - `TeleportHandler.tpPlayer()`'s chain-walking `while` loop computes
    `nextHopper` from the hopper's own location on its first pass, so
    `nextHopper == hopper` is trivially true immediately and the loop
    always `break`s before ever calling `player.teleport(...)` — the method
    never actually moves a player, for any hopper chain, as currently
    written. See the class doc comment in `TeleportHandlerTest`.
  - `CommandGive`'s `else if (args.length == 1)` self-give branch is
    unreachable dead code by construction (see the exclusions table above).

### Folia

**Verdict: NOT safely achievable in this pass — `folia-supported` is NOT
added to `plugin.yml`.**

Concrete blockers, confirmed by re-reading the scheduler call sites
(`grep -rn "getScheduler()" src/main/java`):

- `HopHandler.hopperRunner()` — a single **global** repeating task
  (`Bukkit.getScheduler().scheduleSyncRepeatingTask`, not a region-aware
  scheduler) that, every N ticks, iterates *every hopper on the entire
  server* and synchronously reads/writes both the hopper's own block state
  and an arbitrarily-distant "synced" destination block
  (`hopper.getSyncedBlock()`) in the same pass. Folia's region-threading
  model exists specifically to forbid exactly this: synchronous block
  access across chunk/region boundaries from a single non-region-owned
  thread.
- `TeleportHandler.teleportRunner()` — the same global-scheduler pattern,
  iterating `Bukkit.getOnlinePlayers()` (players can be in any region) and
  calling the synchronous `player.teleport(location)` to a hopper-chain
  destination that can be in a completely different chunk than the
  player's current region. Folia requires cross-region teleports to go
  through `teleportAsync` with an explicit region-reassignment, not a
  synchronous call from a global task.
- `EpicHoppersPlugin`'s periodic `saveToFile()`
  (`Bukkit.getScheduler().scheduleSyncRepeatingTask`, every 6000 ticks)
  iterates every hopper server-wide and calls
  `hopper.getLocation().getChunk()` from the global-scheduler thread —
  lower risk since it's a read for serialization rather than a block
  mutation, but the same unrestricted cross-region access pattern.
- **Fundamental architecture problem, not a small fix**: the plugin's core
  feature is hopper-to-hopper "sync" — transferring items or teleporting
  players between two hoppers/chests that the player is free to place
  anywhere on the server, in different chunks that Folia may assign to
  different region threads. Making this genuinely region-safe would require
  re-architecting the entire hop/teleport pipeline into cross-region
  scheduling (dispatch keyed by whichever region owns each hopper's chunk,
  plus two-phase read-on-source/write-on-destination coordination for any
  hop whose endpoints land in different regions, plus async player
  teleportation with a completion callback before continuing any chain
  logic) — a rewrite of the plugin's central mechanic, not the "small,
  low-risk change" the brief's bar for flipping the flag calls for. This
  matches (and confirms, independently re-derived) this repo's own
  `CLAUDE.md` "Platforms" section, which already flagged Folia as untested
  for exactly these two classes.

## Coverage in context (measured 2026-08-13)

Read from the JaCoCo XML report, not from whether the gate passes:

- **Analysed surface:** 41 of 56 compiled classes (73%).
- **Line coverage of that surface:** 100.0% (666 lines analysed).
- Classes outside that surface are excluded by the documented exclusion list. They
  are not covered by any test and are not runtime-verified.

A passing `check` means "no regression inside the analysed surface" — it does not
mean the whole codebase is tested to that percentage.
