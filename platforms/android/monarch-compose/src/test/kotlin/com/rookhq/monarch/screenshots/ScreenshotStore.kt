package com.rookhq.monarch.screenshots

import java.io.File

/** What a run does with the baselines. */
internal enum class ScreenshotMode { Off, Verify, Record }

/**
 * Where the baselines are and what to do with them, read from system properties that `monarch.components` sets:
 * `monarch.screenshots` (`verify` by default, `record`, or `off`), `monarch.screenshots.dir`,
 * `monarch.screenshots.out` and `monarch.screenshots.ids`.
 *
 * Every baseline is stored in `src/test/screenshots/<id>/<id>.<theme>[.x2][.rtl].png`. The docs previews are a
 * different set (`previews/`, written by `PreviewCaptureTest`): they show the primary example, not every fixture.
 */
internal class ScreenshotStore(
    val mode: ScreenshotMode,
    private val baselines: File,
    val outputs: File,
    val ids: Set<String>?,
) {
    /** The baseline file of one image. */
    fun fileFor(id: String, variant: ScreenshotVariant): File = File(baselines, "$id/$id.${variant.suffix}.png")

    /** The core set: the ids in `core-components.txt`. */
    val core: Set<String> by lazy { readCore(File(baselines, "core-components.txt")) }

    companion object {
        /** The store of the running test, from the system properties. */
        fun fromSystemProperties(): ScreenshotStore {
            val requested = System.getProperty("monarch.screenshots")
            val mode = ScreenshotMode.entries.firstOrNull { it.name.equals(requested, ignoreCase = true) }
                ?: ScreenshotMode.Verify
            val root = File(System.getProperty("monarch.rootDir") ?: ".")
            return ScreenshotStore(
                mode = mode,
                baselines = File(
                    System.getProperty("monarch.screenshots.dir") ?: "$root/monarch-compose/src/test/screenshots",
                ),
                outputs = File(
                    System.getProperty("monarch.screenshots.out") ?: "$root/monarch-compose/build/outputs/screenshots",
                ),
                ids = System.getProperty("monarch.screenshots.ids")?.split(',')?.filter { it.isNotBlank() }?.toSet(),
            )
        }

        /** The ids of a core list file: one per line, `#` starts a comment. */
        fun readCore(file: File): Set<String> =
            file.readLines().map { it.substringBefore('#').trim() }.filter { it.isNotEmpty() }.toSet()
    }
}
