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

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.github.kwhat:jnativehook:2.2.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")
    implementation("net.java.dev.jna:jna:5.14.0")
    implementation("net.java.dev.jna:jna-platform:5.14.0")
    implementation(files("libs/image4j-0.7.2.jar"))
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // JavaFX (Windows only)
    implementation("org.openjfx:javafx-base:$javafxVersion:win")
    implementation("org.openjfx:javafx-controls:$javafxVersion:win")
    implementation("org.openjfx:javafx-fxml:$javafxVersion:win")
    implementation("org.openjfx:javafx-graphics:$javafxVersion:win")
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("tracker.MainApp")
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs = listOf(
        "--module-path", classpath.asPath,
        "--add-modules", "javafx.controls,javafx.fxml"
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

val jpackageOutputDir = "$buildDir/jpackage"

