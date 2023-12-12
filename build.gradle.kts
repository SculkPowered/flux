plugins {
    id("java")
    id("com.github.johnrengelman.shadow").version("8.1.1")
}

group = "io.github.sculkpowered"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven("https://libraries.minecraft.net")
}

dependencies {
    compileOnly("io.github.sculkpowered.server:api:1.0.0-SNAPSHOT")
    annotationProcessor("io.github.sculkpowered.server:api:1.0.0-SNAPSHOT")
    implementation("com.influxdb:influxdb-client-java:6.11.0")
}