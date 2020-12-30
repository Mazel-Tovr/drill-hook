import com.epam.drill.gradle.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*

plugins {
    kotlin("multiplatform")
    id("com.epam.drill.cross-compilation")
    `maven-publish`
}

repositories {
    mavenCentral()
    jcenter()
}

val atomicfuVersion: String by extra
val kxCollectionsVersion: String by extra

kotlin {

    crossCompilation {
        common {
            addCInterop()
            dependencies {
                implementation("org.jetbrains.kotlinx:atomicfu:$atomicfuVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:$kxCollectionsVersion")
            }
        }
    }

    setOf(
        macosX64(),
        mingwX64(),
        linuxX64()
    ).forEach {
        it.mainCompilation.addCInterop()
    }

}


tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest> {
    testLogging.showStandardStreams = true
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile> {
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.ExperimentalUnsignedTypes"
}

publishing {
    repositories {
        maven {
            url = uri("https://oss.jfrog.org/oss-release-local")
            credentials {
                username =
                    if (project.hasProperty("bintrayUser"))
                        project.property("bintrayUser").toString()
                    else System.getenv("BINTRAY_USER")
                password =
                    if (project.hasProperty("bintrayApiKey"))
                        project.property("bintrayApiKey").toString()
                    else System.getenv("BINTRAY_API_KEY")
            }
        }
    }
}

fun KotlinNativeCompilation.addCInterop() {
    cinterops.create("hook_bindings").includeDirs(rootProject.file("lib").resolve("include"))
}
