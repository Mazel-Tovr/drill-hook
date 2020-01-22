
plugins {
    id("org.jetbrains.kotlin.multiplatform")
}
repositories {
    mavenCentral()
    maven(url = "https://oss.jfrog.org/artifactory/list/oss-release-local")
}

val binariesFolder = libDir.resolve(presetName)

kotlin {
    targets {
        currentTarget("nativeCommon") {
            compilations["main"].cinterops.create("hook_bindings").includeDirs(libDir.resolve("include"))
            binaries
                .forEach {
                    it.linkerOpts("-L$binariesFolder", "-lfunchook", "-rpath", "$binariesFolder")
                }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest> {
    testLogging.showStandardStreams = true
}