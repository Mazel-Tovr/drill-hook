import org.jetbrains.kotlin.gradle.plugin.mpp.*
import java.net.*

plugins {
    kotlin("multiplatform")
    id("com.github.hierynomus.license")
    `maven-publish`
}

val scriptUrl: String by extra

repositories {
    mavenLocal()
    mavenCentral()
    apply(from = "$scriptUrl/maven-repo.gradle.kts")
}

val atomicfuVersion: String by extra
val kxCollectionsVersion: String by extra

kotlin {

    targets {
        setOf(
            macosX64(),
            mingwX64(),
            linuxX64()
        ).forEach {
            it.compilations["main"].addCInterop()
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:atomicfu:$atomicfuVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:$kxCollectionsVersion")
            }
        }
        val commonNative by creating {
            dependsOn(commonMain)
        }

        val posixNative by creating {
            dependsOn(commonNative)
        }
        val linuxX64Main by getting {
            dependsOn(posixNative)
        }
        val macosX64Main by getting {
            dependsOn(posixNative)
        }
        val mingwX64Main by getting {
            dependsOn(commonNative)
        }
    }

}


tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest> {
    testLogging.showStandardStreams = true
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile> {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.ExperimentalUnsignedTypes"
}


fun KotlinNativeCompilation.addCInterop() {
    cinterops.create("hook_bindings").includeDirs(rootProject.file("lib").resolve("include"))
}

val licenseFormatSettings by tasks.registering(com.hierynomus.gradle.license.tasks.LicenseFormat::class) {
    source = fileTree(project.projectDir).also {
        include("**/*.kt", "**/*.java", "**/*.groovy")
        exclude("**/.idea")
    }.asFileTree
}

license {
    headerURI = URI("https://raw.githubusercontent.com/Drill4J/drill4j/develop/COPYRIGHT")
}

tasks["licenseFormat"].dependsOn(licenseFormatSettings)
