plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
    id("io.ktor.plugin")
    id("com.github.ben-manes.versions")
}

group = "pl.antoniuk"
version = "1.0-SNAPSHOT"

// This is the critical part - setting the correct main class
application {
    mainClass.set("pl.antoniuk.MainKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(Ktor.server.core)
    implementation(Ktor.server.netty)
    implementation(Ktor.plugins.serialization.kotlinx.json)
    implementation(Ktor.server.contentNegotiation)
    implementation(Ktor.client.core)
    implementation(Ktor.client.cio)
    implementation(Ktor.server.cors)
    implementation(Ktor.server.callLogging)
    implementation(Ktor.server.cachingHeaders)
    implementation("ch.qos.logback:logback-classic:_")
    implementation(Ktor.client.contentNegotiation)
    implementation(JetBrains.exposed.core)
    implementation(JetBrains.exposed.dao)
    implementation(JetBrains.exposed.jdbc)
    implementation("org.jetbrains.exposed:exposed-java-time:_")
    implementation("org.postgresql:postgresql:_")
    implementation("com.zaxxer:HikariCP:_")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

tasks {
    val fatJar = register<Jar>("fatJar") {
        dependsOn.addAll(listOf("compileJava", "compileKotlin", "processResources"))
        archiveClassifier.set("fat")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest {
            attributes(mapOf(
                "Main-Class" to "pl.antoniuk.MainKt"  // This needs to match your actual main class
            ))
        }
        val sourcesMain = sourceSets.main.get()
        val contents = configurations.runtimeClasspath.get()
            .map { if (it.isDirectory) it else zipTree(it) } +
                sourcesMain.output
        from(contents)
    }
    build {
        dependsOn(fatJar)
    }
}