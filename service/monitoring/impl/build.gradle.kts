plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.engboost.monitoringdevices.service.monitoring.impl"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    api(project(":service:monitoring:api"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.koin.android)
}
