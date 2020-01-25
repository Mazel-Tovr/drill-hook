plugins {
    id("org.jetbrains.kotlin.multiplatform")
    `maven-publish`
}

kotlin {
    targets {

        if (isDevMode)
            currentTarget {
                if (org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_UNIX))
                    compilations["main"].defaultSourceSet.kotlin.srcDirs("./src/commonUnixMain/kotlin")
                binaries.forEach {
                    if (org.jetbrains.kotlin.konan.target.HostManager.hostIsMingw)
                        it.linkerOpts("-lpsapi", "-lwsock32", "-lws2_32", "-lmswsock")
                }
            }
        else {
            setOf(linuxX64(), macosX64(), mingwX64 {
                binaries.forEach { it.linkerOpts("-lpsapi", "-lwsock32", "-lws2_32", "-lmswsock") }
            }).forEach {
                it.compilations["main"].defaultSourceSet {
                    dependencies {
                        implementation(project(":core"))
                    }
                }
            }
        }

        sourceSets {
            named("commonMain") {
                dependencies {
                    implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
                }
            }
            maybeCreate("${presetName}Main").dependencies {
                implementation(project(":core"))
            }
            if (!isDevMode) {
                if (org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_UNIX)) {
                    val commonNativeMain = maybeCreate("commonUnixMain")
                    targets.filterIsInstance<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>()
                        .filter { it.konanTarget.family.name == "LINUX" || it.konanTarget.family.name == "OSX" }
                        .forEach {
                            it.compilations.forEach { knCompilation ->
                                if (knCompilation.name == "main")
                                    knCompilation.defaultSourceSet { dependsOn(commonNativeMain) }
                            }
                        }

                }
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest> {
    testLogging.showStandardStreams = true
}