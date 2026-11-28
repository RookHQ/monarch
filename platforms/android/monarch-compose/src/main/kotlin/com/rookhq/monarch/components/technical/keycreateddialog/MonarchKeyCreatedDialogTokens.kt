package com.rookhq.monarch.components.technical.keycreateddialog

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens

/** Component tokens and the pure logic of MonarchKeyCreatedDialog. */
internal object MonarchKeyCreatedDialogTokens {
    /** The space between the parts of the dialog's content. */
    val Gap: Dp = MonarchTokens.Space.space4.dp

    /** The space between the message that asks for the acknowledgement and the checkbox. */
    val NudgeGap: Dp = MonarchTokens.Space.space1.dp

    /** Whether the checkbox must be ticked before closing: [override], else only for a secret. */
    fun needsAcknowledgement(kind: MonarchKeyCreatedDialogKind, override: Boolean?): Boolean =
        override ?: (kind == MonarchKeyCreatedDialogKind.Secret)

    /** Whether Done, Back and the app's own dismissal requests may close the dialog. */
    fun canClose(needsAcknowledgement: Boolean, acknowledged: Boolean): Boolean = !needsAcknowledgement || acknowledged

    /** The tone of the notice: a warning for a secret, information for a public value. */
    fun noticeTone(kind: MonarchKeyCreatedDialogKind): MonarchTone = when (kind) {
        MonarchKeyCreatedDialogKind.Secret -> MonarchTone.Warning
        MonarchKeyCreatedDialogKind.Public -> MonarchTone.Info
    }

    /** Whether the copied clip is marked sensitive, so Android 13 and later hide it in the clipboard preview. */
    fun sensitive(kind: MonarchKeyCreatedDialogKind): Boolean = kind == MonarchKeyCreatedDialogKind.Secret

    /** The colour of the message that asks for the acknowledgement. */
    fun nudgeColor(colors: MonarchColors): Color = colors.dangerStrong
}
