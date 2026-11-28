package com.rookhq.monarch.components.intelligence.aiartifactcanvas

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiArtifactKind

/**
 * The artifact a `MonarchAiArtifactCanvas` shows: one version of it, with
 * its full content.
 *
 * @property id the artifact's stable id.
 * @property kind what the artifact is; it decides the preview.
 * @property title the artifact's title.
 * @property version the version shown, counting from 1.
 * @property content the content as text: Markdown for documents and tables, source for code, an HTML document for
 * [MonarchAiArtifactKind.Html]. Untrusted model output: HTML is only shown in a sandboxed `WebView`. For an image,
 * the app loads the picture and passes it to the canvas.
 * @property language the language of the Code tab, such as "kotlin"; `null` uses the kind's default.
 */
@Immutable
public data class MonarchAiCanvasArtifact(
    public val id: String,
    public val kind: MonarchAiArtifactKind,
    public val title: String,
    public val version: Int,
    public val content: String,
    public val language: String? = null,
)
