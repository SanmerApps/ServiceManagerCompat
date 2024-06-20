plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
}

task<Delete>("clean") {
    delete(layout.buildDirectory)
}

subprojects {
    apply(plugin = "maven-publish")
    configure<PublishingExtension> {
        publications {
            all {
                group = "dev.sanmer.su"
                version = "0.1.1"
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