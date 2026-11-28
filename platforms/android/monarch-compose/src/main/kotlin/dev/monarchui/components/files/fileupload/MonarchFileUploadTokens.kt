package dev.monarchui.components.files.fileupload

import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** One checked file: accepted when [code] is `null`; [custom] is the app's own message. */
internal class FileUploadCheck(val file: MonarchPickedFile, val code: MonarchFileRejectionCode?, val custom: String?)

/** Component tokens and rules of MonarchFileUpload, built from primitives and roles only. */
internal object MonarchFileUploadTokens {
    val ZonePadding: Dp = MonarchTokens.Space.space4.dp
    val Gap: Dp = MonarchTokens.Space.space2.dp
    val TextGap: Dp = MonarchTokens.Space.space1.dp

    /** The rules of an `accept` list, trimmed, lower case and without blanks. */
    private fun rules(accept: List<String>): List<String> =
        accept.map { it.trim().lowercase() }.filter { it.isNotEmpty() }

    /**
     * Whether a file matches [accept]: MIME types (`application/pdf`),
     * wildcards (`image/` followed by a star) and extensions (`.csv`). An empty
     * list accepts every file.
     */
    fun matchesAccept(name: String, mimeType: String?, accept: List<String>): Boolean {
        val rules = rules(accept)
        if (rules.isEmpty()) return true
        val lowerName = name.lowercase()
        val type = mimeType.orEmpty().lowercase()
        return rules.any { rule ->
            when {
                rule.startsWith(".") -> lowerName.endsWith(rule)
                rule.endsWith("/*") -> type.startsWith(rule.dropLast(1))
                else -> type == rule
            }
        }
    }

    /**
     * Checks [files] against the limits, in order: the type, then the size,
     * then [validate], then the count. Files over the count are refused after
     * the other checks, so the first valid files are kept. With [multiple]
     * off, only the first file is checked and the others are refused as too many.
     */
    @Suppress("LongParameterList")
    fun check(
        files: List<MonarchPickedFile>,
        accept: List<String>,
        maxSize: Long?,
        maxFiles: Int?,
        currentCount: Int,
        multiple: Boolean,
        validate: ((MonarchPickedFile) -> String?)?,
    ): List<FileUploadCheck> {
        val room = if (maxFiles == null) Int.MAX_VALUE else (maxFiles - currentCount).coerceAtLeast(0)
        var accepted = 0
        return files.mapIndexed { index, file ->
            var custom: String? = null
            val code = when {
                !multiple && index > 0 -> MonarchFileRejectionCode.TooMany
                !matchesAccept(file.name, file.mimeType, accept) -> MonarchFileRejectionCode.InvalidType
                maxSize != null && file.size != null && file.size > maxSize -> MonarchFileRejectionCode.TooLarge
                validate?.invoke(file)?.also { custom = it } != null -> MonarchFileRejectionCode.Custom
                accepted >= room -> MonarchFileRejectionCode.TooMany
                else -> null
            }
            if (code == null) accepted++
            FileUploadCheck(file, code, custom)
        }
    }

    /** The error set on the button: [errorText] while invalid, else the first refusal, else none. */
    fun buttonError(invalid: Boolean, errorText: String?, rejections: List<String>): String? =
        if (invalid) errorText?.takeIf { it.isNotBlank() } else rejections.firstOrNull()

    /** Whether no more files can be added: [maxFiles] are attached already. */
    fun full(maxFiles: Int?, currentCount: Int): Boolean = maxFiles != null && currentCount >= maxFiles

    /** The MIME types the document picker filters by: the MIME rules and the types of known extensions. */
    fun documentTypes(accept: List<String>, typeOfExtension: (String) -> String?): Array<String> {
        val types = rules(accept).mapNotNull { rule ->
            if (rule.startsWith(".")) typeOfExtension(rule.drop(1)) else rule
        }
        val unknownExtension = rules(accept).any { it.startsWith(".") && typeOfExtension(it.drop(1)) == null }
        return if (types.isEmpty() || unknownExtension) arrayOf(ANY_TYPE) else types.distinct().toTypedArray()
    }

    /** What the photo picker shows: images only, videos only, or both, from [accept]. */
    fun mediaType(accept: List<String>): PickVisualMedia.VisualMediaType {
        val rules = rules(accept)
        return when {
            rules.isNotEmpty() && rules.all { it.startsWith("image/") } -> PickVisualMedia.ImageOnly
            rules.isNotEmpty() && rules.all { it.startsWith("video/") } -> PickVisualMedia.VideoOnly
            else -> PickVisualMedia.ImageAndVideo
        }
    }

    /** The zone: a subtle fill with a border, the danger border when invalid, muted when disabled. */
    fun zoneColors(invalid: Boolean, enabled: Boolean, colors: MonarchColors): MonarchSurfaceColors =
        MonarchSurfaceColors(
            container = if (enabled) colors.bgSubtle else colors.bgMuted,
            content = labelColor(enabled, colors),
            border = if (invalid) colors.dangerBorder else colors.borderDefault,
        )

    /** The colour of the label. */
    fun labelColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textDefault else colors.textDisabled

    /** The colour of the hint. */
    fun hintColor(enabled: Boolean, colors: MonarchColors): Color =
        if (enabled) colors.textMuted else colors.textDisabled

    private const val ANY_TYPE = "*/*"
}
