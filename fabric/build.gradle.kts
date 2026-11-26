@file:Suppress("UnstableApiUsage")

plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
    id("com.gradleup.shadow")
    `maven-publish`
}

val loader = prop("loom.platform")!!
val minecraft: String = stonecutter.current.version
val common: Project = requireNotNull(stonecutter.node.sibling("")?.project) {
    "No common project for $project"
}

group = mod.group
version = "${mod.version}+$minecraft"
base {
    archivesName.set("${mod.id}-$loader")
}

architectury {
    platformSetupLoomIde()
    fabric()
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
    get("developmentFabric").extendsFrom(commonBundle)
}

repositories {
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
    modImplementation("net.fabricmc:fabric-loader:${mod.dep("fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${common.mod.dep("fabric_api")}")
    modImplementation("dev.architectury:architectury-fabric:${common.mod.dep("architectury_api")}")

    commonBundle(project(common.path, "namedElements")) { isTransitive = false }
    shadowBundle(project(common.path, "transformProductionFabric")) { isTransitive = false }
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

tasks.shadowJar {
    configurations = listOf(shadowBundle)
    archiveClassifier = "dev-shadow"
}
tasks.remapJar {
    input = tasks.shadowJar.get().archiveFile
    archiveClassifier = null
    dependsOn(tasks.shadowJar)
}
tasks.jar {
    archiveClassifier = "dev"
}

tasks.processResources {
    properties(listOf("fabric.mod.json"),
        "id" to mod.id,
        "name" to mod.name,
        "version" to mod.version,
        "minecraft" to common.mod.prop("mc_dep_fabric")
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

// For addon mods (Fixtures & Footnotes & Structures) to develop against an unreleased build:
// `publishToMavenLocal` puts this node in ~/.m2 as com.beepsterr.fixfoot:fixfoot-fabric:<ver>+<mc>.
// Nothing here reaches a remote — there is no repositories {} block, so mavenLocal is the only sink.
//
// Artifacts are listed explicitly rather than via components["java"] on purpose. The component
// would carry a POM full of transitive deps (Fabric API, Architectury, MixinExtras), and consumers
// pull this in through Loom's `modLocalRuntime`, which wants the jar and nothing else. Publishing
// bare artifacts keeps a stray transitive from landing on an addon's classpath.
publishing {
    publications {
        create<MavenPublication>("mod") {
            artifactId = base.archivesName.get()
            artifact(tasks.remapJar)
            artifact(tasks.remapSourcesJar) { classifier = "sources" }
        }
    }
}
