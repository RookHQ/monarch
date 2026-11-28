package dev.monarchui.components.identity.memberrow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.components.identity.rolepicker.MonarchRoleOption
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import kotlin.coroutines.cancellation.CancellationException

/** A line under the identity after an action: a failure or a success. */
internal data class MonarchMemberRowNotice(val text: String, val error: Boolean)

/** Component tokens and the pure logic of MonarchMemberRow. */
internal object MonarchMemberRowTokens {
    /** The gap between the identity, the notice and the controls. */
    val Gap: Dp = MonarchTokens.Space.space2.dp

    /** The gap between badges and between buttons. */
    val ItemGap: Dp = MonarchTokens.Space.space2.dp

    /** The start padding of the parts under the identity, matching the list item's padding. */
    val Inset: Dp = MonarchTokens.Space.space4.dp

    /** From this width the controls sit beside the identity, as on the web. */
    val WideRowWidth: Dp = (MonarchTokens.Space.space24 * 6).dp

    /** The width of the role picker in a wide row. */
    val WideRolePickerWidth: Dp = (MonarchTokens.Space.space16 * 2.5f).dp

    /** Whether the role picker is shown: by default when the role can be changed and it is not your own row. */
    fun editable(canChangeRole: Boolean?, hasCallback: Boolean, self: Boolean, roleCount: Int): Boolean =
        hasCallback && roleCount > 0 && (canChangeRole ?: !self)

    /** Whether Remove is shown: by default when the member can be removed and it is not your own row. */
    fun removable(canRemove: Boolean?, hasCallback: Boolean, self: Boolean): Boolean =
        hasCallback && (canRemove ?: !self)

    /** The title of a role value, or the value itself when it is not one of the roles. */
    fun roleTitle(role: String?, roles: List<MonarchRoleOption<String>>): String? =
        role?.let { value -> roles.firstOrNull { it.value == value }?.title ?: value }

    /** The words shown for a failure: the error's own message, else [failed]. */
    fun failureText(error: Throwable, failed: String): String {
        if (error is CancellationException) throw error
        return error.message?.takeIf { it.isNotBlank() } ?: failed
    }

    /** What TalkBack reads after the name, the parts joined by commas and empty parts left out. */
    fun spokenValue(parts: List<String?>): String = parts.filterNot { it.isNullOrBlank() }.joinToString(", ")

    /** The tone of the status badge: [override], else info for an invitation and warning for a suspension. */
    fun statusTone(status: MonarchMemberRowStatus, override: MonarchTone?): MonarchTone =
        override ?: if (status == MonarchMemberRowStatus.Invited) MonarchTone.Info else MonarchTone.Warning

    /** The colour of the name: muted when suspended. */
    fun nameColor(status: MonarchMemberRowStatus, colors: MonarchColors): Color =
        if (status == MonarchMemberRowStatus.Suspended) colors.textMuted else colors.textDefault

    /** The colour of a notice: danger strong for a failure, success strong for a success. */
    fun noticeColor(notice: MonarchMemberRowNotice, colors: MonarchColors): Color =
        if (notice.error) colors.dangerStrong else colors.successStrong
}
