import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import dev.monarchui.buildlogic.inventory.GenerateCatalogueRegistry

// The sample catalogue: its list of families and pages is generated from the
// section files, so adding a page only adds a file.
plugins {
    id("monarch.android.application")
}

val generateCatalogueRegistry = tasks.register<GenerateCatalogueRegistry>("generateCatalogueRegistry") {
    group = "monarch"
    description = "Writes the list of catalogue families and pages from sections/<family>/*Section.kt."
    monarchRoot.set(rootProject.layout.projectDirectory)
    sources.from(fileTree("src/main/kotlin") { include("**/sections/**/*.kt") })
    outputDir.set(layout.buildDirectory.dir("generated/monarch/main/kotlin"))
}

extensions.configure<ApplicationAndroidComponentsExtension> {
    onVariants { variant ->
        variant.sources.kotlin?.addGeneratedSourceDirectory(
            generateCatalogueRegistry,
            GenerateCatalogueRegistry::outputDir,
        )
    }
}
