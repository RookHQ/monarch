// Sample and test icons for Monarch for Android: the Material icons (Apache-2.0) that examples, previews,
// tests and the sample app draw. Never a dependency of monarch-compose and never published: the library
// bundles no icons (policy in the visual spec, icons.md).
plugins {
    id("monarch.android.library")
}

android {
    namespace = "dev.monarchui.sampleicons"
}

dependencies {
    api(platform(libs.compose.bom))
    api(libs.compose.ui)
    api(libs.compose.material.icons.extended)
}
