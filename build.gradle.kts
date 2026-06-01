import java.io.ByteArrayOutputStream

plugins {
    id("java")
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.spring") version "2.2.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0"
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.7" // Oppdatert til å matche Boot 3.4
}

group = "no.iktdev.streamit"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://reposilite.iktdev.no/releases")
    }
    maven {
        url = uri("https://reposilite.iktdev.no/snapshots")
    }
}

val exposedVersion = "0.61.0"

dependencies {
    implementation(kotlin("script-runtime"))

    // Spring Boot 3.4.1 Startere (håndterer alle kjerneversjoner, inkludert SnakeYAML)
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-configuration-processor")

    // Oppdatert til v2.x som er påkrevd for Spring Boot 3.x / Jakarta EE
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib") // Kotlin 2.2 trenger ikke jdk8-suffikset lenger

    implementation("com.auth0:java-jwt:4.4.0") // Oppdatert til moderne versjon
    implementation("com.google.code.gson:gson:2.11.0") // Matcher nyere JVM-er bedre
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")
    implementation("no.iktdev:exfl:0.0.16-SNAPSHOT")

    // Database stuff
    implementation("org.jetbrains.exposed:exposed-core:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-dao:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-jdbc:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-java-time:${exposedVersion}")

    // Riktig driver-artefakt for Spring Boot 3.x (BOM-styrt versjon)
    implementation("com.mysql:mysql-connector-j")
    implementation("org.postgresql:postgresql:42.7.7")
    implementation("org.xerial:sqlite-jdbc:3.46.0.0") // Oppdatert for Java 21+ kompatibilitet
    implementation("com.h2database:h2")

    // HikariCP 7.0.2 ble fjernet her – spring-boot-starter-data-jpa leverer nøyaktig den
    // versjonen som passer til Boot 3.4.1 (ingen sjanse for klasselaster-kræsj)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    implementation("com.google.zxing:core:3.5.3")
    implementation("com.sksamuel.aedile:aedile-core:3.0.0")

    // --- TESTING ---
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    // Manuelle, utdaterte versjoner av assertj og mockito er fjernet.
    // spring-boot-starter-test tar seg av disse med korrekte, samkjøre versjoner automatisk.
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-actuator")

    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
}

tasks.test {
    useJUnitPlatform()
}

tasks.bootJar {
    archiveFileName.set("app.jar")
    launchScript()
}

tasks.jar {
    archiveFileName.set("app.jar")
    archiveBaseName.set("app")
}

fun findLatestTag(): String {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine = listOf("git", "describe", "--tags", "--abbrev=0")
        standardOutput = stdout
        isIgnoreExitValue = true
    }
    return stdout.toString().trim().removePrefix("v")
}

fun isSnapshotBuild(): Boolean {
    val ref = System.getenv("GITHUB_REF") ?: ""
    return ref.endsWith("/master") || ref.endsWith("/main")
}

fun getCommitsSinceTag(tag: String): Int {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine = listOf("git", "rev-list", "$tag..HEAD", "--count")
        standardOutput = stdout
        isIgnoreExitValue = true
    }
    return stdout.toString().trim().toIntOrNull() ?: 0
}

val latestTag = findLatestTag().ifEmpty { "0.0" }
val versionString = if (isSnapshotBuild()) {
    val parts = latestTag.split(".")
    val patch = parts.lastOrNull()?.toIntOrNull()?.plus(1) ?: 1
    val base = if (parts.size >= 2) "${parts[0]}.${parts[1]}" else latestTag
    val buildNumber = getCommitsSinceTag("v$latestTag")
    "$base.$patch-SNAPSHOT-$buildNumber"
} else {
    latestTag
}

version = versionString

tasks.register("printVersion") {
    doLast {
        println(project.version)
    }
}