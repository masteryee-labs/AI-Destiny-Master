pluginManagement {
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
        maven("https://jitpack.io")
        maven("https://maven.scijava.org/content/repositories/public/")
    }
}

rootProject.name = "AIDestinyMaster"

include(
    ":app",
    ":core:astro",
    ":core:lunar",
    ":core:ai",
    ":data",
    ":sync",
    ":billing",
    ":ads",
    ":features:bazi",
    ":features:ziwei",
    ":features:astrochart",
    ":features:design",
    ":features:almanac",
    ":features:mix-ai"
)
