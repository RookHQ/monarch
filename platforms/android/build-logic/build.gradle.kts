plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.plugin.android)
    implementation(libs.plugin.kotlin)
    implementation(libs.plugin.compose.compiler)
    implementation(libs.plugin.detekt)
    implementation(libs.plugin.binary.compatibility.validator)
    implementation(libs.plugin.dokka)
    implementation(libs.plugin.maven.publish)
    implementation(libs.plugin.roborazzi)
}
