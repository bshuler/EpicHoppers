import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    java
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

tasks.processResources {
    val props = mapOf("version" to project.version)
    filesMatching("plugin.yml") {
        expand(props)
    }
}
