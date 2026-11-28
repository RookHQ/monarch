import dev.monarchui.buildlogic.configureMonarchPom

plugins {
    id("monarch.kotlin.library")
}

configureMonarchPom(
    displayName = "Monarch Tokens",
    summary = "Design tokens of the Monarch design system as Kotlin constants. No Android or Compose dependency.",
)
