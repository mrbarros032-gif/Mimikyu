plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.metrolist.innertube"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    api(libs.innertubex)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.encoding)
    implementation(libs.okhttp.logging)
    api(libs.timber)
    testImplementation(libs.junit)

    coreLibraryDesugaring(libs.desugaring)
}

tasks.withType<com.android.build.gradle.internal.tasks.CheckAarMetadataTask>().configureEach {
    enabled = false
}
