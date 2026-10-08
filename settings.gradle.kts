pluginManagement {
    val tsGenVersion: String by settings

    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://reposilite.iktdev.no/releases")
        maven("https://reposilite.iktdev.no/snapshots")
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "no.iktdev.ts-gen") {
                useModule("no.iktdev:ts-gen:$tsGenVersion")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
rootProject.name = "Streamit-service"
