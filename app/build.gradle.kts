import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.smartcampusassist"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.smartcampusassist"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        buildConfigField(
            "String",
            "GEMINI_API_KEY",
            "\"${localProperties.getProperty("gemini.api.key", "")}\""
        )
        buildConfigField(
            "String",
            "ALLOWED_LOGIN_EMAIL",
            "\"${localProperties.getProperty("allowed.login.email", "")}\""
        )
        buildConfigField(
            "String",
            "FIREBASE_STORAGE_BUCKET",
            "\"smartcampusassist.firebasestorage.app\""
        )
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

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    /* CORE */
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    /* SPLASH SCREEN (IMPORTANT) */
    implementation("androidx.core:core-splashscreen:1.0.1")

    /* MATERIAL XML THEMES (IMPORTANT) */
    implementation("com.google.android.material:material:1.12.0")

    /* COMPOSE */
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    /* NAVIGATION */
    implementation("androidx.navigation:navigation-compose:2.7.7")

    /* VIEWMODEL */
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")

    /* COROUTINES */
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    /* FIREBASE */
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage")

    /* GOOGLE LOGIN */
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    /* OTHER */
    implementation("com.google.accompanist:accompanist-navigation-animation:0.32.0")
    implementation("com.google.ai.client.generativeai:generativeai:0.7.0")

    /* TEST */
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
