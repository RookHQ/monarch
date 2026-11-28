import dev.monarchui.buildlogic.configureMonarchPom

// Optional Poppins typeface for Monarch. A separate artifact so an app that keeps the system font
// pays no size cost. The font files are SIL Open Font License 1.1 (OFL.txt).
plugins {
    id("monarch.android.library")
}

android {
    namespace = "dev.monarchui.fonts.poppins"
    lint {
        // The font files are used through R.font; the licence text is shipped for the OFL, not read by code.
        disable += "UnusedResources"
    }
}

dependencies {
    api(project(":monarch-compose"))
}

configureMonarchPom(
    displayName = "Monarch Fonts Poppins",
    summary = "Optional Poppins typeface (regular, medium, semibold; SIL OFL 1.1) for Monarch for Android.",
)
