import io.gitlab.arturbosch.detekt.Detekt

// Static analysis shared by every module. The rules are in config/detekt/detekt.yml.
// There is no baseline file: a finding fails the build until it is fixed.
plugins {
    id("io.gitlab.arturbosch.detekt")
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    source.setFrom("src/main/kotlin", "src/debug/kotlin", "src/test/kotlin", "src/testDebug/kotlin")
    parallel = false
}

tasks.withType<Detekt>().configureEach {
    // MonarchTokens.kt is written by packages/tokens/scripts/emit-native.ts.
    exclude("**/tokens/MonarchTokens.kt")
    // Written by scripts/native/glyphs.mjs and scripts/native/fixtures.mjs (long path data and copy).
    exclude("**/internal/MonarchGlyphs.kt")
    exclude("**/fixtures/ParityFixtures.kt")
    reports {
        html.required.set(true)
        xml.required.set(false)
        txt.required.set(false)
        sarif.required.set(false)
        md.required.set(false)
    }
}
