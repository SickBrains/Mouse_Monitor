plugins {
    kotlin("jvm") version "2.1.10"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    testImplementation(kotlin("test"))
    implementation(kotlin("test"))
}

application {
    mainClass.set("ml.MainKt")
}

tasks.test {
    useJUnitPlatform()
}
