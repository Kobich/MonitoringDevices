plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.engboost.monitoringdevices.service.monitoring.api"
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
    api(project(":scanner:wifi:api"))
    api(project(":scanner:bluetooth:api"))
    api(libs.kotlinx.coroutines.core)
}
