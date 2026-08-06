plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.arroom.characters"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.arroom.characters"
        // SceneView 2.x + Filament требуют минимум API 28
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        // ARCore работает только на ARM
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }

        // Ставим только те локали, которые реально переведены
        resourceConfigurations += listOf("en", "ru")
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
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }

    lint {
        // Отчёт читаем в артефактах CI, но сборку не роняем:
        // предупреждения Compose-линта часто ложные
        abortOnError = false
        warningsAsErrors = false
        htmlReport = true
    }

    // .glb нельзя сжимать — Filament читает их через mmap
    androidResources {
        noCompress += listOf("glb", "gltf", "bin", "ktx", "filamat")
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
}

dependencies {
    // --- AR ядро ---
    implementation("io.github.sceneview:arsceneview:2.2.1")

    // --- Compose ---
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // --- Прочее ---
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // --- Тесты ---
    testImplementation("junit:junit:4.13.2")
}
