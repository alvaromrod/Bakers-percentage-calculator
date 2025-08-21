plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.pep1lo.bakerspercentagecalculator"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pep1lo.bakerspercentagecalculator"
        minSdk = 24
        targetSdk = 35
        versionCode = 4
        versionName = "1.4-beta"

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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("com.google.android.material:material:1.12.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
}