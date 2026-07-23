import java.io.File
import java.util.Properties

plugins {
    id("com.android.application") version "8.7.3"
    id("org.jetbrains.kotlin.android") version "2.0.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    id("com.google.devtools.ksp") version "2.0.21-1.0.27"
}

// Helper to load env variables from .env or local.properties files
fun getEnvValue(key: String, defaultValue: String = ""): String {
    // 1. Try reading from .env file
    val envFile = File(rootDir, ".env")
    if (envFile.exists()) {
        val properties = Properties()
        envFile.inputStream().use { properties.load(it) }
        val value = properties.getProperty(key)
        if (value != null && value.isNotEmpty()) {
            return value
        }
    }
    // 2. Try reading from local.properties file
    val localPropsFile = File(rootDir, "local.properties")
    if (localPropsFile.exists()) {
        val properties = Properties()
        localPropsFile.inputStream().use { properties.load(it) }
        val value = properties.getProperty(key)
        if (value != null && value.isNotEmpty()) {
            return value
        }
    }
    // 3. Fallback to system environment variables
    val sysEnv = System.getenv(key)
    if (sysEnv != null && sysEnv.isNotEmpty()) {
        return sysEnv
    }
    return defaultValue
}


android {
    namespace = "com.jarvis"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jarvis"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Inject configuration variables from .env file
        buildConfigField("String", "GEMINI_API_KEY", "\"${getEnvValue("GEMINI_API_KEY")}\"")
        buildConfigField("String", "LLM_BASE_URL", "\"${getEnvValue("LLM_BASE_URL", "https://generativelanguage.googleapis.com/v1beta")}\"")
        buildConfigField("String", "LLM_MODEL", "\"${getEnvValue("LLM_MODEL", "gemini-2.5-flash")}\"")
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }


}

dependencies {
    // --- Core Android ---
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("com.google.android.material:material:1.12.0")

    // --- Jetpack Compose ---
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.3")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // --- Coroutines ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // --- Room Database (Step 6: Memory System) ---
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // --- WorkManager (Step 12: Reliability & Task Scheduling) ---
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // --- Biometric (Step 13: Personalization & Safety Prompts) ---
    implementation("androidx.biometric:biometric:1.1.0")

    // --- Security / EncryptedSharedPreferences (Step 14: Keystore Sync) ---
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // --- ML Kit Text Recognition (Step 10: Screen Understanding) ---
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // --- Lifecycle ---
    val lifecycleVersion = "2.8.7"
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-common-java8:$lifecycleVersion")

    // --- JSON Parsing (Step 9: Workflow serialization) ---
    // Uses org.json (bundled with Android SDK, no extra dependency needed)

    // --- Testing ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
