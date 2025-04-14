plugins {
    alias(libs.plugins.self.library)
    alias(libs.plugins.rikka.refine)
    alias(libs.plugins.kotlin.parcelize)
    `maven-publish`
}

android {
    namespace = "dev.sanmer.su"

    defaultConfig {
        consumerProguardFile("proguard-rules.pro")
    }

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

    implementation(libs.iamr0s.dhizuku.api)

    implementation(libs.androidx.annotation)
    implementation(libs.kotlinx.coroutines.android)
}
