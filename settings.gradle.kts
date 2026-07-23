pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.kikugie.dev/releases")
        maven("https://maven.kikugie.dev/snapshots")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.7"
}

stonecutter {
    centralScript = "build.gradle.kts"
    kotlinController = true

    create(rootProject) {
        // Root `src/` is the shared 'common' project; each branch is a loader.
        // Adding a Minecraft version later = one entry here (+ a versions/<mc>/gradle.properties).
        versions("1.21.1", "1.21.4")
        branch("fabric")
        branch("neoforge")
    }
}

rootProject.name = "FixturesAndFootnotes"
