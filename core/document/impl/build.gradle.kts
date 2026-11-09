plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.android.core.document.impl"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        // OpenDocument.core needs API 26: merged with tools:overrideLibrary,
        // office readers are registered only on API 26+ (see DocumentCore).
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    api(project(":core:document:api"))

    implementation(libs.odr.core.android)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.exifinterface)
    implementation(libs.kotlinx.coroutines.core)
}
