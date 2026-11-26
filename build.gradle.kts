plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
}

val minecraft = stonecutter.current.version

version = "${mod.version}+$minecraft"
base {
    archivesName.set("${mod.id}-common")
}

architectury.common(stonecutter.tree.branches.mapNotNull {
    if (stonecutter.current.project !in it) null
    else it.project.prop("loom.platform")
})

repositories {
    maven("https://maven.architectury.dev")
    maven("https://maven.parchmentmc.org")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    // Mojang official mappings everywhere; Parchment param-names layered on where published
    // (1.21.x). 26.x has no Parchment yet, so the guard drops it and we run pure Mojmaps.
    mappings(loom.layered {
        officialMojangMappings()
        if (stonecutter.eval(minecraft, "<26")) {
            parchment("org.parchmentmc.data:parchment-$minecraft:${mod.dep("parchment")}@zip")
        }
    })
    modImplementation("net.fabricmc:fabric-loader:${mod.dep("fabric_loader")}")
    modImplementation("dev.architectury:architectury:${mod.dep("architectury_api")}")
    "io.github.llamalad7:mixinextras-common:${mod.dep("mixin_extras")}".let {
        annotationProcessor(it)
        implementation(it)
    }
}

loom {
    decompilers {
        get("vineflower").apply { // names lambdas — helpful when writing mixins later
            options.put("mark-corresponding-synthetics", "1")
        }
    }
}

java {
    withSourcesJar()
    // 26.x compiles on Java 25; 1.21.x on Java 21. Gradle auto-detects both installed JDKs.
    val java = if (stonecutter.eval(minecraft, ">=26")) JavaVersion.VERSION_25 else JavaVersion.VERSION_21
    targetCompatibility = java
    sourceCompatibility = java
}

// 1.21.2 dropped the object form of a recipe ingredient: {"item": "minecraft:oak_log"} no longer
// parses and has to be the bare id "minecraft:oak_log". 1.21.1 only accepts the object form, so the
// sources stay in it and newer nodes rewrite their copy. Every ingredient here is a single item —
// no tags, no lists of alternatives — which is all this pattern handles; a recipe result is keyed
// "id", not "item", so it is never touched.
if (stonecutter.eval(minecraft, ">=1.21.2")) {
    tasks.processResources {
        val itemIngredient = Regex("""\{\s*"item"\s*:\s*("[^"]+")\s*}""")
        doLast {
            destinationDir.resolve("data").walk()
                .filter { it.isFile && it.extension == "json" && it.parentFile.name == "recipe" }
                .forEach { it.writeText(itemIngredient.replace(it.readText(), "$1")) }
        }
    }
}

tasks.build {
    group = "versioned"
    description = "Build the active version — run 'chiseledBuild' for all versions."
}
