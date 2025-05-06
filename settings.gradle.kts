plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
rootProject.name = "Streamit-service"
include("libs")
include("libs:shared")

findProject(":libs:shared")?.name = "shared"