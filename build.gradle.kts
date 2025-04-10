plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
}

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}

subprojects {
    val baseVersionName by extra("0.1.6")

    apply(plugin = "maven-publish")
    configure<PublishingExtension> {
        publications {
            all {
                group = "dev.sanmer.su"
                version = baseVersionName
            }
        }

        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/SanmerApps/ServiceManagerCompat")
                credentials {
                    username = System.getenv("GITHUB_ACTOR")
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }
}
