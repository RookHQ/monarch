package com.rookhq.monarch.components.files.filetypeicon

import android.webkit.MimeTypeMap

/** The extension and MIME tables of the web `getFileTypeFamily`, so every platform agrees. */
internal object MonarchFileTypeRules {
    private val extensions: Map<MonarchFileTypeFamily, List<String>> = mapOf(
        MonarchFileTypeFamily.Image to listOf(
            "png", "jpg", "jpeg", "gif", "webp", "avif", "svg", "bmp", "ico", "heic", "heif", "tif", "tiff",
        ),
        MonarchFileTypeFamily.Video to listOf("mp4", "m4v", "mov", "webm", "mkv", "avi", "ogv", "wmv"),
        MonarchFileTypeFamily.Audio to listOf("mp3", "wav", "ogg", "oga", "m4a", "aac", "flac", "opus", "weba"),
        MonarchFileTypeFamily.Pdf to listOf("pdf"),
        MonarchFileTypeFamily.Document to listOf("doc", "docx", "odt", "rtf", "pages"),
        MonarchFileTypeFamily.Spreadsheet to listOf("xls", "xlsx", "ods", "csv", "tsv", "numbers"),
        MonarchFileTypeFamily.Presentation to listOf("ppt", "pptx", "odp", "key"),
        MonarchFileTypeFamily.Archive to listOf("zip", "rar", "7z", "tar", "gz", "tgz", "bz2", "xz"),
        MonarchFileTypeFamily.Code to listOf(
            "js", "jsx", "ts", "tsx", "mjs", "cjs", "json", "html", "htm", "css", "scss", "py", "rb", "go", "rs",
            "java", "kt", "swift", "c", "h", "cpp", "cs", "php", "sh", "sql", "yaml", "yml", "xml", "toml",
        ),
        MonarchFileTypeFamily.Text to listOf("txt", "md", "markdown", "log", "ini"),
    )

    private val byExtension: Map<String, MonarchFileTypeFamily> =
        extensions.flatMap { (family, list) -> list.map { it to family } }.toMap()

    private val mimeRules: List<Pair<Regex, MonarchFileTypeFamily>> = listOf(
        Regex("^image/") to MonarchFileTypeFamily.Image,
        Regex("^video/") to MonarchFileTypeFamily.Video,
        Regex("^audio/") to MonarchFileTypeFamily.Audio,
        Regex("^application/pdf$") to MonarchFileTypeFamily.Pdf,
        Regex("(spreadsheet|ms-excel|text/csv|tab-separated)") to MonarchFileTypeFamily.Spreadsheet,
        Regex("(presentation|ms-powerpoint)") to MonarchFileTypeFamily.Presentation,
        Regex("(wordprocessing|msword|opendocument\\.text|rtf)") to MonarchFileTypeFamily.Document,
        Regex("(zip|x-tar|gzip|x-7z|x-rar|x-bzip|x-xz|compressed)") to MonarchFileTypeFamily.Archive,
        Regex("(javascript|typescript|json|xml|x-sh|x-python|html|css|x-yaml|sql)") to MonarchFileTypeFamily.Code,
        Regex("^text/") to MonarchFileTypeFamily.Text,
    )

    private const val GENERIC_TYPE = "application/octet-stream"

    /** The lower-case extension of a file name or path, without the dot; empty when there is none. */
    fun extension(fileName: String?): String {
        val base = fileName.orEmpty().split('/', '\\').last()
        val dot = base.lastIndexOf('.')
        return if (dot > 0) base.substring(dot + 1).lowercase() else ""
    }

    /** The family of a MIME type alone, or `null` when no rule matches. */
    fun familyOfType(contentType: String?): MonarchFileTypeFamily? {
        val type = contentType.orEmpty().lowercase().substringBefore(';').trim()
        if (type.isEmpty() || type == GENERIC_TYPE) return null
        return mimeRules.firstOrNull { (rule, _) -> rule.containsMatchIn(type) }?.second
    }

    /** See [MonarchFileTypeFamily.of]. */
    fun family(fileName: String?, contentType: String?): MonarchFileTypeFamily {
        familyOfType(contentType)?.let { return it }
        val extension = extension(fileName)
        if (extension.isEmpty()) return MonarchFileTypeFamily.Other
        return byExtension[extension]
            ?: familyOfType(MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension))
            ?: MonarchFileTypeFamily.Other
    }
}
