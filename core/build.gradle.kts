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
        currentTarget(presetName) {
            binaries
                .forEach {
                    it.linkerOpts("-L$binariesFolder", "-lfunchook", "-rpath", "$binariesFolder")
                }
        }
    }


    sourceSets {

        maybeCreate("${presetName}Main").dependencies {
            implementation(project(":core:common"))
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
    environment("PATH", binariesFolder)
}