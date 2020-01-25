plugins {
    `maven-publish`
}
subprojects {
    repositories {
        mavenCentral()
        maven(url = "https://oss.jfrog.org/artifactory/list/oss-release-local")
    }

    if (name != "test") {
        apply {
            plugin(org.gradle.api.publish.maven.plugins.MavenPublishPlugin::class)
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
    }
}