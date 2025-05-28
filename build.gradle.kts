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

val javafxVersion = "21.0.2"
val appName = "MouseMonitor"

configurations.all {
    exclude(group = "org.slf4j", module = "slf4j-log4j12")
}


dependencies {
    testImplementation(kotlin("test"))
    implementation("com.github.kwhat:jnativehook:2.2.2")
    implementation("net.java.dev.jna:jna:5.14.0")
    implementation("net.java.dev.jna:jna-platform:5.14.0")
    implementation(files("libs/image4j-0.7.2.jar"))
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("org.apache.parquet:parquet-avro:1.15.2")
    implementation("org.apache.avro:avro:1.11.4")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")

    // Add this line
    implementation("org.apache.hadoop:hadoop-common:3.3.6")

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
        kotlin.srcDir("src/main/kotlin")
        resources.srcDir("src/main/resources")
    }
}


sourceSets["main"].kotlin.srcDirs("src/main/kotlin")

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
