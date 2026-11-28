package dev.monarchui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.internal.field.MonarchFieldBox
import dev.monarchui.internal.field.MonarchFieldPlaceholder
import dev.monarchui.internal.field.MonarchFieldState
import dev.monarchui.internal.field.monarchFieldTextStyle
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.tokens.MonarchTokens

/**
 * Review-only showcases of the W0 foundations that have no public component (and so no inventory id): rendered
 * with the docs preview pipeline (`recordPreviews -Pmonarch.previews.ids=field-box`) into a scratch folder for the
 * side-by-side review. Never written to `platforms/android/previews/`: the ids are rendered only when asked for.
 */
internal object ReviewShowcases {
    val all: Map<String, @Composable () -> Unit> = mapOf("field-box" to { FieldBoxReview() })
}

/** One workspace form: a filled field, a focused search with an icon, an invalid one with a suffix, a disabled one. */
@Composable
private fun FieldBoxReview() {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MonarchTokens.Space.space5.dp)) {
        Labelled("Workspace name", helper = "Shown on invoices and receipts.") {
            Field(value = "Northwind Studio")
        }
        Labelled("Invite a teammate") {
            Field(
                value = "Priya",
                state = MonarchFieldState(focused = true),
                leading = { MonarchIcon(SampleIcons.Search, contentDescription = null, size = MonarchIconSize.Sm) },
            )
        }
        Labelled("Seats", error = "Your plan allows up to 50 seats.") {
            Field(value = "64", state = MonarchFieldState(invalid = true), suffix = "seats")
        }
        Labelled("Billing region", helper = "Set by your organization.") {
            Field(value = "", placeholder = "Europe (Frankfurt)", state = MonarchFieldState(enabled = false))
        }
    }
}

@Composable
private fun Labelled(label: String, helper: String? = null, error: String? = null, field: @Composable () -> Unit) {
    val colors = MonarchTheme.colors
    val type = MonarchTheme.typography
    Column(verticalArrangement = Arrangement.spacedBy(MonarchTokens.Space.space1_5.dp)) {
        Text(label, style = type.label, color = colors.textDefault)
        field()
        helper?.let { Text(it, style = type.caption, color = colors.textMuted) }
        error?.let {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MonarchTokens.Space.space1_5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchIcon(SampleIcons.Error, null, size = MonarchIconSize.Sm, tint = colors.dangerStrong)
                Text(it, style = type.caption, color = colors.dangerStrong)
            }
        }
    }
}

@Composable
private fun Field(
    value: String,
    state: MonarchFieldState = MonarchFieldState(),
    placeholder: String? = null,
    leading: (@Composable () -> Unit)? = null,
    suffix: String? = null,
) {
    var text by remember { mutableStateOf(value) }
    MonarchFieldBox(
        modifier = Modifier.fillMaxWidth(),
        state = state,
        leading = leading,
        suffix = suffix,
    ) {
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            enabled = state.enabled,
            singleLine = true,
            textStyle = monarchFieldTextStyle().copy(
                color = if (state.enabled) MonarchTheme.colors.textDefault else MonarchTheme.colors.textDisabled,
            ),
            decorationBox = { inner ->
                Box {
                    if (text.isEmpty() && placeholder != null) MonarchFieldPlaceholder(placeholder, state.enabled)
                    inner()
                }
            },
        )
    }
}
