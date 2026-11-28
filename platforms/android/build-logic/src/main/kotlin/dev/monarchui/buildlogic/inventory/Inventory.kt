package dev.monarchui.buildlogic.inventory

import groovy.json.JsonException
import groovy.json.JsonSlurper
import java.io.File

/** One example of an inventory entry. */
class InventoryExample(val title: String, val code: String, val setup: String?, val imports: List<String>)

/** One inventory file, `inventory/<id>.json`, after schema validation. */
class InventoryEntry(val raw: Map<String, Any?>) {
    val id: String get() = raw["id"] as String
    val name: String? get() = raw["name"] as String?
    val title: String get() = raw["title"] as String
    val family: String get() = raw["family"] as String
    val category: String get() = raw["category"] as String
    val coverage: String get() = raw["coverage"] as String
    val status: String get() = raw["status"] as String

    /** The documentation layer (ADR 0017): `components` or `patterns`. */
    val layer: String get() = raw["layer"] as String
    val kind: String get() = raw["kind"] as String
    val wrapsNative: String? get() = raw["wrapsNative"] as String?
    val tests: List<String> get() = (raw["tests"] as List<*>).map { it.toString() }

    val examples: List<InventoryExample>
        get() = (raw["examples"] as List<*>).map { item ->
            val example = item as Map<*, *>
            InventoryExample(
                title = example["title"] as String,
                code = example["code"] as String,
                setup = example["setup"] as String?,
                imports = (example["imports"] as List<*>?).orEmpty().map { it.toString() },
            )
        }

    /** Whether Monarch ships Kotlin for this entry, as opposed to documentation only. */
    val shipsCode: Boolean get() = coverage == "build" || coverage == "style"

    /** Whether [query] is this entry's id, type name or title, ignoring case. */
    fun answersTo(query: String): Boolean =
        listOf(id, name.orEmpty(), title).any { it.equals(query, ignoreCase = true) }
}

/** Reads and validates the inventory. Problems are collected, so one run reports all of them. */
object Inventory {
    /** The order of fields in components.json: the order of `properties` in the schema. */
    private fun fieldOrder(schema: Map<*, *>): List<String> =
        (schema["properties"] as Map<*, *>).keys.map { it.toString() }

    fun load(layout: MonarchLayout, errors: MutableList<String>): List<InventoryEntry> {
        if (!layout.schema.isFile) {
            errors += "inventory/schema.json is missing"
            return emptyList()
        }
        val schema = JsonSlurper().parse(layout.schema) as Map<*, *>
        val order = fieldOrder(schema)
        val files = layout.inventory.listFiles { file -> file.extension == "json" && file.name != "schema.json" }
        return files.orEmpty().sortedBy { it.name }.mapNotNull { file -> read(file, schema, order, errors) }
    }

    private fun read(file: File, schema: Map<*, *>, order: List<String>, errors: MutableList<String>): InventoryEntry? {
        val where = "inventory/${file.name}"
        val parsed = try {
            JsonSlurper().parse(file)
        } catch (problem: JsonException) {
            errors += "$where: not valid JSON: ${problem.message}"
            return null
        }
        val before = errors.size
        Schema.validate(parsed, schema, where, errors)
        if (errors.size > before) return null
        val fields = parsed as Map<*, *>
        val entry = InventoryEntry(order.filter { fields.containsKey(it) }.associateWith { fields[it] })
        if ("${entry.id}.json" != file.name) errors += "$where: id \"${entry.id}\" must equal the file name"
        return entry
    }

    /** The entries that answer to an id, a type name or a title. */
    fun resolve(entries: List<InventoryEntry>, query: String): List<InventoryEntry> =
        entries.filter { it.answersTo(query) }
}
