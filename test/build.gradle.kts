import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

val libName = "hook"

val JVM_TEST_TARGET_NAME = "jvmAgent"

kotlin {
    targets {
        currentTarget(JVM_TEST_TARGET_NAME) {
            binaries.apply { sharedLib(libName, setOf(DEBUG)) }.forEach {
                if (org.jetbrains.kotlin.konan.target.HostManager.hostIsMingw)
                    it.linkerOpts("-lpsapi", "-lwsock32", "-lws2_32", "-lmswsock")
            }
        }
    }
    val jvm = jvm {
        compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation(kotlin("reflect"))
                implementation(kotlin("test-junit"))
            }
        }
    }
    val jvmCommonSourceset = jvm.compilations["main"].defaultSourceSet
    jvm("SunServer") {
        compilations["test"].defaultSourceSet {
            dependsOn(jvmCommonSourceset)
        }
    }

    jvm("UndertowServer") {
        compilations["test"].defaultSourceSet {
            dependsOn(jvmCommonSourceset)
            dependencies {
                implementation("io.undertow:undertow-core:2.0.29.Final")
                implementation("io.undertow:undertow-servlet:2.0.29.Final")
            }
        }
    }
    jvm("JettyServer") {
        compilations["test"].defaultSourceSet {
            dependsOn(jvmCommonSourceset)
            dependencies {
                implementation("org.eclipse.jetty:jetty-server:9.4.26.+")
            }
        }
    }

    sourceSets {

        val common = maybeCreate("${JVM_TEST_TARGET_NAME}Main")
        with(common) {
            dependencies {
                implementation(project(":platform"))
                implementation("com.epam.drill:jvmapi-native:0.0.2")
            }
        }


        val jvmsTargets = targets.filterIsInstance<org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget>()
            .filter { it.name != "jvm" }
        jvmsTargets.forEach {
            it.compilations.forEach { knCompilation ->
                if (knCompilation.name == "test") {
                    knCompilation.defaultSourceSet {
                        dependencies {
                            implementation(kotlin("test-junit"))
                        }
                    }
                } else {
                    knCompilation.defaultSourceSet {
                        dependencies {
                            implementation(kotlin("stdlib"))
                            implementation(kotlin("reflect"))
                        }
                    }
                }

            }
        }

        named("commonMain") {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest> {
    testLogging.showStandardStreams = true
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.ExperimentalStdlibApi"
}
tasks.withType<org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest> {
    dependsOn(tasks.getByPath("link${libName.capitalize()}DebugShared${JVM_TEST_TARGET_NAME.capitalize()}"))
    testLogging.showStandardStreams = true

    val targetFromPreset =
        (kotlin.targets[JVM_TEST_TARGET_NAME]) as KotlinNativeTarget
    jvmArgs(
        "-agentpath:${targetFromPreset
            .binaries
            .findSharedLib(libName, NativeBuildType.DEBUG)!!
            .outputFile.toPath()}"
    )
}
