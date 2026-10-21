plugins {
    id("ch.acanda.gradle.fabrikt") version "1.18.0"
    kotlin("jvm") version "2.2.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")
}

val consumerOpenApiTestFile = rootProject.layout.buildDirectory.file("generated/openapi/openapi-consumer.json")
val generatedKotlin = layout.buildDirectory.dir("generated/fabrikt-validation/kotlin")

fabrikt {
    generate("streamit-consumer-test") {
        apiFile = consumerOpenApiTestFile.get().asFile
        basePackage = "no.iktdev.streamit.service.openapi.validation.consumer"
        outputDirectory = layout.buildDirectory.dir("generated/fabrikt-validation").get().asFile
        sourcesPath = "kotlin"
        validationLibrary = NoValidation
        model {
            generate = enabled
            serializationLibrary = Jackson
        }
    }
}

kotlin {
    sourceSets.named("main") {
        kotlin.srcDir(generatedKotlin)
    }
}

tasks.named("fabriktGenerate") {
    dependsOn(rootProject.tasks.named("consumerOpenApiSpecTest"))
}

tasks.named("compileKotlin") {
    dependsOn("fabriktGenerate")
}

tasks.register("verifyFabriktModels") {
    group = "verification"
    description = "Generates and compiles consumer models, then verifies Catalog inheritance."
    dependsOn("compileKotlin")

    doLast {
        val packageDirectory = generatedKotlin.get().dir(
            "no/iktdev/streamit/service/openapi/validation/consumer/models"
        ).asFile
        val catalog = packageDirectory.resolve("Catalog.kt")
        val movie = packageDirectory.resolve("Movie.kt")
        val serie = packageDirectory.resolve("Serie.kt")

        check(catalog.isFile && movie.isFile && serie.isFile) {
            "Fabrikt did not generate Catalog, Movie, and Serie models in $packageDirectory"
        }
        check(Regex("(?:public\\s+)?(?:sealed\\s+)?(?:open\\s+)?class Catalog\\s*\\(").containsMatchIn(catalog.readText())) {
            "Expected Catalog to be generated as a base model, not a oneOf-only type."
        }
        check(
            Regex("(?:public\\s+)?(?:data\\s+)?class Movie\\s*\\(").containsMatchIn(movie.readText()) &&
                Regex(":\\s*Catalog\\s*\\(").containsMatchIn(movie.readText())
        ) {
            "Expected Movie to inherit from Catalog."
        }
        check(
            Regex("(?:public\\s+)?(?:data\\s+)?class Serie\\s*\\(").containsMatchIn(serie.readText()) &&
                Regex(":\\s*Catalog\\s*\\(").containsMatchIn(serie.readText())
        ) {
            "Expected Serie to inherit from Catalog."
        }
        check(Regex("val video: Video(?!\\?)").containsMatchIn(movie.readText())) {
            "Expected Movie.video to remain required and non-null."
        }
        check(Regex("val episodes: List<Episode>(?!\\?)").containsMatchIn(serie.readText())) {
            "Expected Serie.episodes to remain required and non-null."
        }
    }
}
