plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp") version "2.0.20-1.0.25"
}

android {
    namespace = "com.evrenhouse.esteh"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.evrenhouse.esteh"
        minSdk = 26
        targetSdk = 34
        // versionCode: naik monoton dari jumlah commit (paritas kasir-mobile).
        versionCode = (project.findProperty("versionCode") as? String)?.toIntOrNull()
            ?: System.getenv("VERSION_CODE")?.toIntOrNull()
            ?: 1
        // versionName: default di bawah; override via -PversionName / VERSION_NAME.
        // Bump resmi via ./bump-version.sh (patch|minor|major).
        versionName = (project.findProperty("versionName") as? String)
            ?: System.getenv("VERSION_NAME")
            ?: "0.3.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Base URL backend. Default production; override lokal via
        // -PAPI_BASE_URL=http://10.0.2.2:3006 (emulator) sesuai kebutuhan.
        val apiBaseUrl = System.getenv("API_BASE_URL")
            ?: (project.findProperty("API_BASE_URL") as? String)
            ?: "https://esteh.sanxmon.my.id"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    signingConfigs {
        create("release") {
            // Opsional: hanya aktif bila kredensial tersedia (CI release).
            // Lokal: -PESTEH_SIGNING_PASSWORD=... (keystore di app/keystore/release.jks, gitignored).
            // CI: secrets ESTEH_SIGNING_PASSWORD + ESTEH_KEYSTORE_BASE64.
            val pw = System.getenv("ESTEH_SIGNING_PASSWORD")
                ?: (project.findProperty("ESTEH_SIGNING_PASSWORD") as? String)
            if (!pw.isNullOrEmpty()) {
                storeFile = file("keystore/release.jks")
                storePassword = pw
                keyAlias = "esteh"
                keyPassword = pw
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName(
                if ((System.getenv("ESTEH_SIGNING_PASSWORD")
                        ?: (project.findProperty("ESTEH_SIGNING_PASSWORD") as? String)).isNullOrEmpty()
                ) "debug" else "release"
            )
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-svg:2.6.0")

    implementation("androidx.room:room-runtime:2.7.0")
    implementation("androidx.room:room-ktx:2.7.0")
    ksp("androidx.room:room-compiler:2.7.0")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
