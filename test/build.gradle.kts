import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

repositories {
    mavenCentral()
    maven(url = "https://oss.jfrog.org/artifactory/list/oss-release-local")
}

val libName = "hook"

val binariesFolder = libDir.resolve(presetName)

val JVM_TEST_TARGET_NAME = "jvmAgent"

kotlin {
    targets {
        currentTarget(JVM_TEST_TARGET_NAME) {
            binaries.apply { sharedLib(libName, setOf(DEBUG)) }
                .forEach {
                    it.linkerOpts("-L$binariesFolder", "-lfunchook", "-rpath", "$binariesFolder")
                }
        }
    }

    jvm()

    sourceSets {

        val common = maybeCreate("${JVM_TEST_TARGET_NAME}Main")
        with(common) {
            dependencies {
                implementation(project(":core"))
            }
        }

        jvm("jvm").compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation(kotlin("reflect"))
            }
        }
        jvm("jvm").compilations["test"].defaultSourceSet {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest> {
    testLogging.showStandardStreams = true
}
tasks.withType<org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest> {
    dependsOn(tasks.getByPath("link${libName.capitalize()}DebugShared${JVM_TEST_TARGET_NAME.capitalize()}"))
    testLogging.showStandardStreams = true

    val targetFromPreset =
        (kotlin.targets[JVM_TEST_TARGET_NAME]) as KotlinNativeTarget
    environment("PATH" to binariesFolder)
    jvmArgs(
        "-agentpath:${targetFromPreset
            .binaries
            .findSharedLib(libName, NativeBuildType.DEBUG)!!
            .outputFile.toPath()}"
    )
}
