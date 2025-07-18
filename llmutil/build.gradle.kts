plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.hs16542.dildogent.llmutil"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        targetSdk = 35

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    
    buildFeatures {
        dataBinding = true
        viewBinding = true
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    
    // 视频播放和媒体处理
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")
    implementation("androidx.media3:media3-common:1.2.1")
    
    // 音频处理
    implementation("com.google.android.exoplayer:exoplayer-core:2.19.1")
    implementation("com.google.android.exoplayer:exoplayer-ui:2.19.1")
    
    // 网络请求库
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    
    // JSON解析
    implementation("com.google.code.gson:gson:2.10.1")
    
    // 协程支持
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // 语音识别 (可选，用于本地语音识别)
    implementation("com.google.cloud:google-cloud-speech:4.8.0")
    
    // 文件处理
    implementation("androidx.documentfile:documentfile:1.0.1")
    
    // 权限处理
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    
    // Material Design
    implementation("com.google.android.material:material:1.11.0")
    
    // 离线模型支持
    // TensorFlow Lite - 使用更稳定的版本
    implementation("org.tensorflow:tensorflow-lite:2.13.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.4.4")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.13.0")
    // GPU委托插件在某些版本中可能不可用，使用可选依赖
    // implementation("org.tensorflow:tensorflow-lite-gpu-delegate-plugin:2.13.0")
    
    // ONNX Runtime - 使用稳定版本
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.15.1")
    
    // Hugging Face Transformers (通过ONNX) - 可选，可能不可用
    // implementation("com.microsoft.onnxruntime:onnxruntime-extensions-android:1.15.1")
    
    // 文本处理 - 使用可用的库
    // implementation("com.hankcs:hanlp:portable-1.8.4")
    // implementation("org.lucee:jieba-analysis:1.0.3")
    
    // 模型文件管理
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // 测试依赖
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    
    // 单元测试依赖
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.2.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.robolectric:robolectric:4.10.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
} 