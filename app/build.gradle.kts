plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
//    id "kotlin-serialization' // Make sure this is included


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
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
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
    implementation(libs.play.services.dtdi)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation ("androidx.compose.material:material-icons-extended:1.5.0")

    implementation ("androidx.work:work-runtime-ktx:2.7.1")


    // DataStore
    implementation ("androidx.datastore:datastore-preferences:1.0.0")
    implementation ("androidx.datastore:datastore-core:1.0.0")

    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2") // Version before 1.6.0

    implementation ("androidx.work:work-runtime-ktx:2.7.1")




    implementation ("androidx.navigation:navigation-compose:2.8.3")

    implementation (libs.kotlinx.serialization.json)
//    implementation ("com.github.bumptech.glide:glide:4.15.1")

    implementation(libs.timber)
//    implementation (libs.mpandroidchart)
    //proxy
    // Netty dependencies
//    implementation ("io.netty:netty-all:4.1.95.Final")
//    implementation ("io.netty:netty-all:4.1.68.Final")
    // Netty dependencies
    implementation ("io.netty:netty-handler:4.1.68.Final")
    implementation ("io.netty:netty-codec-socks:4.1.68.Final")
    implementation ("io.netty:netty-transport:4.1.68.Final")
    implementation ("io.netty:netty-transport-native-epoll:4.1.68.Final")



//    implementation(libs.charts)







//    implementation ("androidx.core:core-ktx:1.12.0")
//    implementation ("androidx.activity:activity-compose:1.7.2")
//    implementation ("androidx.compose.ui:ui:1.5.0")
//    implementation ("androidx.compose.material:material:1.5.0")
//    implementation ("androidx.compose.ui:ui-tooling-preview:1.5.0")
//    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
//    implementation ("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
}