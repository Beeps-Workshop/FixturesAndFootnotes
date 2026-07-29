@file:Suppress("UnstableApiUsage")

plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
    id("com.gradleup.shadow")
}

val loader = prop("loom.platform")!!
val minecraft: String = stonecutter.current.version
val common: Project = requireNotNull(stonecutter.node.sibling("")?.project) {
    "No common project for $project"
}

version = "${mod.version}+$minecraft"
base {
    archivesName.set("${mod.id}-$loader")
}

architectury {
    platformSetupLoomIde()
    neoForge()
}

val commonBundle: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}
val shadowBundle: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}
configurations {
    compileClasspath.get().extendsFrom(commonBundle)
    runtimeClasspath.get().extendsFrom(commonBundle)
    get("developmentNeoForge").extendsFrom(commonBundle)
}

repositories {
    maven("https://maven.neoforged.net/releases/")
    maven("https://maven.architectury.dev")
    maven("https://maven.parchmentmc.org")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    mappings(loom.layered {
        officialMojangMappings()
        if (stonecutter.eval(minecraft, "<26")) {
            parchment("org.parchmentmc.data:parchment-$minecraft:${common.mod.dep("parchment")}@zip")
        }
    })
    "neoForge"("net.neoforged:neoforge:${common.mod.dep("neoforge")}")
    modImplementation("dev.architectury:architectury-neoforge:${common.mod.dep("architectury_api")}")
    "io.github.llamalad7:mixinextras-neoforge:${mod.dep("mixin_extras")}".let {
        implementation(it)
        include(it)
    }

    commonBundle(project(common.path, "namedElements")) { isTransitive = false }
    shadowBundle(project(common.path, "transformProductionNeoForge")) { isTransitive = false }
}

loom {
    decompilers {
        get("vineflower").apply { options.put("mark-corresponding-synthetics", "1") }
    }
    runConfigs.all {
        // Off deliberately. Loom's generated IDE configs are Application-type and bound to module
        // names like FixturesAndFootnotes.fabric.1.21.1.main, which is not how this Stonecutter
        // tree gets imported — so every one of them showed up broken, both versions. The working
        // configs are the Gradle-type ones in .run/, which just invoke the task and always resolve.
        isIdeConfigGenerated = false
        runDir = "../../run"
        vmArgs("-Dmixin.debug.export=true")
    }
}

java {
    withSourcesJar()
    val java = if (stonecutter.eval(minecraft, ">=26")) JavaVersion.VERSION_25 else JavaVersion.VERSION_21
    targetCompatibility = java
    sourceCompatibility = java
}

tasks.jar {
    archiveClassifier = "dev"
}
tasks.shadowJar {
    configurations = listOf(shadowBundle)
    archiveClassifier = "dev-shadow"
    exclude("fabric.mod.json", "architectury.common.json")
}
tasks.remapJar {
    input = tasks.shadowJar.get().archiveFile
    archiveClassifier = null
    dependsOn(tasks.shadowJar)
}

tasks.processResources {
    properties(listOf("META-INF/neoforge.mods.toml", "pack.mcmeta"),
        "id" to mod.id,
        "name" to mod.name,
        "version" to mod.version,
        "minecraft" to common.mod.prop("mc_dep_neoforge")
    )
}

tasks.build {
    group = "versioned"
    description = "Must run through 'chiseledBuild'"
}

tasks.register<Copy>("buildAndCollect") {
    group = "versioned"
    description = "Collect the remapped jar into build/libs/<version>/<loader>."
    from(tasks.remapJar.get().archiveFile, tasks.remapSourcesJar.get().archiveFile)
    into(rootProject.layout.buildDirectory.file("libs/${mod.version}/$loader"))
    dependsOn("build")
}
