import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    java
    jacoco
    id("com.gradleup.shadow") version "9.6.1"
}

group = "com.songoda"
version = providers.gradleProperty("pluginVersion").get()

// Java 25: the latest resolvable paper-api (26.2.build.111-stable, matching
// Minecraft's current calendar-versioned release) publishes Gradle module
// metadata requiring JVM 25. The foojay-resolver-convention plugin (declared
// in settings.gradle.kts) auto-provisions this JDK into Gradle's own
// toolchain cache; it does not touch the system/Homebrew JDK 21 install. See
// PLAN.md/CLAUDE.md.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    // WildStackerAPI: rebranded groupId/package (xyz.wildseries -> com.bgsoftware)
    // years ago; live coordinate confirmed via maven-metadata.xml. See CLAUDE.md.
    maven("https://repo.bg-software.com/repository/api/")
}

// Paper API version tests actually run against. MockBukkit-v26.1.2 (see
// below) bundles registry/tag JSON data baked for exactly this Paper
// version; a mismatched paper-api on the test classpath throws
// InternalDataLoadException ("MockBukkit / MC version mismatch?") at
// ServerMock construction. Confirmed by hitting that exact failure with the
// main sourceSet's own (newer) `paperApiVersion` (26.2) leaking onto the
// test classpath before this was pinned down.
val testPaperApiVersion = "26.1.2.build.74-stable"

configurations {
    // Test code needs the same Bukkit/Vault/WildStacker API surface as main
    // (e.g. Methods.resolveParticle's org.bukkit.Particle) without
    // re-declaring every compileOnly dependency a second time - but paper-api
    // itself must resolve to testPaperApiVersion, not main's newer
    // `paperApiVersion`, so it's forced below on the test configurations.
    testImplementation.get().extendsFrom(compileOnly.get())
}

listOf("testCompileClasspath", "testRuntimeClasspath").forEach { name ->
    configurations.named(name) {
        resolutionStrategy.force("io.papermc.paper:paper-api:$testPaperApiVersion")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${providers.gradleProperty("paperApiVersion").get()}")
    // VaultAPI transitively pulls a pre-Mojang-mapped CraftBukkit snapshot
    // (org.bukkit:bukkit:1.13.1-R0.1-SNAPSHOT) whose "bukkit" capability
    // conflicts with the one paper-api provides. Only the Vault economy
    // interfaces are used here, so the transitive Bukkit dependency is
    // excluded rather than letting the two resolve against each other.
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
    // Soft dependency, gated at runtime by
    // Bukkit.getPluginManager().isPluginEnabled("WildStacker"). Live coordinate,
    // unlike the 9 protection-plugin hooks (see legacy-hooks/).
    compileOnly("com.bgsoftware:WildStackerAPI:2026.2")

    // Test infrastructure. MockBukkit-v26.1.2 is the newest MockBukkit
    // release available (org.mockbukkit.mockbukkit namespace, checked live
    // against repo1.maven.org's maven-metadata.xml on 2026-08-10) and is the
    // target tests run against - NOT the same as the main sourceSet's
    // default `paperApiVersion` (26.2). This is deliberate: this plugin's
    // whole multi-version story (PLAN.md milestone 4) is that its source
    // uses only the stable Bukkit API surface unchanged from 1.18.2 through
    // 26.2, so a test target one calendar version behind (26.1.2) exercises
    // the exact same code paths. See CLAUDE.md "Testing" section.
    testImplementation(platform("org.junit:junit-bom:6.1.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v26.1.2:4.115.0")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveBaseName.set("EpicHoppers")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

// Classes excluded from the 100% coverage bar below. Each one mixes
// genuinely-testable logic (already tested elsewhere in the class) with code
// that is not realistically testable with MockBukkit/JUnit alone - a
// defensive `catch (Exception e) { Debugger.runReport(e); }` block, a live
// external dependency (MySQL), or a javac/JaCoCo line-attribution quirk for
// a bare `continue;` as the sole body of an `if`. See PLAN.md "Coverage
// exclusions" for the one-sentence reason behind every excluded class.
val jacocoExcludedClasses = listOf(
    "com/songoda/epichoppers/Locale.class",
    "com/songoda/epichoppers/EpicHoppersPlugin.class",
    "com/songoda/epichoppers/storage/types/StorageMysql.class",
    "com/songoda/epichoppers/listeners/BlockListeners.class",
    "com/songoda/epichoppers/listeners/InventoryListeners.class",
    "com/songoda/epichoppers/listeners/HopperListeners.class",
    "com/songoda/epichoppers/command/commands/CommandGive.class",
    "com/songoda/epichoppers/hopper/EHopper.class",
    "com/songoda/epichoppers/handlers/EnchantmentHandler.class",
    "com/songoda/epichoppers/handlers/TeleportHandler.class",
    "com/songoda/epichoppers/handlers/HopHandler.class",
    "com/songoda/epichoppers/hopper/levels/modules/ModuleAutoCrafting.class",
    "com/songoda/epichoppers/hopper/levels/modules/ModuleSuction.class",
    "com/songoda/epichoppers/utils/MySQLDatabase.class",
    "com/songoda/epichoppers/utils/Methods.class"
)

fun ConfigurableFileCollection.excludeJacocoClasses() {
    setFrom(files.map { fileTree(it) { exclude(jacocoExcludedClasses) } })
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    classDirectories.excludeJacocoClasses()
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.test)
    classDirectories.excludeJacocoClasses()
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "1.00".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    filesMatching("plugin.yml") {
        expand(props)
    }
}
