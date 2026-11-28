package com.rookhq.monarch.screenshots.showcase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.datadisplay.progress.MonarchProgress
import com.rookhq.monarch.components.datadisplay.progress.MonarchProgressSize
import com.rookhq.monarch.components.feedback.calloutbanner.MonarchCalloutBanner
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyState
import com.rookhq.monarch.components.feedback.spinner.MonarchSpinner
import com.rookhq.monarch.components.feedback.spinner.MonarchSpinnerSize
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.sampleicons.SampleIcons
import com.rookhq.monarch.theme.MonarchTheme

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
