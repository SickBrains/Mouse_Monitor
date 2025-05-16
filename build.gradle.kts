import org.gradle.jvm.tasks.Jar
import java.io.File

plugins {
    kotlin("jvm") version "2.1.10"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val javafxVersion = "21"
val appName = "MouseMonitor"

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.github.kwhat:jnativehook:2.2.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")
    implementation("net.java.dev.jna:jna:5.14.0")
    implementation("net.java.dev.jna:jna-platform:5.14.0")
    implementation(files("libs/image4j-0.7.2.jar"))
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // JavaFX (Windows platform-specific)
    implementation("org.openjfx:javafx-base:$javafxVersion:win")
    implementation("org.openjfx:javafx-controls:$javafxVersion:win")
    implementation("org.openjfx:javafx-fxml:$javafxVersion:win")
    implementation("org.openjfx:javafx-graphics:$javafxVersion:win")
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("tracker.MainKt")
}

// === JavaFX module path setup for IntelliJ run ===
val javafxModules = listOf("javafx.controls", "javafx.fxml")

val javafxLibs = configurations.runtimeClasspath.get()
    .filter { it.name.contains("javafx") }
    .joinToString(File.pathSeparator) { it.absolutePath }

tasks.withType<JavaExec>().configureEach {
    jvmArgs = listOf(
        "--module-path", javafxLibs,
        "--add-modules", javafxModules.joinToString(",")
    )
}

sourceSets {
    main {
        resources.srcDir("src/main/resources")
    }
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// === Fat Jar for packaging ===
tasks.register<Jar>("fatJar") {
    archiveBaseName.set(appName)
    archiveClassifier.set("all")
    archiveVersion.set(version.toString())
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Main-Class"] = "tracker.MainKt"
    }

    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith(".jar") }.map { zipTree(it) }
    })
}

// === jpackage task ===
tasks.register<Exec>("jpackage") {
    dependsOn("fatJar")

    val outputDir = "$buildDir/jpackage"
    val jarName = "$appName-$version-all.jar"
    val iconPath = "src/main/resources/logo.ico"

    commandLine = listOf(
        "jpackage",
        "--type", "exe",
        "--name", appName,
        "--input", "build/libs",
        "--main-jar", jarName,
        "--main-class", "tracker.MainKt",
        "--dest", outputDir,
        "--icon", iconPath,
        "--app-version", "1.0.0",
        "--win-shortcut",
        "--win-menu",
        "--win-dir-chooser"
    )
}
