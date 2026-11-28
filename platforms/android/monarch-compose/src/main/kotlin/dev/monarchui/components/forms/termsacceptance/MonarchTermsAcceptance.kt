package dev.monarchui.components.forms.termsacceptance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.forms.checkbox.MonarchCheckboxWithLinks
import dev.monarchui.components.navigation.link.MonarchLinkDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * The required "I agree to the Terms of Service and Privacy Policy" checkbox
 * for sign-up, with a link to each document, reporting the versions that were
 * accepted and when.
 *
 * The sentence is the label of the checkbox row, with each document's title a
 * link inside it (a `LinkAnnotation` in the Monarch link style), as on the web.
 * The row toggles as a whole and the links open their documents; TalkBack lists
 * the links of the row. A required box says
 * "Required" under the sentence. While [showRequiredError] is set and the box
 * is not ticked, the error is shown in words, set as the box's error for
 * TalkBack and announced through a polite live region.
 *
 * Start [accepted] as `false` for sign-up so the agreement is an active
 * choice, and set [showRequiredError] when the person tries to continue.
 *
 * @param accepted whether the box is ticked.
 * @param onAcceptedChange called when the box changes, with the new state and the details to store: the version of
 * each document and the time from [clock].
 * @param documents the documents. Their titles are joined into the sentence and each is a link inside it.
 * @param modifier the modifier of the group.
 * @param required whether the box must be ticked to continue. Shows "Required" and allows the error.
 * @param showRequiredError shows "Accept the terms to continue" while the box is required and not ticked.
 * @param enabled whether the box and the links can be used.
 * @param onOpenDocument opens a document. `null` opens its url with `LocalUriHandler`.
 * @param clock the current time in epoch milliseconds, for the details. Replace it in tests.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchTermsAcceptance(
    accepted: Boolean,
    onAcceptedChange: (Boolean, MonarchTermsAcceptanceDetails) -> Unit,
    documents: List<MonarchTermsDocument>,
    modifier: Modifier = Modifier,
    required: Boolean = true,
    showRequiredError: Boolean = false,
    enabled: Boolean = true,
    onOpenDocument: ((MonarchTermsDocument) -> Unit)? = null,
    clock: () -> Long = { System.currentTimeMillis() },
    labels: MonarchTermsAcceptanceLabels = MonarchTermsAcceptanceLabels(),
) {
    val tokens = MonarchTermsAcceptanceTokens
    val uriHandler = LocalUriHandler.current
    val two = stringResource(R.string.monarch_termsacceptance_join_two)
    val middle = stringResource(R.string.monarch_termsacceptance_join_middle)
    val last = stringResource(R.string.monarch_termsacceptance_join_last)
    val titles = tokens.join(
        documents.map { it.title },
        two = { a, b -> two.format(a, b) },
        middle = { a, b -> middle.format(a, b) },
        last = { a, b -> last.format(a, b) },
    )
    val sentence = labels.label?.invoke(titles) ?: stringResource(R.string.monarch_termsacceptance_label, titles)
    val linkStyles = MonarchLinkDefaults.styles()
    val label = remember(sentence, titles, documents, enabled, linkStyles, onOpenDocument) {
        tokens.linkedSentence(sentence, titles, documents, if (enabled) linkStyles else null) { document ->
            onOpenDocument?.invoke(document) ?: uriHandler.openUri(document.url)
        }
    }
    val errorText = monarchString(labels.requiredMessage, R.string.monarch_termsacceptance_required_message)
    val showsError = tokens.showsError(required, showRequiredError, accepted)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
        MonarchCheckboxWithLinks(
            checked = accepted,
            onCheckedChange = { checked ->
                onAcceptedChange(
                    checked,
                    MonarchTermsAcceptanceDetails(MonarchTermsDocument.versions(documents), clock()),
                )
            },
            label = label,
            modifier = Modifier.semantics { if (showsError) error(errorText) },
            enabled = enabled,
            description = if (required) {
                monarchString(labels.required, R.string.monarch_termsacceptance_required)
            } else {
                null
            },
        )
        if (showsError) {
            Text(
                text = errorText,
                modifier = Modifier.padding(start = tokens.TextIndent),
                style = MonarchTheme.typography.caption,
                color = tokens.errorColor(MonarchTheme.colors),
            )
        }
        // Always present, so the error is announced once when it appears.
        Box(
            Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                if (showsError) contentDescription = errorText
            },
        )
    }
}
