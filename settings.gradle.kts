rootProject.name = "drill-hook"
pluginManagement {
    val kotlinVersion: String by extra
    val licenseVersion: String by extra
    plugins {
        kotlin("multiplatform") version kotlinVersion
        id("com.github.hierynomus.license") version licenseVersion
    }

    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}
