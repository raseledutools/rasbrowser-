plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.google.dagger.hilt.android)
    alias(libs.plugins.google.devtools.ksp)
}

android {
    namespace = "com.familybrowser"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.familybrowser"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // ── Core ──────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // ── Compose BOM ───────────────────────────────────────────
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.activity:activity-compose:1.9.0")

    // ── Lifecycle / ViewModel ─────────────────────────────────
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.common.java8)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    // ── Navigation ────────────────────────────────────────────
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // ── Hilt DI ───────────────────────────────────────────────
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.fragment)

    // ── Coroutines ────────────────────────────────────────────
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // ── WebView ───────────────────────────────────────────────
    implementation("androidx.webkit:webkit:1.11.0")

    // ── Security ──────────────────────────────────────────────
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // ── Room ──────────────────────────────────────────────────
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ── DataStore ─────────────────────────────────────────────
    implementation(libs.androidx.datastore.preferences)

    // ── Network ───────────────────────────────────────────────
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.gson)

    // ── Image Loading ─────────────────────────────────────────
    implementation(libs.glide.library)
    kapt(libs.glide.compiler)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // ── Splash Screen ─────────────────────────────────────────
    implementation("androidx.core:core-splashscreen:1.0.1")

    // ── Accompanist ───────────────────────────────────────────
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.34.0")
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // ── Fragment ──────────────────────────────────────────────
    implementation(libs.androidx.fragment.ktx)

    // ── UI Extras ─────────────────────────────────────────────
    implementation(libs.lottie)
    implementation(libs.shimmer)
    implementation(libs.timber)

    // ── Testing ───────────────────────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
