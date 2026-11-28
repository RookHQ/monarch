package dev.monarchui.components.intelligence.aiartifactcanvas

import androidx.compose.runtime.Immutable
import dev.monarchui.components.intelligence.aiconversation.MonarchAiArtifactKind

/**
 * Replacements for the strings `MonarchAiArtifactCanvas` supplies. Each
 * `null` keeps the built-in English string `monarch_aiartifactcanvas_<key>`.
 *
 * @property label the TalkBack name of the canvas from the title. Default "Artifact: Q3 summary".
 * @property kind the kind name under the title, such as "Document" or "Web page".
 * @property version the version under the title. Default "v3".
 * @property preview the Preview tab.
 * @property code the Code tab.
 * @property close the TalkBack name of the close button, or its text without a close icon. Default "Close artifact".
 * @property copy the copy button.
 * @property copied the copy button after copying.
 * @property download the download button.
 * @property save the save button, such as "Save to library". Default "Save".
 * @property frameTitle the TalkBack name of the web page preview from the title. Default "Preview of Q3 summary".
 * @property sandboxNote shown under the web page preview.
 * @property imageUnavailable shown when an image artifact has no picture.
 * @property noPreview shown when the kind has no preview.
 */
@Immutable
public class MonarchAiArtifactCanvasLabels(
    public val label: ((title: String) -> String)? = null,
    public val kind: ((MonarchAiArtifactKind) -> String)? = null,
    public val version: ((version: Int) -> String)? = null,
    public val preview: String? = null,
    public val code: String? = null,
    public val close: String? = null,
    public val copy: String? = null,
    public val copied: String? = null,
    public val download: String? = null,
    public val save: String? = null,
    public val frameTitle: ((title: String) -> String)? = null,
    public val sandboxNote: String? = null,
    public val imageUnavailable: String? = null,
    public val noPreview: String? = null,
) {
    private val parts: List<Any?>
        get() = listOf(
            label, kind, version, preview, code, close, copy, copied, download, save, frameTitle, sandboxNote,
            imageUnavailable, noPreview,
        )

    override fun equals(other: Any?): Boolean = other is MonarchAiArtifactCanvasLabels && other.parts == parts

    override fun hashCode(): Int = parts.hashCode()
}
