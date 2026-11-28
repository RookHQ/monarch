package dev.monarchui.components.identity.stepupauthdialog

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens

/** The sizes, colours and pure rules of MonarchStepUpAuthDialog. */
internal object MonarchStepUpAuthDialogTokens {
    /** The gap between the parts of the dialog. */
    val Gap = MonarchTokens.Space.space4.dp

    /** The round tile behind the lock above the title. */
    val IconTile = MonarchTokens.Space.space10.dp

    /** The gap between the buttons. */
    val ButtonGap = MonarchTokens.Space.space2.dp

    /** The digits of an authenticator code. */
    const val CodeLength = 6

    /** The code cells are shown in groups of this size. */
    const val CodeGroup = 3

    /** The colour of the description. */
    fun description(colors: MonarchColors): Color = colors.textMuted

    /**
     * The methods offered, in order, without repeats. [MonarchStepUpMethod.DeviceOwner] is left out when the app
     * gives no way to run the device check; an empty list is the password.
     */
    fun methods(methods: List<MonarchStepUpMethod>, deviceCheck: Boolean): List<MonarchStepUpMethod> =
        methods.distinct()
            .filter { it != MonarchStepUpMethod.DeviceOwner || deviceCheck }
            .ifEmpty { listOf(MonarchStepUpMethod.Password) }

    /** Keeps only digits, at most [CodeLength]. */
    fun code(text: String): String = text.filter { it in '0'..'9' }.take(CodeLength)

    /** Whether the method has a field the person fills in. */
    fun hasField(method: MonarchStepUpMethod): Boolean =
        method == MonarchStepUpMethod.Password || method == MonarchStepUpMethod.Code
}
