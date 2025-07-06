pluginManagement {
    repositories {
        // 添加阿里云镜像，优先使用
        maven(url = "https://maven.aliyun.com/repository/gradle-plugin")
        maven(url = "https://maven.aliyun.com/repository/public")
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
    plugins {
        id("com.android.application") version "8.10.1"
        id("com.android.library") version "8.10.1"
        id("org.jetbrains.kotlin.android") version "2.0.21"
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 添加阿里云镜像，优先使用
        maven(url = "https://maven.aliyun.com/repository/public")
        google()
        mavenCentral()
    }
}

rootProject.name = "Dildogent"
include(":app")
include(":utils")
 