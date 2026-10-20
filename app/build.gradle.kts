import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.pocketexpense"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.pocketexpense"
        minSdk = 24
        targetSdk = 37
        versionCode = 5
        versionName = "1.0.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val localProps = Properties().apply {
                val propFile = rootProject.file("local.properties")
                if (propFile.exists()) {
                    propFile.inputStream().use { load(it) }
                }
            }

            val keystorePath = System.getenv("POCKETEXPENSE_RELEASE_STORE_FILE")
                ?: (project.findProperty("POCKETEXPENSE_RELEASE_STORE_FILE") as? String)
                ?: localProps.getProperty("POCKETEXPENSE_RELEASE_STORE_FILE")

            val keystorePassword = System.getenv("POCKETEXPENSE_RELEASE_STORE_PASSWORD")
                ?: (project.findProperty("POCKETEXPENSE_RELEASE_STORE_PASSWORD") as? String)
                ?: localProps.getProperty("POCKETEXPENSE_RELEASE_STORE_PASSWORD")

            val keyAliasStr = System.getenv("POCKETEXPENSE_RELEASE_KEY_ALIAS")
                ?: (project.findProperty("POCKETEXPENSE_RELEASE_KEY_ALIAS") as? String)
                ?: localProps.getProperty("POCKETEXPENSE_RELEASE_KEY_ALIAS")

            val keyPasswordStr = System.getenv("POCKETEXPENSE_RELEASE_KEY_PASSWORD")
                ?: (project.findProperty("POCKETEXPENSE_RELEASE_KEY_PASSWORD") as? String)
                ?: localProps.getProperty("POCKETEXPENSE_RELEASE_KEY_PASSWORD")

            val missingVars = mutableListOf<String>()
            if (keystorePath.isNullOrBlank()) missingVars.add("POCKETEXPENSE_RELEASE_STORE_FILE")
            if (keystorePassword.isNullOrBlank()) missingVars.add("POCKETEXPENSE_RELEASE_STORE_PASSWORD")
            if (keyAliasStr.isNullOrBlank()) missingVars.add("POCKETEXPENSE_RELEASE_KEY_ALIAS")
            if (keyPasswordStr.isNullOrBlank()) missingVars.add("POCKETEXPENSE_RELEASE_KEY_PASSWORD")

            val isReleaseRequested = gradle.startParameter.taskNames.any { name ->
                name.contains("Release", ignoreCase = true) || name == "assemble" || name == "bundle" || name == "build"
            }

            if (missingVars.isNotEmpty()) {
                if (isReleaseRequested) {
                    throw GradleException(
                        "Release build signing failed: Missing required signing credential environment variable(s): ${missingVars.joinToString(", ")}. " +
                        "A release build must fail if any release signing credential is missing. Debug signing fallback has been removed."
                    )
                }
            } else {
                val store = file(keystorePath)
                if (!store.exists()) {
                    throw GradleException("Release build signing failed: Keystore file does not exist at: ${store.absolutePath}")
                }
                storeFile = store
                storePassword = keystorePassword
                keyAlias = keyAliasStr
                keyPassword = keyPasswordStr
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
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
    implementation(libs.androidx.compose.material.icons.extended)

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

ksp {
    arg("room.generateKotlin", "true")
}