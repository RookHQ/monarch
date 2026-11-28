package dev.monarchui.screenshots.showcase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.monarchui.accessibility.monarchTouchTargetOverhang
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.navigation.link.MonarchLink
import dev.monarchui.components.navigation.link.MonarchLinkVariant
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sampleicons.SampleIcons

private val gap = Arrangement.spacedBy(8.dp)

internal val actionShowcases: Map<String, @Composable () -> Unit> = mapOf(
    "button" to {
        // Variants, then sizes and states, then status colours, then a full-width call to action. Each row lets its
        // 48dp touch targets overhang it, so the gaps between rows are the visible 16dp, not 32.
        ShowcaseColumn {
            Wrap(gap, Modifier.monarchTouchTargetOverhang()) {
                MonarchButton(text = "Save changes", onClick = {}, variant = MonarchButtonVariant.Primary)
                MonarchButton(text = "Cancel", onClick = {}, variant = MonarchButtonVariant.Secondary)
                MonarchButton(text = "Learn more", onClick = {}, variant = MonarchButtonVariant.Ghost)
            }
            Wrap(gap, Modifier.monarchTouchTargetOverhang()) {
                MonarchButton(text = "Delete", onClick = {}, variant = MonarchButtonVariant.Danger)
                MonarchButton(text = "Remove", onClick = {}, variant = MonarchButtonVariant.DangerSubtle)
                MonarchButton(text = "Read the guide", onClick = {}, variant = MonarchButtonVariant.Link)
            }
            Wrap(gap, Modifier.monarchTouchTargetOverhang()) {
                MonarchButton(text = "Small", onClick = {}, size = MonarchButtonSize.Sm)
                MonarchButton(text = "Medium", onClick = {}, size = MonarchButtonSize.Md)
                MonarchButton(
                    text = "New ticket",
                    onClick = {},
                    size = MonarchButtonSize.Lg,
                    leadingIcon = { MonarchIcon(SampleIcons.Plus, contentDescription = null) },
                )
            }
            Wrap(gap, Modifier.monarchTouchTargetOverhang()) {
                MonarchButton(
                    text = "Saving",
                    onClick = {},
                    variant = MonarchButtonVariant.Primary,
                    loading = true,
                    leadingIcon = { MonarchIcon(SampleIcons.Check, contentDescription = null) },
                )
                MonarchButton(text = "Disabled", onClick = {}, enabled = false)
                MonarchButton(text = "Approve", onClick = {}, variant = MonarchButtonVariant.Success)
            }
            MonarchButton(
                text = "Continue",
                onClick = {},
                modifier = Modifier.fillMaxWidth().monarchTouchTargetOverhang(),
                variant = MonarchButtonVariant.Primary,
                trailingIcon = { MonarchIcon(SampleIcons.ArrowForward, contentDescription = null) },
            )
        }
    },
    "icon-button" to {
        // A message toolbar: quiet ghost actions, one outlined action, and the sizes.
        ShowcaseColumn {
            Wrap(gap, Modifier.monarchTouchTargetOverhang()) {
                MonarchIconButton(label = "Mark as unread", onClick = {}) {
                    MonarchIcon(SampleIcons.MarkUnread, contentDescription = null)
                }
                MonarchIconButton(label = "Archive", onClick = {}) {
                    MonarchIcon(SampleIcons.Archive, contentDescription = null)
                }
                MonarchIconButton(label = "Delete", onClick = {}) {
                    MonarchIcon(SampleIcons.Trash, contentDescription = null)
                }
                MonarchIconButton(label = "More actions", onClick = {}) {
                    MonarchIcon(SampleIcons.MoreVertical, contentDescription = null)
                }
            }
            Wrap(gap, Modifier.monarchTouchTargetOverhang()) {
                MonarchIconButton(
                    label = "Add",
                    onClick = {},
                    variant = MonarchButtonVariant.Secondary,
                    size = MonarchButtonSize.Sm,
                ) {
                    MonarchIcon(SampleIcons.Plus, contentDescription = null)
                }
                MonarchIconButton(label = "Add", onClick = {}, variant = MonarchButtonVariant.Secondary) {
                    MonarchIcon(SampleIcons.Plus, contentDescription = null)
                }
                MonarchIconButton(
                    label = "Add",
                    onClick = {},
                    variant = MonarchButtonVariant.Secondary,
                    size = MonarchButtonSize.Lg,
                ) {
                    MonarchIcon(SampleIcons.Plus, contentDescription = null)
                }
                MonarchIconButton(label = "Edit", onClick = {}, variant = MonarchButtonVariant.Primary) {
                    MonarchIcon(SampleIcons.Pencil, contentDescription = null)
                }
            }
        }
    },
    "link" to {
        ShowcaseColumn {
            Wrap(gap) {
                MonarchLink(text = "Read the billing guide", onClick = {})
                MonarchLink(text = "Manage plan", onClick = {}, variant = MonarchLinkVariant.Standalone)
                MonarchLink(text = "Skip for now", onClick = {}, variant = MonarchLinkVariant.Subtle)
            }
            MonarchLink(text = "Open the status page", onClick = {}, external = true)
            MonarchLink(text = "Unavailable", onClick = {}, enabled = false)
        }
    },
)
