plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "com.example.medai.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidxMedia)
    implementation(libs.androidxCoreKtx)
    implementation(libs.androidxAppcompat)
    implementation(libs.retrofit)
    implementation(libs.retrofitConverterGson)
    implementation(libs.okhttp)
    implementation(libs.okhttpLogging)
    implementation(libs.kotlinxCoroutinesCore)
    implementation(libs.kotlinxCoroutinesAndroid)
}