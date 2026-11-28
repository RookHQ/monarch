plugins {
    id("monarch.catalogue")
}

android {
    namespace = "dev.monarchui.sample"

    defaultConfig {
        applicationId = "dev.monarchui.sample"
    }
}

dependencies {
    implementation(project(":monarch-compose"))
    // Real icons for the catalogue pages (Material icons, Apache-2.0).
    implementation(project(":monarch-sample-icons"))
    // The catalogue can show the components in Poppins as well as the system font.
    implementation(project(":monarch-fonts-poppins"))
    // The adaptive artifacts are implementation dependencies of monarch-compose, so an app does not
    // see them through it. The catalogue declares them like an app would, so its pages can show
    // the real window size class and pane APIs next to the Monarch components.
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3.adaptive)
    implementation(libs.compose.material3.adaptive.layout)
    implementation(libs.compose.material3.adaptive.navigation)
    implementation(libs.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.ui.tooling.preview)

    constraints {
        implementation(libs.androidx.lifecycle.runtime) {
            because("lifecycle-runtime 2.9.4, pulled in by Compose, ships lint checks that AGP 9.5 cannot load")
        }
    }
}
