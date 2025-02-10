plugins {
    alias(libs.plugins.androidLibrary)
    id("maven-publish")
}

android {
    namespace = "com.upitranzact.sdk"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    
    // Retrofit core library
    implementation("com.squareup.retrofit2:retrofit:2.9.0")

    // Converter for JSON (Gson)
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Logging interceptor (for debugging API calls)
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")

    implementation("com.makeramen:roundedimageview:2.3.0")
    implementation("com.airbnb.android:lottie:6.1.0")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.github.upitranzact"
            artifactId = "sdk"
            version = "1.0.0"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}