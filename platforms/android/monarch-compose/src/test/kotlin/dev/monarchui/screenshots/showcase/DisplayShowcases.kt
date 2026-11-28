package dev.monarchui.screenshots.showcase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.datadisplay.accordion.MonarchAccordion
import dev.monarchui.components.datadisplay.accordion.MonarchAccordionItem
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.datadisplay.badge.MonarchBadgeVariant
import dev.monarchui.components.datadisplay.card.MonarchCard
import dev.monarchui.components.datadisplay.card.MonarchCardHeader
import dev.monarchui.components.datadisplay.listrow.MonarchListRow
import dev.monarchui.components.datadisplay.metric.MonarchMetricChange
import dev.monarchui.components.datadisplay.metric.MonarchMetricFormat
import dev.monarchui.components.datadisplay.statcard.MonarchStatCard
import dev.monarchui.components.datagrid.datatable.MonarchDataTable
import dev.monarchui.components.datagrid.datatable.MonarchDataTableColumn
import dev.monarchui.components.datagrid.datatable.MonarchDataTableLayout
import dev.monarchui.components.feedback.skeleton.MonarchSkeleton
import dev.monarchui.components.feedback.skeleton.MonarchSkeletonShape
import dev.monarchui.components.identity.avatar.MonarchAvatar
import dev.monarchui.components.identity.avatar.MonarchAvatarGroup
import dev.monarchui.components.identity.avatar.MonarchAvatarShape
import dev.monarchui.components.identity.avatar.MonarchAvatarSize
import dev.monarchui.components.identity.presencedot.MonarchPresenceStatus
import dev.monarchui.components.navigation.breadcrumb.MonarchBreadcrumb
import dev.monarchui.components.navigation.breadcrumb.MonarchBreadcrumbItem
import dev.monarchui.components.typography.codeblock.MonarchCodeBlock
import dev.monarchui.components.typography.kbd.MonarchKbd
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.components.workflow.stepper.MonarchStepper
import dev.monarchui.components.workflow.stepper.MonarchStepperOrientation
import dev.monarchui.components.workflow.stepper.MonarchStepperStep
import dev.monarchui.foundation.MonarchMotion
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sampleicons.SampleIcons
import dev.monarchui.theme.LocalMonarchMotion
import dev.monarchui.theme.MonarchTheme

private val gap = Arrangement.spacedBy(8.dp)

internal val displayShowcases: Map<String, @Composable () -> Unit> = mapOf(
    "badge" to {
        ShowcaseColumn {
            Wrap(gap) {
                MonarchBadge(text = "New")
                MonarchBadge(text = "Active", tone = MonarchTone.Success)
                MonarchBadge(text = "Pending", tone = MonarchTone.Warning)
                MonarchBadge(text = "Failed", tone = MonarchTone.Danger)
                MonarchBadge(text = "Beta", tone = MonarchTone.Info)
            }
            // Soft tint with strong text is the default; outline for a quiet label; an outlined icon when it helps.
            Wrap(gap) {
                MonarchBadge(text = "Draft", variant = MonarchBadgeVariant.Outline)
                MonarchBadge(text = "Overdue", tone = MonarchTone.Danger, variant = MonarchBadgeVariant.Outline)
                MonarchBadge(
                    text = "Featured",
                    tone = MonarchTone.Accent,
                    leadingIcon = { MonarchIcon(SampleIcons.Star, contentDescription = null) },
                )
            }
        }
    },
    "avatar" to {
        ShowcaseColumn {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                MonarchAvatar(image = null, name = "Ana Silva", size = MonarchAvatarSize.Xs)
                MonarchAvatar(image = null, name = "Ben Okafor", size = MonarchAvatarSize.Sm)
                MonarchAvatar(image = null, name = "Chen Wei", size = MonarchAvatarSize.Md)
                MonarchAvatar(image = null, name = "Dara Novak", size = MonarchAvatarSize.Lg)
                MonarchAvatar(image = null, name = "Eli Park", size = MonarchAvatarSize.Xl)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                MonarchAvatar(
                    image = null,
                    name = "Ana Silva",
                    size = MonarchAvatarSize.Lg,
                    status = MonarchPresenceStatus.Online,
                )
                MonarchAvatar(
                    image = null,
                    name = "Ben Okafor",
                    size = MonarchAvatarSize.Lg,
                    status = MonarchPresenceStatus.Away,
                )
                MonarchAvatar(
                    image = null,
                    name = "Chen Wei",
                    size = MonarchAvatarSize.Lg,
                    status = MonarchPresenceStatus.Busy,
                )
                MonarchAvatar(
                    image = null,
                    name = "Rook Labs",
                    size = MonarchAvatarSize.Lg,
                    shape = MonarchAvatarShape.Square,
                )
            }
            MonarchAvatarGroup(
                items = listOf("Ana Silva", "Ben Okafor", "Chen Wei", "Dara Novak", "Eli Park"),
                label = "Project members",
                maxVisible = 3,
            ) { person -> MonarchAvatar(image = null, name = person) }
        }
    },
    "card" to {
        // An outline card with a header, a figure and a footer action, and a card that opens as a whole. No raised
        // card: inline cards draw no shadow (PREMIUM rule 7).
        ShowcaseColumn {
            MonarchCard(
                header = { MonarchCardHeader(title = "Open tickets", description = "Updated 5 minutes ago") },
                footer = {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        MonarchButton(text = "View all", onClick = {}, size = MonarchButtonSize.Sm)
                    }
                },
            ) {
                MonarchText(text = "128", style = MonarchTextStyle.Title)
            }
            MonarchCard(
                onClick = {},
                header = { MonarchCardHeader(title = "Acme renewal", description = "Closes Friday · $24,000") },
            ) {
                MonarchText(
                    text = "Sam Ortiz is waiting on the signed order form.",
                    style = MonarchTextStyle.Body,
                    color = MonarchTheme.colors.textMuted,
                )
            }
        }
    },
    "list-row" to {
        // A settings list: outlined icons, one muted supporting line, a quiet chevron or value at the end, hairline
        // dividers and no card or shadow around the rows (PREMIUM rules 6, 7 and 9).
        val chevron: @Composable () -> Unit = {
            MonarchIcon(
                SampleIcons.ChevronRight,
                contentDescription = null,
                tint = MonarchTheme.colors.textMuted,
            )
        }
        Column(Modifier.fillMaxWidth()) {
            MonarchListRow(
                headlineContent = { Text("Billing") },
                supportingContent = { Text("Visa ending 4242 · Renews Apr 1") },
                leadingContent = { MonarchIcon(SampleIcons.Card, contentDescription = null) },
                trailingContent = chevron,
                onClick = {},
                divider = true,
            )
            MonarchListRow(
                headlineContent = { Text("Members") },
                supportingContent = { Text("12 people in Acme Corp") },
                leadingContent = { MonarchIcon(SampleIcons.Users, contentDescription = null) },
                trailingContent = { MonarchBadge(text = "2 invites", tone = MonarchTone.Info) },
                onClick = {},
                divider = true,
            )
            MonarchListRow(
                headlineContent = { Text("Notifications") },
                supportingContent = { Text("Email and push") },
                leadingContent = { MonarchIcon(SampleIcons.Bell, contentDescription = null) },
                trailingContent = chevron,
                onClick = {},
                divider = true,
            )
            MonarchListRow(
                headlineContent = { Text("Security") },
                supportingContent = { Text("Two-step sign-in is on") },
                leadingContent = { MonarchIcon(SampleIcons.Lock, contentDescription = null) },
                trailingContent = chevron,
                onClick = {},
            )
        }
    },
    "accordion" to {
        var open by remember { mutableStateOf(setOf("billing")) }
        ShowcaseColumn {
            MonarchAccordion(
                items = listOf(
                    MonarchAccordionItem("billing", "How does billing work?"),
                    MonarchAccordionItem("cancel", "Can I cancel at any time?"),
                    MonarchAccordionItem("export", "Can I export my data?"),
                ),
                expanded = open,
                onExpandedChange = { open = it },
                trailingIcon = { MonarchIcon(SampleIcons.ChevronDown, contentDescription = null) },
            ) { item ->
                MonarchText(
                    text = when (item.id) {
                        "billing" -> "Invoices are sent monthly to the billing contact."
                        "cancel" -> "Yes, at the end of the current period."
                        else -> "Yes, as CSV from the settings page."
                    },
                )
            }
        }
    },
    "skeleton" to {
        // A message loading on a card: avatar and two lines, a preview block, two lines of text, 12dp apart. The
        // pulse never settles, so the preview shows the resting fill (animations off), where the pulse starts.
        CompositionLocalProvider(LocalMonarchMotion provides MonarchMotion(reducedMotion = true)) {
            MonarchCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MonarchSkeleton(shape = MonarchSkeletonShape.Circle)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            MonarchSkeleton(Modifier.width(180.dp), shape = MonarchSkeletonShape.Text)
                            MonarchSkeleton(Modifier.width(120.dp), shape = MonarchSkeletonShape.Text)
                        }
                    }
                    MonarchSkeleton(Modifier.height(96.dp), shape = MonarchSkeletonShape.Rect)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MonarchSkeleton(shape = MonarchSkeletonShape.Text)
                        MonarchSkeleton(Modifier.fillMaxWidth(0.7f), shape = MonarchSkeletonShape.Text)
                    }
                }
            }
        }
    },
    "kbd" to {
        ShowcaseColumn {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                MonarchText(text = "Search", modifier = Modifier.weight(1f))
                MonarchKbd(keys = "Ctrl+K", alwaysVisible = true)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                MonarchText(text = "New message", modifier = Modifier.weight(1f))
                MonarchKbd(keys = "Ctrl+Shift+N", alwaysVisible = true)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                MonarchText(text = "Close", modifier = Modifier.weight(1f))
                MonarchKbd(keys = "Esc", alwaysVisible = true)
            }
        }
    },
    "code-block" to {
        ShowcaseColumn {
            MonarchCodeBlock(
                code = "val invoice = Invoice(\n    customer = \"cus_1042\",\n" +
                    "    amount = 4_800,\n)\nclient.send(invoice)",
                language = "kotlin",
                title = "CreateInvoice.kt",
                showLineNumbers = true,
            )
        }
    },
    "stat-card" to {
        ShowcaseColumn {
            MonarchStatCard(
                label = "Revenue",
                value = 48_200.0,
                format = MonarchMetricFormat.Currency("EUR"),
                change = MonarchMetricChange.between(48_200.0, 43_000.0),
                caption = "vs last month",
            )
            MonarchStatCard(label = "Open tickets", value = 128.0, change = MonarchMetricChange.between(128.0, 141.0))
            MonarchStatCard(label = "Trial conversions", value = null, loading = true)
        }
    },
    "stepper" to {
        ShowcaseColumn {
            MonarchStepper(
                steps = listOf(
                    MonarchStepperStep("account", "Account", complete = true),
                    MonarchStepperStep("plan", "Plan"),
                    MonarchStepperStep("payment", "Payment"),
                    MonarchStepperStep("confirm", "Confirm"),
                ),
                current = 1,
                orientation = MonarchStepperOrientation.Vertical,
            )
        }
    },
    "data-table" to {
        val rows = listOf(
            listOf("Ticket 42", "Open", "3"),
            listOf("Ticket 7", "Closed", "12"),
            listOf("Ticket 19", "Open", "5"),
        )
        ShowcaseColumn {
            MonarchDataTable(
                caption = "Support tickets",
                rows = rows,
                columns = listOf(
                    MonarchDataTableColumn<List<String>>("Ticket", id = "id") { it[0] },
                    MonarchDataTableColumn<List<String>>("Status", id = "status") { it[1] },
                    MonarchDataTableColumn<List<String>>("Replies", id = "replies", numeric = true) { it[2] },
                ),
                rowKey = { it[0] },
                layout = MonarchDataTableLayout.Table,
            )
        }
    },
    "breadcrumb" to {
        ShowcaseColumn {
            MonarchBreadcrumb(
                items = listOf("Drive", "Clients", "Acme Ltd", "Contracts").map { MonarchBreadcrumbItem(it) },
                onItemClick = {},
            )
        }
    },
)
