plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// One fixed package and one fixed signing key, so a newer build installs over the older one
// (adb install -r) and keeps Device Owner. The app never updates itself.
val buildNo = (System.getenv("GITHUB_RUN_NUMBER") ?: "0").toInt()
val buildNames = listOf(
    "Anchor", "Beacon", "Cedar", "Delta", "Ember", "Falcon", "Granite", "Harbor", "Indigo", "Juniper",
    "Keystone", "Lantern", "Meadow", "Nimbus", "Orchard", "Pebble", "Quartz", "Ridge", "Summit", "Tundra"
)

android {
    namespace = "com.guardian.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.guardian.app"
        minSdk = 29
        targetSdk = 34
        versionCode = maxOf(buildNo, 1)
        versionName = "1.0.$buildNo"
        resValue("string", "app_name", "${buildNames[buildNo % buildNames.size]} $buildNo")
    }

    signingConfigs {
        create("harbor") {
            storeFile = rootProject.file("signing/harbor.jks")
            storePassword = "harborshield"
            keyAlias = "harbor"
            keyPassword = "harborshield"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("harbor")
        }
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
