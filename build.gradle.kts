import org.apache.tools.ant.taskdefs.condition.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*

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

kotlin {
    // For ARM, should be changed to iosArm32 or iosArm64
    // For Linux, should be changed to e.g. linuxX64
    // For MacOS, should be changed to e.g. macosX64
    // For Windows, should be changed to e.g. mingwX64

    targets {
        if (isDevMode) {
            currentTarget("nativeCommon") {
                compilations["main"].cinterops.create("hook_bindings").includeDirs("./lib/include")
                binaries {
                    val resolve = file("./lib").resolve(presetName)
                    sharedLib(libName, setOf(DEBUG)) {
                        linkerOpts("-L$resolve", "-lfunchook", "-rpath", "$resolve")
                    }
                    executable {
                        // Change to specify fully qualified name of your application"s entry point:
                        entryPoint = "com.epam.drill.hook.main"
                        // Specify command-line arguments, if necessary:

                        linkerOpts("-L$resolve", "-lfunchook", "-rpath", "$resolve")
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
        @Suppress("UNUSED_VARIABLE") val commonNativeTest = maybeCreate("nativeAgentTest")
        if (!isDevMode) {
            kotlin.targets.filterIsInstance<KotlinNativeTarget>().forEach {
                it.compilations.forEach { knCompilation ->
                    if (knCompilation.name == "main")
                        knCompilation.defaultSourceSet { dependsOn(commonNativeMain) }
                    else
                        knCompilation.defaultSourceSet { dependsOn(commonNativeTest) }

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
    if (isDevMode)
        dependsOn(tasks.getByPath("link${libName.capitalize()}DebugShared${"nativeCommon".capitalize()}"))
    else
        dependsOn(tasks.getByPath("link${libName.capitalize()}DebugShared${presetName.capitalize()}"))

    testLogging.showStandardStreams = true

    val targetFromPreset = (
            if (isDevMode) kotlin.targets["nativeCommon"]
            else kotlin.targetFromPreset(kotlin.presets.getByName(presetName))) as KotlinNativeTarget
    println(targetFromPreset)
    println(targetFromPreset.name)
    jvmArgs(
        "-agentpath:${targetFromPreset
            .binaries
            .findSharedLib(libName, NativeBuildType.DEBUG)!!
            .outputFile.toPath()}"
    )
}
