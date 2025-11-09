plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.hilt.android) // Plugin bạn đã thêm (ĐÚNG)
}

android {
    namespace = "com.AnhQuoc.studentmanagementapp"
    compileSdk = 36 // Bạn có thể đổi thành 34 nếu chưa tải SDK 36

    defaultConfig {
        applicationId = "com.AnhQuoc.studentmanagementapp"
        minSdk = 24
        targetSdk = 36 // Bạn có thể đổi thành 34 nếu chưa tải SDK 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =

            "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Thêm dòng này để nhập Firebase BOM (Bill of Materials)
    // SỬA LẠI: Bỏ 1 dòng trùng lặp
    implementation(platform("com.google.firebase:firebase-bom:33.1.1"))

    implementation("androidx.lifecycle:lifecycle-viewmodel:2.6.2")

    implementation("androidx.lifecycle:lifecycle-livedata:2.6.2")


    annotationProcessor("androidx.lifecycle:lifecycle-compiler:2.6.2")

    // === SỬA LỖI: THÊM 2 DÒNG HILT VÀO ĐÂY ===
    implementation(libs.hilt.android)
    // Vì dự án của bạn là Java, chúng ta dùng annotationProcessor
    annotationProcessor(libs.hilt.compiler)
    // ------------------------------------

    // SỬA LẠI: Bỏ 1 dòng trùng lặp
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation(libs.appcompat)

    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.google.firebase:firebase-storage")
}