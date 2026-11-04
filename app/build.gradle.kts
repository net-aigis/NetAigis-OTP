import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

// APK basename: free → otp-free-{debug|release}.apk; pro (default) → otp-{debug|release}.apk
base {
    archivesName.set("otp")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
val requestedReleaseBuild = gradle.startParameter.taskNames.any {
    it.contains("Release", ignoreCase = true)
}

if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
} else if (requestedReleaseBuild) {
    throw GradleException(
        "keystore.properties fehlt. Fuer Release-Builds releaseStoreFile, releaseStorePassword, releaseKeyAlias und releaseKeyPassword lokal setzen."
    )
}

android {
    namespace = "de.wittmer.app.otp"
    testNamespace = "de.wittmer.app.otp.uitest"
    compileSdk = rootProject.extra["compileSdkVersion"] as Int

    defaultConfig {
        testInstrumentationRunnerArguments += mapOf("clearPackageData" to "true")
        versionCode = rootProject.extra["versionCode"] as Int
        versionName = rootProject.extra["versionName"] as String
        minSdk = rootProject.extra["minSdkVersion"] as Int
        targetSdk = rootProject.extra["targetSdkVersion"] as Int
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "edition"
    productFlavors {
        create("free") {
            dimension = "edition"
            applicationId = "de.wittmer.app.otp.free"
        }
        create("pro") {
            dimension = "edition"
            applicationId = "de.wittmer.app.otp"
            // Default when tasks omit the flavor (e.g. assembleDebug).
            isDefault = true
        }
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = file(keystoreProperties["releaseStoreFile"] as String)
                storePassword = keystoreProperties["releaseStorePassword"] as String
                keyAlias = keystoreProperties["releaseKeyAlias"] as String
                keyPassword = keystoreProperties["releaseKeyPassword"] as String
            }
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
        getByName("release") {
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
        animationsDisabled = true
    }

    applicationVariants.configureEach {
        val apkName = if (flavorName == "pro") {
            "otp-${buildType.name}.apk"
        } else {
            "otp-${flavorName}-${buildType.name}.apk"
        }
        outputs.configureEach {
            (this as com.android.build.gradle.api.ApkVariantOutput).outputFileName = apkName
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugarJdkLibs)
    implementation(project(":token-images"))
    implementation(project(":text-drawable"))
    implementation(project(":token-data"))
    implementation(libs.appCompat)
    implementation(libs.material)
    implementation(libs.zxing)
    implementation(libs.gson)
    implementation(libs.glide)
    implementation(libs.coreKtx)
    implementation(libs.activityKtx)
    implementation(libs.bundles.kotlinxCoroutines)

    implementation(libs.bundles.cameraX)
    implementation(libs.biometric)
    implementation(libs.guava)

    implementation(libs.hiltAndroid)
    ksp(libs.hiltAndroidCompiler)

    androidTestImplementation(libs.bundles.androidxTest)
    androidTestUtil(libs.androidxTestOrchestrator)

    androidTestImplementation(libs.bundles.androidxJunit)
    androidTestImplementation(libs.androidxTruth)
    androidTestImplementation(libs.bundles.espresso)
}
