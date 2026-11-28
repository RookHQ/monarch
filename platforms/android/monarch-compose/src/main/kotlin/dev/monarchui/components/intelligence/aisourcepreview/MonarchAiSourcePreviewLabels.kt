package dev.monarchui.components.intelligence.aisourcepreview

import androidx.compose.runtime.Immutable
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourceKind

/**
 * Replacements for the built-in strings of a `MonarchAiSourcePreview`. Every
 * `null` keeps the built-in string `monarch_aisourcepreview_<key>`; the kind
 * names are the ones `MonarchAiSourceList` uses.
 *
 * @property quoted the caption of the quote block, also read as the state of the highlighted paragraph,
 * "Quoted passage".
 * @property openOriginal the button that opens the source, "Open original".
 * @property noPreview shown when there is neither content nor a quote, "No preview is available for this
 * source."
 * @property close the button that closes the sheet, "Close".
 * @property kind the name of each kind of source: "Web page", "Document", "Record", "File".
 */
@Immutable
public class MonarchAiSourcePreviewLabels(
    public val quoted: String? = null,
    public val openOriginal: String? = null,
    public val noPreview: String? = null,
    public val close: String? = null,
    public val kind: ((kind: MonarchAiSourceKind) -> String)? = null,
)
