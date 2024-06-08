plugins {
    alias(libs.plugins.self.library)
}

android {
    namespace = "dev.sanmer.su.stub"
}

dependencies {
    annotationProcessor(libs.rikka.refine.compiler)
    compileOnly(libs.rikka.refine.annotation)
    compileOnly(libs.androidx.annotation)
}