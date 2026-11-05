plugins {
    alias(libs.plugins.corntime.android.library.compose)
}

android {
    resourcePrefix = ""
}

dependencies {
    api(projects.core.domain)
    api(projects.core.presentation)

    api(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.ui.tooling.preview)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.animation)
    api(libs.androidx.constraintlayout.compose)
    api(libs.androidx.compose.material.icons.extended)

    api(libs.androidx.lifecycle.runtime.compose)
    api(libs.androidx.lifecycle.viewmodel.compose)

    // Images
    api(libs.coil.compose)
    api(libs.coil.network.okhttp)

    // Effects & animation
    api(libs.shimmer)
    api(libs.orbital)
    api(libs.haze)
    api(libs.haze.blur)
    api(libs.liquid)
    api(libs.lottie.compose)

    api(libs.kotlinx.collections.immutable)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
