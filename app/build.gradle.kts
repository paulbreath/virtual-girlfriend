plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.universal"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.universal"
        minSdk = 24
        targetSdk = 33
        versionCode = 1
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val moondreamAuth = (project.findProperty("MOONDREAM_AUTH") as String?)
            ?.replace("\"", "\\\"")
            ?: ""
        buildConfigField("String", "MOONDREAM_AUTH", "\"$moondreamAuth\"")

        val puterAuthToken = (project.findProperty("PUTER_AUTH_TOKEN") as String?)
            ?.replace("\"", "\\\"")
            ?: ""
        buildConfigField("String", "PUTER_AUTH_TOKEN", "\"$puterAuthToken\"")

        val zImageApiKey = (project.findProperty("ZIMAGE_API_KEY") as String?)
            ?.replace("\"", "\\\"")
            ?: ""
        buildConfigField("String", "ZIMAGE_API_KEY", "\"$zImageApiKey\"")

        val grokApiKey = (project.findProperty("GROK_API_KEY") as String?)
            ?.replace("\"", "\\\"")
            ?: ""
        buildConfigField("String", "GROK_API_KEY", "\"$grokApiKey\"")
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
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

// Allow debug builds without google-services.json
afterEvaluate {
    tasks.matching {
        it.name.endsWith("GoogleServices") && it.name.contains("Debug")
    }.configureEach {
        enabled = false
    }
}

dependencies {
    // Firebase - KEPT
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.code.gson:gson:2.10.1")
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Material Design
    implementation("com.google.android.material:material:1.11.0")

    // UI Components
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // JavaScript Engine - FIXED VERSION
    implementation("org.mozilla:rhino:1.7.13")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
