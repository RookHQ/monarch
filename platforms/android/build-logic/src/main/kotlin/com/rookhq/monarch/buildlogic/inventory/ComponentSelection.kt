package com.rookhq.monarch.buildlogic.inventory

import org.gradle.api.GradleException
import org.gradle.api.Project
import java.io.File

/**
 * The components named by `-Pcomponent=<type name, title or id>[,<another>...]`:
 * their inventory entries, test classes and files. `null` when the property is
 * not set. A comma list checks several components in one Gradle run.
 */
class ComponentSelection(
    val query: String,
    val entries: List<InventoryEntry>,
    val testClasses: List<String>,
    val files: List<File>,
) {
    val ids: List<String> get() = entries.map { it.id }
    val typeNames: List<String> get() = entries.mapNotNull { it.name }.distinct()

    companion object {
        /** The Gradle property that selects a component for `componentCheck`. */
        const val PROPERTY = "component"

        fun from(project: Project): ComponentSelection? {
            val query = project.providers.gradleProperty(PROPERTY).orNull?.trim() ?: return null
            val layout = MonarchLayout(project.rootDir)
            // Problems in other inventory files are reported by generateInventory, not here.
            val all = Inventory.load(layout, mutableListOf())
            val names = query.split(',').map(String::trim).filter(String::isNotEmpty)
            if (names.isEmpty()) {
                throw GradleException("-P$PROPERTY needs a type name, title or id (or a comma list of them)")
            }
            val entries = names.flatMap { name ->
                Inventory.resolve(all, name).ifEmpty {
                    throw GradleException(
                        "-P$PROPERTY=$name: no valid inventory entry has that id, type name or title. " +
                            "Check inventory/<id>.json; ./gradlew generateInventory reports what is wrong with it.",
                    )
                }
            }.distinctBy { it.id }
            val fixtures = Sources.fixtures(layout)
            val ids = entries.map { it.id }.toSet()
            return ComponentSelection(
                query = query,
                entries = entries,
                testClasses = fixtures.filter { it.id in ids }.map { it.testClass }.distinct(),
                files = entries.flatMap { Sources.filesOf(it, layout, fixtures) }.distinct(),
            )
        }
    }
}
