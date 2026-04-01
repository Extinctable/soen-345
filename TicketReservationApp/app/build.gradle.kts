plugins {
    id("com.android.application")

    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "code.ticketreservationapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "code.ticketreservationapp"
        minSdk = 26
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all {
                it.useJUnitPlatform()
            }
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    
    // JUnit 5 (Jupiter)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    
    // Mockito for mocking
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    
    // Android instrumentation tests
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.database)
}
