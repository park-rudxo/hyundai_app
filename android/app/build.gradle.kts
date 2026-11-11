import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}
val localConfig = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val naverMapKey = (localConfig.getProperty("NAVER_MAP_CLIENT_ID") ?: System.getenv("NAVER_MAP_CLIENT_ID") ?: "").trim()
require(naverMapKey.matches(Regex("[A-Za-z0-9_-]*"))) { "Invalid NAVER_MAP_CLIENT_ID format" }
// Gradle's Windows test worker can lose non-ASCII classpath entries.
// Keep source in the workspace and optionally put generated output in an ASCII path.
providers.gradleProperty("buildRoot").orNull?.let { layout.buildDirectory.set(file("$it/app")) }
android {
    namespace = "io.changyeoga.app"
    compileSdk = 35
    useLibrary("android.car", false)
    defaultConfig {
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
        applicationId = "io.changyeoga.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 9
        versionName = "0.6.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["naverMapKey"] = naverMapKey
        buildConfigField("boolean", "HAS_NAVER_MAP_KEY", naverMapKey.isNotEmpty().toString())
    }
    flavorDimensions += "surface"
    productFlavors {
        create("phone") { dimension = "surface"; applicationIdSuffix = ".phone"; buildConfigField("boolean", "IS_VEHICLE", "false"); buildConfigField("boolean", "IS_DEMO", "false"); resValue("string", "app_name", "챙겨가 준비") }
        create("vehicle") { dimension = "surface"; buildConfigField("boolean", "IS_VEHICLE", "true"); buildConfigField("boolean", "IS_DEMO", "false"); resValue("string", "app_name", "챙겨가") }
        create("demo") { dimension = "surface"; applicationIdSuffix = ".demo"; buildConfigField("boolean", "IS_VEHICLE", "true"); buildConfigField("boolean", "IS_DEMO", "true"); resValue("string", "app_name", "챙겨가 시연") }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    testOptions { unitTests.all { it.jvmArgs("-Dfile.encoding=UTF-8") } }
}
dependencies {
    add("vehicleImplementation", "ai.pleos.playground:NaviHelper:3.0.5")
    add("phoneImplementation", "com.naver.maps:map-sdk:3.24.0")
    // FusedLocationSource references Play Services at runtime; map-sdk does not bundle it.
    add("phoneImplementation", "com.google.android.gms:play-services-location:21.0.1")
    add("phoneImplementation", "androidx.fragment:fragment-ktx:1.8.6")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}
