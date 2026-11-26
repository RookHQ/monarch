package com.rookhq.monarch.testing

/**
 * The component selected by `./gradlew componentCheck -Pcomponent=…`, read
 * from system properties that the build sets. `null` means every component.
 */
internal object SelectedComponent {
    /** The selected inventory ids. */
    val ids: Set<String>? = list("monarch.component.ids")

    /** The Kotlin type names of the selected entries. */
    val names: Set<String>? = list("monarch.component.names")

    private fun list(property: String): Set<String>? =
        System.getProperty(property)?.split(',')?.filter { it.isNotBlank() }?.toSet()
}
