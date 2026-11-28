package dev.monarchui.components.intelligence.aiconversation

/**
 * A file attached to a message. Upload the file first; the part carries where it is, not its bytes.
 *
 * @property name the file name.
 * @property mediaType the media type, for example `application/pdf`.
 * @property url where the file lives: an upload URL, or a `data:` URL for an inlined file.
 * @property size the size in bytes.
 * @property fileId the id from your upload service, sent to the model service instead of the bytes.
 */
public data class MonarchAiFilePart(
    public val name: String,
    public val mediaType: String,
    public val url: String? = null,
    public val size: Long? = null,
    public val fileId: String? = null,
) : MonarchAiPart {
    override val type: String get() = "file"
}
