plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.engboost.monitoringdevices.core.document.impl"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        // OpenDocument.core needs API 26: it is merged with tools:overrideLibrary and
        // office readers are registered only on API 26+ (see DefaultDocumentReaderVariants).
        minSdk = 24
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
