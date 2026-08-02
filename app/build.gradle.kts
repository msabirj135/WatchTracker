import java.util.Properties

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")

    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { inputStream ->
            load(inputStream)
        }
    }
}

val tmdbProxyBaseUrl = localProperties
    .getProperty("TMDB_PROXY_BASE_URL", "")
    .trim()
    .trimEnd('/')
    .takeIf { it.isNotBlank() }
    ?: "https://watchtracker-proxy-not-configured.invalid"

val watchTrackerAppKey = localProperties
    .getProperty("WATCHTRACKER_APP_KEY", "")
    .trim()

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.sabir.watchtracker"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.sabir.watchtracker"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            type = "String",
            name = "TMDB_PROXY_BASE_URL",
            value = "\"$tmdbProxyBaseUrl\""
        )

        buildConfigField(
            type = "String",
            name = "WATCHTRACKER_APP_KEY",
            value = "\"$watchTrackerAppKey\""
        )

        buildConfigField(
            type = "int",
            name = "DATABASE_SCHEMA_VERSION",
            value = "11"
        )

        buildConfigField(
            type = "String",
            name = "RELEASE_CHANNEL",
            value = "\"Personal\""
        )
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
