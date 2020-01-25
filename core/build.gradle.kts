plugins {
    id("org.jetbrains.kotlin.multiplatform")
    `maven-publish`
}

kotlin {
    targets {
        if (isDevMode)
            currentTarget {
                compilations["main"].apply {
                    defaultSourceSet.kotlin.srcDir("./src/nativeCommonMain/kotlin")
                    cinterops.create("hook_bindings").includeDirs(libDir.resolve("include"))
                }
            }
        else {
            setOf(linuxX64(), macosX64(), mingwX64()).forEach {
                it.compilations["main"].cinterops.create("hook_bindings").includeDirs(libDir.resolve("include"))
            }
        }
    }
    sourceSets {
        if (!isDevMode) {
            val commonNativeMain = maybeCreate("nativeCommonMain")
            targets.filterIsInstance<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().forEach {
                it.compilations.forEach { knCompilation ->
                    if (knCompilation.name == "main")
                        knCompilation.defaultSourceSet { dependsOn(commonNativeMain) }
                }
            }
        }
    }
}


tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest> {
    testLogging.showStandardStreams = true
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile> {
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.ExperimentalUnsignedTypes"
}