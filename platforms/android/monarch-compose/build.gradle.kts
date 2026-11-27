import com.rookhq.monarch.buildlogic.configureMonarchPom

plugins {
    id("monarch.components")
}

android {
    namespace = "com.rookhq.monarch"
}

dependencies {
    api(project(":monarch-tokens"))
    api(platform(libs.compose.bom))
    api(libs.compose.ui)
    api(libs.compose.foundation)
    api(libs.compose.material3)
    // For components, so that adding one never means editing this file. See docs/ADDING-COMPONENTS.md.
    // Adaptive layouts: window size classes, panes and the navigation suite.
    implementation(libs.compose.material3.adaptive)
    implementation(libs.compose.material3.adaptive.layout)
    implementation(libs.compose.material3.adaptive.navigation)
    implementation(libs.compose.material3.adaptive.navigation.suite)
    // BackHandler, and rememberLauncherForActivityResult for the system photo and document pickers.
    implementation(libs.androidx.activity.compose)
    // Called directly by components (for example CodeSnippetTabsSync), so declared, not left transitive.
    implementation(libs.androidx.core.ktx)
    // The system emoji picker behind MonarchEmojiPicker (a View, hosted with AndroidView).
    implementation(libs.androidx.emoji2.emojipicker)
    // Real icons for tests and examples (Material icons, Apache-2.0). Test scope only: the library bundles no icons.
    testImplementation(project(":monarch-sample-icons"))
    // The docs previews are set in Poppins (D10.4): PreviewCaptureTest uses the optional font pack. Tests only.
    testImplementation(project(":monarch-fonts-poppins"))
    // The Studio previews in src/debug use them too. The debug variant is not published.
    debugImplementation(project(":monarch-sample-icons"))
    // @Preview functions live in src/debug, so they are not part of the published library.
    debugImplementation(libs.compose.ui.tooling.preview)

    constraints {
        // Debug only: lint analyses the debug variant, and the published release variant is left alone.
        debugImplementation(libs.androidx.lifecycle.runtime) {
            because("lifecycle-runtime 2.9.4, pulled in by Compose, ships lint checks that AGP 9.5 cannot load")
        }
    }
}

configureMonarchPom(
    displayName = "Monarch Compose",
    summary = "Jetpack Compose theme, foundations and components of the Monarch design system.",
)
