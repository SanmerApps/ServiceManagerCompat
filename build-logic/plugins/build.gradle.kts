plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.android.gradle)
    compileOnly(libs.kotlin.gradle)
}

gradlePlugin {
    plugins {
        register("self.library") {
            id = "self.library"
            implementationClass = "LibraryConventionPlugin"
        }
    }
}