plugins {
    `kotlin-dsl`
}

repositories {
    mavenLocal()
    jcenter()
}

val kotlinVersion = "1.3.61"
dependencies {
    implementation(kotlin("gradle-plugin", kotlinVersion))
    implementation(kotlin("stdlib-jdk8", kotlinVersion))
    implementation(kotlin("reflect", kotlinVersion))
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}
