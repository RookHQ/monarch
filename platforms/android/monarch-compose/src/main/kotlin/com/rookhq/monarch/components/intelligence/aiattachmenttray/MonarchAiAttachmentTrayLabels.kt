package com.rookhq.monarch.components.intelligence.aiattachmenttray

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.files.attachmentchip.MonarchAttachmentChipLabels

/**
 * Replacements for the built-in strings of a `MonarchAiAttachmentTray`. Every
 * `null` keeps the built-in string `monarch_aiattachmenttray_<key>`.
 *
 * @property label the name TalkBack reads for the tray, "Attachments".
 * @property ready announced when a file finishes uploading, from its name: "photo.png uploaded".
 * @property removed announced when a file is removed with its button, from its name: "photo.png removed".
 * @property chip the strings of each `MonarchAttachmentChip`.
 */
@Immutable
public class MonarchAiAttachmentTrayLabels(
    public val label: String? = null,
    public val ready: ((String) -> String)? = null,
    public val removed: ((String) -> String)? = null,
    public val chip: MonarchAttachmentChipLabels = MonarchAttachmentChipLabels(),
)
