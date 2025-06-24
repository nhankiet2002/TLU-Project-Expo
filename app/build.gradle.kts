plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.cse441.tluprojectexpo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cse441.tluprojectexpo"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    sourceSets {
        getByName("main") {
            assets {
                srcDirs("src\\main\\assets", "src\\main\\assets")
            }
        }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.9.0")

    // Firebase BoM (Bill of Materials)
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))

    // Firebase modules
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.android.gms:play-services-tasks:18.2.0")

    // Material Design
    implementation("com.google.android.material:material:1.10.0")

    // Cloudinary (for image upload)
    implementation("com.cloudinary:cloudinary-android:2.4.0")

    // Glide (image loading)
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Flexbox (layout)
    implementation("com.google.android.flexbox:flexbox:3.0.0")

    // CircleImageView (rounded images)
    implementation("de.hdodenhof:circleimageview:3.1.0")


    // Thêm Gson
    implementation ("com.google.code.gson:gson:2.10.1")
    // Other libraries (from libs.versions.toml)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.firestore)
    implementation(libs.navigation.fragment)
    implementation(libs.swiperefreshlayout)

    implementation(libs.firebase.common)


    implementation(libs.annotation)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)

    implementation(libs.annotation)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
}
dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.messaging)
}
