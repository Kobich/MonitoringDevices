plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.android.core.logger.client.api"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
