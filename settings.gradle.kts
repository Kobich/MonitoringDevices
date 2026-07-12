pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MonitoringDevices"
include(":app")
include(":feature:wifi:api")
include(":feature:wifi:impl")
include(":feature:bluetooth:api")
include(":feature:bluetooth:impl")
include(":feature:radio:api")
include(":feature:radio:impl")
include(":scanner:wifi:api")
include(":scanner:wifi:impl")
include(":scanner:bluetooth:api")
include(":scanner:bluetooth:impl")
include(":scanner:radio:api")
include(":scanner:radio:impl")
 
