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

### 2. Modern build (latest Paper API)

- [ ] Replace the broken two-module Maven layout (`pom.xml` with no
      `<modules>` despite `EpicHoppers-API`/`EpicHoppers-Plugin` being
      separate trees) with one consolidated Gradle 9.x project.
- [ ] `paper-api` at the latest resolvable version (queried live from
      `fill.papermc.io`/`repo.papermc.io` maven-metadata — **26.2** /
      `26.2.build.111-stable` at time of writing, confirmed identical to the
      version `EpicFurnaces` used a short time earlier in the same session;
      do not hardcode this without re-checking, MC is calendar-versioned).
- [ ] Java 25 toolchain, `com.gradleup.shadow`, `foojay-resolver-convention`.
- [ ] Remove Arconix/`org.json.simple`/NMS version-sniffing (see `CLAUDE.md`
      table); fix `Inventory.getTitle()` (2 sites in `SettingsManager`) and
      `Player#getItemInHand()` (`Methods.isSync`, `EntityListeners.onDrop`).
- [ ] Repoint `WildStackerAPI` from the dead `xyz.wildseries` package (which
      also had *no* Maven coordinate at all in the original `pom.xml` — a
      pre-existing build gap, not just a dead dependency) to the live
      `com.bgsoftware:WildStackerAPI:2026.2` coordinate.
- [ ] Fix legacy `com.mysql.jdbc.Driver` string in `MySQLDatabase.java` to
      `com.mysql.cj.jdbc.Driver`.
- [ ] Correct `Setting.o7`'s `Main.Upgrade Particle Type` default from
      `"WITCH_MAGIC"` (invalid) to `"WITCH"` (valid modern `Particle`
      constant).
- [ ] Relocate the 9 protection-plugin hook files to `legacy-hooks/`
      (excluded from compilation), strip their registration from
      `EpicHoppersPlugin.onEnable()`.
- [ ] Add VaultAPI exclusion for the transitive `org.bukkit:bukkit`
      capability conflict if it recurs (confirmed present in `EHopper.java`'s
      economy-upgrade path and `CommandBoost`'s Vault usage — same shape of
      problem `EpicFurnaces` hit).
- [ ] Bump `plugin.yml`'s `api-version` from `1.13` to match the real build
      target, and drop the hard `depend: [Arconix]`.
- [ ] Green build (`./gradlew build`), verify jar contents (`unzip -l`).
      Commit + push.

### 3. Cross-platform assessment

To be filled in after independently reasoning through this plugin's own
hopper/item-transport/teleport/GUI subsystems (not copied from
`EpicFurnaces`'s conclusion, even though it is expected to land in the same
place for the same underlying reason — Bukkit plugins vs. mod loaders are
different programming models).

### 4. Backward version walk

To be filled in: `-PpaperApiVersion=<coord>` override builds against
1.21.x/1.20.1/1.19.4/1.18.2, each with jar-contents verification.

### 5. Verification

To be filled in: final `unzip -l` pass across all built jars; optional Paper
smoke-boot (scratchpad only, never committed).

## Open problems / honest blockers

(To be filled in as milestones progress — expected candidates: the 9
disabled protection-plugin hooks, Folia compatibility unverified, MySQL
storage backend unverified end-to-end since no live server was booted.)

## Repository / git notes

- Default branch `main` (renamed from `master`, GitHub default branch
  updated). `master` (old default) and `Legacy` branches left in place,
  untouched. This repo also has an `upstream` remote
  (`electro2560/EpicHoppers`) — never pushed to.
- Do not commit anything under `.github/workflows/` — active `gh` token for
  the `bshuler` account lacks the `workflow` scope. Any proposed CI YAML
  lives only in the session scratchpad, never in this repo, until pushed by
  a session with the right token scope.
- Commits authored as `Bert Shuler <BertShuler@proton.me>`, signed via the
  1Password SSH agent. If signing fails with no human at the keyboard, the
  prepared commit message is appended to the session scratchpad's
  `EpicHoppers-commit-msg.txt` instead of being force-committed unsigned.
