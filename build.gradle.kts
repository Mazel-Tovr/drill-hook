import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithTestsPreset
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    id("org.jetbrains.kotlin.multiplatform").version("1.3.61")
}
repositories {
    mavenCentral()
    maven(url = "https://oss.jfrog.org/artifactory/list/oss-release-local")
}

val isDevMode = System.getProperty("idea.active") == "true"

val presetName: String =
    when {
        Os.isFamily(Os.FAMILY_MAC) -> "macosX64"
        Os.isFamily(Os.FAMILY_UNIX) -> "linuxX64"
        Os.isFamily(Os.FAMILY_WINDOWS) -> "mingwX64"
        else -> throw RuntimeException("Target ${System.getProperty("os.name")} is not supported")
    }

fun KotlinMultiplatformExtension.currentTarget(
    name: String? = null,
    config: KotlinNativeTarget.() -> Unit = {}
): KotlinNativeTarget {
    val createTarget =
        (presets.getByName(presetName) as KotlinNativeTargetWithTestsPreset).createTarget(name ?: presetName)
    targets.add(createTarget)
    config(createTarget)
    return createTarget
}

val libName = "hook"

val binariesFolder = file("./lib").resolve(presetName)

kotlin {
    targets {
        if (isDevMode) {
            currentTarget("nativeCommon") {
                compilations["main"].cinterops.create("hook_bindings").includeDirs("./lib/include")
                binaries {
                    sharedLib(libName, setOf(DEBUG)) {
                        linkerOpts("-L$binariesFolder", "-lfunchook", "-rpath", "$binariesFolder")
                    }
                    executable {
                        // Change to specify fully qualified name of your application"s entry point:
                        entryPoint = "com.epam.drill.hook.main"
                        // Specify command-line arguments, if necessary:
                        runTask?.environment("PATH" to binariesFolder)
                        linkerOpts("-L$binariesFolder", "-lfunchook", "-rpath", "$binariesFolder")
                        runTask?.args("")

                    }
                }

            }
        } else {
            mingwX64 {
                compilations["main"].cinterops.create("hook_bindings").includeDirs("./lib/include")
                binaries {

                    sharedLib(libName, setOf(DEBUG)) {
                        val resolve = file("./lib").resolve("mingwX64")
                        linkerOpts(
                            "-L$resolve",
                            "-lfunchook",
                            "-rpath", "$resolve"
                        )
                    }
                }
            }
            macosX64 {
                compilations["main"].cinterops.create("hook_bindings").includeDirs("./lib/include")
                binaries {
                    sharedLib(libName, setOf(DEBUG)) {
                        val resolve = file("./lib").resolve("macosX64")
                        linkerOpts(
                            "-L$resolve",
                            "-lfunchook",
                            "-rpath", "$resolve"
                        )
                        println(linkerOpts)
                    }
                }
            }
            linuxX64 {
                compilations["main"].cinterops.create("hook_bindings").includeDirs("./lib/include")
                binaries {
                    sharedLib(libName, setOf(DEBUG)) {
                        val resolve = file("./lib").resolve("linuxX64")
                        linkerOpts(
                            "-L$resolve",
                            "-lfunchook",
                            "-rpath", "$resolve"
                        )

                    }
                }
            }
        }
    }

    jvm()

    sourceSets {

        val commonNativeMain = maybeCreate("nativeCommonMain")
        if (!isDevMode) {
            kotlin.targets.filterIsInstance<KotlinNativeTarget>().forEach {
                it.compilations.forEach { knCompilation ->
                    if (knCompilation.name == "main")
                        knCompilation.defaultSourceSet { dependsOn(commonNativeMain) }
                }
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

tasks.withType<org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest> {
    dependsOn(tasks.getByPath("link${libName.capitalize()}DebugShared${(if (isDevMode) "nativeCommon" else presetName).capitalize()}"))
    testLogging.showStandardStreams = true

    val targetFromPreset =
        (if (isDevMode) kotlin.targets["nativeCommon"] else kotlin.targetFromPreset(kotlin.presets.getByName(presetName))) as KotlinNativeTarget
    environment("PATH" to binariesFolder)
    jvmArgs(
        "-agentpath:${targetFromPreset
            .binaries
            .findSharedLib(libName, NativeBuildType.DEBUG)!!
            .outputFile.toPath()}"
    )
}
