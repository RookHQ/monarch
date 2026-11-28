pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "monarch-android"

include(":monarch-tokens")
include(":monarch-compose")
include(":monarch-fonts-poppins")
include(":monarch-sample-icons")
include(":sample")
