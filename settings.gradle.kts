rootProject.name = "drill-hook"
pluginManagement {
    val kotlinVersion: String by extra
    val drillGradlePluginVersion: String by extra

    plugins {
        kotlin("multiplatform") version kotlinVersion
        id("com.epam.drill.cross-compilation") version drillGradlePluginVersion
    }

    repositories {
        mavenLocal()
        maven(url = "http://oss.jfrog.org/oss-release-local")
        gradlePluginPortal()
    }
}