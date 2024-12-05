plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

}

android {
    namespace = "com.example.wifip2photspot"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.wifip2photspot"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            resources.merges.add("META-INF/DEPENDENCIES")
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.datastore.preferences.core.jvm)
    implementation(libs.androidx.espresso.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation ("androidx.compose.material:material-icons-extended:1.5.0")

//    implementation ("androidx.work:work-runtime-ktx:2.7.1")


    // DataStore
    implementation ("androidx.datastore:datastore-preferences:1.0.0")
    implementation ("androidx.datastore:datastore-core:1.0.0")

//    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
//
//    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2") // Version before 1.6.0
//
//    implementation ("androidx.work:work-runtime-ktx:2.7.1")

    implementation ("androidx.core:core-ktx:1.10.1")
    implementation ("androidx.navigation:navigation-compose:2.8.3")

    implementation (libs.jsch)
//
//    implementation ("org.apache.sshd:sshd-core:2.9.0")
//    implementation(libs.charts)
//    implementation 'com.jcraft:jsch:0.1.55'



//    implementation ("com.github.peerlab:socks5:1.0.0")
    implementation (libs.kotlinx.serialization.json)
//    implementation ("com.github.bumptech.glide:glide:4.15.1")

    implementation(libs.timber)

    implementation (libs.okhttp)
    implementation (libs.nanohttpd)
//    implementation (libs.littleproxy)
//    implementation ("org.littleshoot:littleproxy:2.0.0-beta6")
//    implementation (libs.netty.all)




}