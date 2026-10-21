plugins {
    id("ch.acanda.gradle.fabrikt") version "1.18.0"
}

repositories {
    mavenCentral()
}

val consumerOpenApiTestFile = rootProject.layout.buildDirectory.file("generated/openapi/openapi-consumer.json")

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

tasks.named("fabriktGenerate") {
    dependsOn(rootProject.tasks.named("consumerOpenApiSpecTest"))
}
