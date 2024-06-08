plugins {
    alias(libs.plugins.self.library)
    alias(libs.plugins.rikka.refine)
    `maven-publish`
}

android {
    namespace = "dev.sanmer.su"

    buildFeatures {
        aidl = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

publishing {
    publications {
        register<MavenPublication>("core") {
            artifactId = "core"

            afterEvaluate {
                from(components.getByName("release"))
            }
        }
    }
}

dependencies {
    compileOnly(projects.stub)
    implementation(libs.hiddenApiBypass)
    implementation(libs.rikka.refine.runtime)

    implementation(libs.libsu.core)
    implementation(libs.libsu.service)

    implementation(libs.rikka.shizuku.api)
    implementation(libs.rikka.shizuku.provider)

    implementation(libs.androidx.annotation)
    implementation(libs.kotlinx.coroutines.android)
}
