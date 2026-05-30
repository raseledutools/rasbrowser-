plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.google.dagger.hilt.android) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("androidx.navigation.safeargs.kotlin") version "2.8.6" apply false
}
