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
    implementation("androidx.lifecycle:lifecycle-process:2.11.0")

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

// One-time source migration for the theatre statistics fix.
// It runs before Kotlin compilation and is idempotent.
val fixTheatreStatisticsSource by tasks.registering {
    doLast {
        val sourceFile = file("src/main/java/com/sabir/watchtracker/ui/library/LibraryScreens.kt")
        var source = sourceFile.readText()

        val oldSection = """        if (libraryUiState.theatreWatchEntries.isNotEmpty()) {
            item { TheatreStatisticsCard(libraryUiState) }
        }

        item {
            StatisticsYearSelector(
                years = availableYears,
                selectedYear = selectedYear,
                onYearSelected = { year -> selectedYear = year }
            )
        }
"""

        val newSection = """        item {
            StatisticsYearSelector(
                years = availableYears,
                selectedYear = selectedYear,
                onYearSelected = { year -> selectedYear = year }
            )
        }

        if (libraryUiState.theatreWatchEntries.isNotEmpty()) {
            item {
                TheatreStatisticsCard(
                    state = libraryUiState,
                    selectedYear = selectedYear
                )
            }
        }
"""

        if (source.contains(oldSection)) {
            source = source.replace(oldSection, newSection)
        }

        val oldFunction = """@Composable
private fun TheatreStatisticsCard(state: LibraryUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ScreenSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            ScreenPrimary.copy(alpha = 0.14f),
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🎟", fontSize = 21.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Theatre watches",
                        color = ScreenTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Your movies watched on the big screen",
                        color = ScreenTextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnnualMetric(
                    modifier = Modifier.weight(1f),
                    value = state.theatreWatchEntries.size.toString(),
                    label = "Movies"
                )
                AnnualMetric(
                    modifier = Modifier.weight(1f),
                    value = state.theatreMoviesThisYear.toString(),
                    label = "This year"
                )
                AnnualMetric(
                    modifier = Modifier.weight(1f),
                    value = formatWatchHours(state.theatreWatchMinutes),
                    label = "Watch time"
                )
            }
        }
    }
}
"""

        val newFunction = """@Composable
private fun TheatreStatisticsCard(
    state: LibraryUiState,
    selectedYear: Int
) {
    val yearVisits = state.theatreWatchEntries
        .flatMap { entry ->
            entry.visitDates.map { date -> entry.item to date }
        }
        .filter { (_, date) ->
            LocalDate.ofEpochDay(date).year == selectedYear
        }

    val movieCount = yearVisits
        .map { (item, _) -> item.mediaType to item.tmdbId }
        .distinct()
        .size

    val watchMinutes = yearVisits.sumOf { (item, _) ->
        item.runtimeMinutes ?: 0
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ScreenSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            ScreenPrimary.copy(alpha = 0.14f),
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🎟", fontSize = 21.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Theatre watches",
                        color = ScreenTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Movies watched in $selectedYear",
                        color = ScreenTextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnnualMetric(
                    modifier = Modifier.weight(1f),
                    value = movieCount.toString(),
                    label = "Movies"
                )
                AnnualMetric(
                    modifier = Modifier.weight(1f),
                    value = yearVisits.size.toString(),
                    label = "Watches"
                )
                AnnualMetric(
                    modifier = Modifier.weight(1f),
                    value = formatWatchHours(watchMinutes),
                    label = "Watch time"
                )
            }
        }
    }
}
"""

        if (source.contains(oldFunction)) {
            source = source.replace(oldFunction, newFunction)
            sourceFile.writeText(source)
        }
    }
}

tasks.matching { it.name == "preBuild" }.configureEach {
    dependsOn(fixTheatreStatisticsSource)
}
