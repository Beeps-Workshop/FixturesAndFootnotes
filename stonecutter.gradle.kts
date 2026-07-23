plugins {
    id("dev.kikugie.stonecutter")
    id("dev.architectury.loom") version "1.17-SNAPSHOT" apply false
    id("architectury-plugin") version "3.5-SNAPSHOT" apply false
    id("com.gradleup.shadow") version "9.6.1" apply false
}

stonecutter active "1.21.1" /* [SC] DO NOT EDIT */

// Aggregate task: builds every (version x loader) and collects jars into
// build/libs/<mod.version>/<loader>. In 0.9.x the old `registerChiseled`/`chiseled`
// DSL is gone; you depend on the per-node task map returned by stonecutter.tasks.named.
tasks.register("chiseledBuild") {
    group = "project"
    description = "Builds all versions and loaders, collecting jars under build/libs."
    // Only the loader branches produce jars (the empty-id branch is the common project).
    dependsOn(stonecutter.tasks.named("buildAndCollect") { branch.id.isNotEmpty() })
}
