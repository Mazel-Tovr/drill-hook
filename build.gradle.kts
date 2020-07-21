import com.epam.drill.gradle.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*

plugins {
    id("org.jetbrains.kotlin.multiplatform") version "1.3.70"
    id("com.epam.drill.cross-compilation") version "0.15.1"
    `maven-publish`
}

repositories {
    mavenCentral()
    jcenter()
}

kotlin {

    crossCompilation {
        common {
            addCInterop()
            dependencies {
                implementation("org.jetbrains.kotlinx:atomicfu-native:0.14.3")
                implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.2")
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