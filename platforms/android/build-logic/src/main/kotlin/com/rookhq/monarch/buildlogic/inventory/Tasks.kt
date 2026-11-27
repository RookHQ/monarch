package com.rookhq.monarch.buildlogic.inventory

import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/** Fails the task with every collected problem, one per line. */
private fun failOn(errors: List<String>, task: String) {
    if (errors.isNotEmpty()) {
        throw GradleException("$task: ${errors.size} problem(s)\n" + errors.joinToString("\n") { "  $it" })
    }
}

/** Base of the tasks that read the per-component files of `platforms/android`. */
abstract class MonarchSourcesTask : DefaultTask() {
    /** `platforms/android`. */
    @get:Internal
    abstract val monarchRoot: DirectoryProperty

    /** The files the task reads, so Gradle knows when to run it again. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:Internal
    protected val layout: MonarchLayout get() = MonarchLayout(monarchRoot.get().asFile)
}

/**
 * Validates every inventory file and the conventions, then writes
 * components.json. With [check] it writes nothing and fails when
 * components.json is not what the inventory files produce.
 */
@DisableCachingByDefault(because = "Writes a file in the source tree")
abstract class GenerateInventory : MonarchSourcesTask() {
    @get:Input
    abstract val check: Property<Boolean>

    @TaskAction
    fun generate() {
        val errors = mutableListOf<String>()
        val entries = Inventory.load(layout, errors)
        Conventions.check(entries, Sources.fixtures(layout), Sources.pages(layout, errors), layout, errors)
        failOn(errors, name)
        val content = Generators.componentsJson(entries)
        val target = layout.componentsJson
        val current = if (target.isFile) target.readText() else null
        val examples = entries.sumOf { it.examples.size }
        when {
            current == content -> logger.lifecycle("$name: ${entries.size} entries, $examples examples; components.json is up to date")
            check.get() -> throw GradleException("$name: components.json is stale. Run ./gradlew generateInventory")
            else -> {
                target.writeText(content)
                logger.lifecycle("$name: ${entries.size} entries, $examples examples; components.json written")
            }
        }
    }
}

/**
 * Writes the test sources that list per-component files: one Kotlin file of
 * compiled examples per inventory entry, and the fixture registry.
 */
@DisableCachingByDefault(because = "Fast, and the output depends on absolute source layout")
abstract class GenerateComponentTestSources : MonarchSourcesTask() {
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val errors = mutableListOf<String>()
        val entries = Inventory.load(layout, errors)
        failOn(errors, name)
        val files = Generators.exampleFiles(entries, Generators.monarchImports(layout)) +
            ("com/rookhq/monarch/generated/AllComponentFixtures.kt" to Generators.fixtureRegistry(Sources.fixtures(layout)))
        Generators.sync(outputDir.get().asFile, files)
    }
}

/** Writes the list of families and pages of the sample catalogue. */
@DisableCachingByDefault(because = "Fast, and the output depends on absolute source layout")
abstract class GenerateCatalogueRegistry : MonarchSourcesTask() {
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val errors = mutableListOf<String>()
        val pages = Sources.pages(layout, errors)
        failOn(errors, name)
        Generators.sync(
            outputDir.get().asFile,
            mapOf("com/rookhq/monarch/sample/generated/CatalogueFamilies.kt" to Generators.catalogueRegistry(pages)),
        )
    }
}

/** Compares the inventory with `platforms/parity.json`. Reports only; it never fails. */
@DisableCachingByDefault(because = "Prints a report")
abstract class ParityReport : MonarchSourcesTask() {
    @TaskAction
    fun report() {
        val errors = mutableListOf<String>()
        val entries = Inventory.load(layout, errors)
        failOn(errors, name)
        if (!layout.parity.isFile) {
            logger.lifecycle("parity: ${layout.parity} not found; nothing to compare")
            return
        }
        val plan = ((JsonSlurper().parse(layout.parity) as Map<*, *>)["components"] as List<*>)
            .associate { item -> (item as Map<*, *>)["id"] as String to item }
        val have = entries.associateBy { it.id }
        val common = plan.keys.intersect(have.keys).sorted()
        logger.lifecycle("parity: ${common.size} of ${plan.size} planned ids have an inventory file")
        val planned = plan.values.groupingBy { (it["android"] as Map<*, *>)["coverage"] }.eachCount()
        val done = common.groupingBy { have.getValue(it).coverage }.eachCount()
        planned.toSortedMap(compareBy { it.toString() }).forEach { (coverage, count) ->
            logger.lifecycle("parity:   $coverage: ${done[coverage] ?: 0} of $count")
        }
        (have.keys - plan.keys).sorted().forEach {
            logger.lifecycle("parity: $it is not in parity.json (family ${have.getValue(it).family})")
        }
        common.forEach { id ->
            val entry = have.getValue(id)
            val item = plan.getValue(id)
            val android = item["android"] as Map<*, *>
            if (android["coverage"] != entry.coverage) {
                logger.lifecycle("parity: $id: coverage is ${entry.coverage} in the inventory and ${android["coverage"]} in parity.json")
            }
            if (android["name"] != entry.name) {
                logger.lifecycle("parity: $id: name is ${entry.name} in the inventory and ${android["name"]} in parity.json")
            }
            if (item["family"] != entry.family) {
                logger.lifecycle("parity: $id: family is ${entry.family} in the inventory and ${item["family"]} in parity.json")
            }
        }
    }
}
