pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google(); mavenCentral()
        maven("https://repository.map.naver.com/archive/maven") { content { includeGroup("com.naver.maps") } }
        maven("https://nexus-playground.pleos.ai/repository/maven-releases/") { content { includeGroupByRegex("ai\\.pleos.*") } }
    }
}
rootProject.name = "Changyeoga"
include(":app")
