import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.gta.game"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.gta.game"
    minSdk = 28
    targetSdk = 36
    versionCode = 1
    versionName = "1.0.0"
    multiDexEnabled = true

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  packaging {
    jniLibs {
      excludes.add("META-INF/*")
    }
    resources {
      excludes.add("META-INF/*")
    }
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    buildConfig = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

dependencies {
  implementation("androidx.appcompat:appcompat:1.6.1")
  implementation("com.google.android.material:material:1.9.0")
  implementation("androidx.constraintlayout:constraintlayout:2.1.4")
  implementation("androidx.navigation:navigation-fragment:2.7.2")
  implementation("androidx.navigation:navigation-ui:2.7.2")
  implementation("androidx.core:core-ktx:1.12.0")
  implementation("com.google.android.play:asset-delivery:2.3.0")
  implementation("com.android.volley:volley:1.2.1")
  implementation("com.intuit.sdp:sdp-android:1.1.0")
  implementation(platform(libs.firebase.bom))
  implementation("com.google.firebase:firebase-analytics")
  implementation("com.google.firebase:firebase-messaging")
  implementation("com.google.firebase:firebase-config")
  implementation("androidx.datastore:datastore-preferences:1.0.0")
  implementation("androidx.datastore:datastore-core:1.0.0")
  implementation("org.ini4j:ini4j:0.5.4")
  implementation("com.github.bumptech.glide:glide:4.15.1")
  annotationProcessor("com.github.bumptech.glide:compiler:4.15.1")
  implementation("androidx.lifecycle:lifecycle-process:2.6.2")
  implementation("com.bytedance.android:shadowhook:1.0.10")
  implementation("com.github.amitshekhariitbhu:PRDownloader:1.0.2")
  testImplementation("junit:junit:4.13.2")
  testImplementation("org.json:json:20231013")
}
