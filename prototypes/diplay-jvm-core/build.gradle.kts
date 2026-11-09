// Compiles the Android-free core of a DiPlay checkout on a desktop JVM.
// Our files: this script, shim/ (android.* stubs) and seams/ (desktop rewrites).
// Usage: DIPLAY_DIR=/path/to/DiPlay gradle compileKotlin
plugins {
    kotlin("jvm") version "2.2.10"
}

repositories { mavenCentral() }

val diplayDir: String = System.getenv("DIPLAY_DIR")
    ?: error("set DIPLAY_DIR to a DiPlay checkout")
val javaRoot = file("$diplayDir/shared/src/main/java")
// One path per line, relative to shared/src/main/java (from tools/port_scope.py --shim).
val portable: List<String> = file(System.getenv("DIPLAY_FILES") ?: "files.txt")
    .readLines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }

// seams/: our desktop rewrites of DiPlay files that touch Android, same package + API.
val seamFiles: List<String> = fileTree("seams").matching { include("**/*.kt") }.files
    .map { it.relativeTo(file("seams")).invariantSeparatorsPath }

// Stage the listed DiPlay files (minus any we replaced in seams/) into build/, which is
// gitignored, so their GPL sources never enter this repo.
val staged = layout.buildDirectory.dir("diplay-src")
val stageDiplay by tasks.registering(Sync::class) {
    from(javaRoot) { include(portable.filterNot { it in seamFiles }) }
    into(staged)
}
sourceSets {
    main {
        kotlin.setSrcDirs(listOf(file("shim"), file("seams"), staged))
        java.setSrcDirs(listOf(staged))
    }
}
tasks.named("compileKotlin") { dependsOn(stageDiplay) }
tasks.named("compileJava") { dependsOn(stageDiplay) }
kotlin { jvmToolchain(21) }

dependencies {
    implementation("org.bouncycastle:bcprov-jdk18on:1.79")
    implementation("org.jmdns:jmdns:3.6.3")
    implementation("net.sf.kxml:kxml2:2.3.0") // android.util.Xml stand-in
}
