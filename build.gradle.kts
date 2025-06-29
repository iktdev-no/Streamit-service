import java.io.ByteArrayOutputStream
import kotlin.sequences.ifEmpty

plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.spring") version "1.5.31"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    id("org.springframework.boot") version "2.5.5"
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

    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-configuration-processor")

    implementation("org.springdoc:springdoc-openapi-ui:1.6.4")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation ("mysql:mysql-connector-java:8.0.29")

    implementation ("com.auth0:java-jwt:4.0.0")
    implementation ("com.google.code.gson:gson:2.9.0")
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.11")
    implementation("no.iktdev:exfl:0.0.16-SNAPSHOT")

    implementation("no.iktdev.streamit.library:streamit-library-db:1.0-rc1")

    implementation(project(mapOf("path" to ":libs:shared")))

    implementation("com.google.zxing:core:3.5.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.1")
    testImplementation("org.assertj:assertj-core:3.4.1")
    testImplementation("org.mockito:mockito-core:3.+")
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    testImplementation("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:1.13.10")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
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
    // Use environment variable or branch name to detect snapshot
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