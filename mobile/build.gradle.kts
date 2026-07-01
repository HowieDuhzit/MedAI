plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "com.example.medai"
    buildFeatures {
        viewBinding = true
        dataBinding = false
    }
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.medai"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    packaging {
        resources {
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/NOTICE.md"
        }
    }
}

dependencies {
    implementation(libs.androidxCoreKtx)
    implementation(libs.androidxAppcompat)
    implementation(libs.material)
    implementation(libs.androidxActivityKtx)
    implementation(libs.androidxConstraintlayout)
    implementation(libs.kotlinxCoroutinesCore)
    implementation(libs.kotlinxCoroutinesAndroid)
    implementation(libs.lifecycleRuntime)
    implementation(libs.lifecycleViewmodel)
    implementation(libs.lifecycleViewmodelKtx)
    implementation(libs.databinding)
    implementation(libs.retrofit)
    implementation(libs.retrofitConverterGson)
    implementation(libs.okhttp)
    implementation(libs.okhttpLogging)
    implementation(project(":shared"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidxJunit)
    androidTestImplementation(libs.androidxEspressoCore)
}