import groovy.json.JsonBuilder
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.compose)
    alias(libs.plugins.jetbrains.kotlin.serialization)
}

val projectName = "Karoo-Wprime-Race"
val projectLabel = "W Prime Race"
val projectDescription = "W Prime balance vs target pacing — TT and Crit variants"
val projectDeveloper = "j4m1eb"

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("11")
    }
}

android {
    namespace = "com.j4m1eb.wprimerace"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.j4m1eb.wprimerace"
        minSdk = 23
        targetSdk = 36
        versionCode = 3
        versionName = "1.2"
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH")
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val keystorePath = System.getenv("KEYSTORE_PATH")
            signingConfig = if (keystorePath != null) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
        debug {
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            merges += "META-INF/LICENSE.md"
            merges += "META-INF/LICENSE-notice.md"
        }
    }
}

dependencies {
    implementation(project(":lib"))
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.bundles.compose.ui)
    implementation(libs.bundles.lifecycle)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.preview)
    implementation(libs.androidx.glance.appwidget.preview)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.timber)
}

tasks.register("generateManifest") {
    description = "Generates manifest.json for Karoo companion app auto-update"
    group = "build"

    val manifestOutputFile = layout.projectDirectory.file("manifest.json")
    outputs.file(manifestOutputFile).withPropertyName("manifestFile")

    doLast {
        val androidExtension = project.extensions.getByName("android") as com.android.build.gradle.AppExtension
        val defaultConfig = androidExtension.defaultConfig
        val baseUrl = "https://github.com/$projectDeveloper/$projectName/releases/latest/download"
        val manifest = mapOf(
            "label" to projectLabel,
            "packageName" to androidExtension.namespace,
            "latestApkUrl" to "$baseUrl/app-release.apk",
            "latestVersion" to defaultConfig.versionName,
            "latestVersionCode" to defaultConfig.versionCode,
            "developer" to projectDeveloper,
            "description" to projectDescription,
            "screenshotUrls" to emptyList<String>()
        )
        manifestOutputFile.asFile.writeText(JsonBuilder(manifest).toPrettyString())
    }
}

tasks.named("assemble") {
    dependsOn("generateManifest")
}
