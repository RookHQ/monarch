package dev.monarchui.screenshots.showcase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.progress.MonarchProgress
import dev.monarchui.components.datadisplay.progress.MonarchProgressSize
import dev.monarchui.components.feedback.calloutbanner.MonarchCalloutBanner
import dev.monarchui.components.feedback.emptystate.MonarchEmptyState
import dev.monarchui.components.feedback.spinner.MonarchSpinner
import dev.monarchui.components.feedback.spinner.MonarchSpinnerSize
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.MonarchTheme

internal val feedbackShowcases: Map<String, @Composable () -> Unit> = mapOf(
    "callout-banner" to {
        ShowcaseColumn {
            MonarchCalloutBanner(
                message = "Your trial ends in 3 days. Add a payment method to keep your projects.",
                tone = MonarchTone.Info,
                title = "Trial ending",
                icon = { MonarchIcon(SampleIcons.Info, contentDescription = null) },
                action = { MonarchButton(text = "Add payment", onClick = {}, size = MonarchButtonSize.Sm) },
            )
            MonarchCalloutBanner(
                message = "Invoice 1042 was sent to Acme Ltd.",
                tone = MonarchTone.Success,
                icon = { MonarchIcon(SampleIcons.Success, contentDescription = null) },
                onDismiss = {},
                dismissIcon = { MonarchIcon(SampleIcons.Close, contentDescription = null) },
            )
            MonarchCalloutBanner(
                message = "Two payments failed. Update the card to avoid a pause.",
                tone = MonarchTone.Warning,
                title = "Payment problem",
                icon = { MonarchIcon(SampleIcons.Warning, contentDescription = null) },
            )
        }
    },
    "progress-bar" to {
        ShowcaseColumn {
            MonarchProgress(progress = 0.64f, label = "Uploading files", showValue = true)
            MonarchProgress(progress = 1f, label = "Backup complete", tone = MonarchTone.Success, showValue = true)
            MonarchProgress(
                progress = 0.3f,
                label = "Storage almost full",
                tone = MonarchTone.Warning,
                size = MonarchProgressSize.Sm,
            )
            MonarchProgress(label = "Preparing export")
        }
    },
    "spinner" to {
        // A spinner next to what is loading, in the muted text colour, at the three sizes.
        ShowcaseColumn {
            listOf(
                MonarchSpinnerSize.Sm to "Saving draft",
                MonarchSpinnerSize.Md to "Loading invoices",
                MonarchSpinnerSize.Lg to "Syncing your calendar",
            ).forEach { (size, text) ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MonarchSpinner(size = size, color = MonarchTheme.colors.textMuted)
                    MonarchText(text = text, color = MonarchTheme.colors.textMuted)
                }
            }
        }
    },
    "empty-state" to {
        ShowcaseColumn {
            MonarchEmptyState(
                title = "No invoices yet",
                message = "Invoices you create appear here.",
                icon = { MonarchIcon(SampleIcons.Search, contentDescription = null) },
                actions = {
                    MonarchButton(
                        text = "Create invoice",
                        onClick = {},
                        variant = MonarchButtonVariant.Primary,
                        leadingIcon = { MonarchIcon(SampleIcons.Plus, contentDescription = null) },
                    )
                },
            )
        }
    },
)
