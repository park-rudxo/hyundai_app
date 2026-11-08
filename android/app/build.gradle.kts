plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}
// Gradle's Windows test worker can lose non-ASCII classpath entries.
// Keep source in the workspace and optionally put generated output in an ASCII path.
providers.gradleProperty("buildRoot").orNull?.let { layout.buildDirectory.set(file("$it/app")) }
android {
    namespace = "io.changyeoga.app"
    compileSdk = 35
    useLibrary("android.car", false)
    defaultConfig {
        applicationId = "io.changyeoga.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "0.4.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
