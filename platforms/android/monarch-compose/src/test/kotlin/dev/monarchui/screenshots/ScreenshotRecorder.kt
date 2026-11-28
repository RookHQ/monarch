@file:OptIn(ExperimentalRoborazziApi::class)

package dev.monarchui.screenshots

import android.graphics.Bitmap
import com.dropbox.differ.SimpleImageComparator
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.JvmImageIoFormat
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.ThresholdValidator
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File

/**
 * Records or compares one image at a time and keeps the failures, so a run reports every changed image and not
 * only the first. A failed compare writes the actual and the diff image under [ScreenshotStore.outputs] and the
 * failure message names their paths.
 *
 * The tolerance is small: a pixel counts as changed when its colour distance is above [PixelDistance], and an
 * image fails when more than [ChangedShare] of its pixels changed. Same-machine runs are exact (the second run
 * must have no diff); the tolerance is only there for rounding of the text renderer.
 */
internal class ScreenshotRecorder(private val store: ScreenshotStore) {
    private val failures = mutableListOf<String>()

    private val options = RoborazziOptions(
        taskType = if (store.mode == ScreenshotMode.Record) RoborazziTaskType.Record else RoborazziTaskType.Verify,
        recordOptions = RoborazziOptions.RecordOptions(
            imageIoFormat = JvmImageIoFormat(
                awtImageWriter = { file, _, image -> ScreenshotPixels.writeIndexed(file, image) },
            ),
        ),
        compareOptions = RoborazziOptions.CompareOptions(
            outputDirectoryPath = store.outputs.path,
            resultValidator = ThresholdValidator(ChangedShare),
            imageComparator = SimpleImageComparator(maxDistance = PixelDistance),
        ),
    )

    /** Records [image] to [file], or compares it with the file. */
    fun check(file: File, image: Bitmap) {
        if (store.mode == ScreenshotMode.Verify && !file.exists()) {
            failures += "${file.path}: no baseline. Record it with ./gradlew recordScreenshots"
            return
        }
        file.parentFile?.mkdirs()
        try {
            ScreenshotPixels.reduce(image).captureRoboImage(file, options)
        } catch (error: AssertionError) {
            failures += "${file.path}: ${error.message?.lineSequence()?.firstOrNull()}\n" +
                "    actual: ${File(store.outputs, "${file.nameWithoutExtension}_actual.png").path}\n" +
                "    diff:   ${File(store.outputs, "${file.nameWithoutExtension}_compare.png").path}"
        }
    }

    /** Fails with every failure of this run. */
    fun assertNoFailures() {
        if (failures.isNotEmpty()) {
            val head = "${failures.size} screenshot(s) differ from their baseline:"
            throw AssertionError(head + "\n" + failures.joinToString("\n"))
        }
    }

    private companion object {
        const val PixelDistance = 0.02F
        const val ChangedShare = 0.001F
    }
}
