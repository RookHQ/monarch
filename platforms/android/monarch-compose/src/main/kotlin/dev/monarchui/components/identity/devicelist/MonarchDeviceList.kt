package dev.monarchui.components.identity.devicelist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.datadisplay.card.MonarchCard
import dev.monarchui.components.feedback.emptystate.MonarchEmptyState
import dev.monarchui.components.feedback.emptystate.MonarchEmptyStateSize
import dev.monarchui.components.feedback.errorstate.MonarchErrorState
import dev.monarchui.components.feedback.inlinenotice.MonarchInlineNotice
import dev.monarchui.components.feedback.loadingstate.MonarchLoadingState
import dev.monarchui.components.feedback.loadingstate.MonarchLoadingStateVariant
import dev.monarchui.components.layout.sectionheader.MonarchSectionHeader
import dev.monarchui.components.overlays.dialog.MonarchDialog
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.rememberMonarchRelativeClock
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * The devices that have accessed an account: name, system, browser and
 * location, when each was last used and first seen, This device and Blocked
 * badges, and an optional Remove action behind a confirmation.
 *
 * Built by Monarch in a `MonarchCard`: a `MonarchSectionHeader`, one row per
 * device with `MonarchBadge`s and a `MonarchButton`, and the Material 3
 * `AlertDialog` (the confirm dialog) before removing. Loading, empty and
 * failed states use `MonarchLoadingState`, `MonarchEmptyState` and
 * `MonarchErrorState`; a failed removal is a danger `MonarchInlineNotice`
 * under the header. Each device's text is one TalkBack element read as its
 * name with the badges, details and times as its state; removal and failures
 * are announced through a polite live region. The rows are not lazy: the
 * list is short and usually sits inside a scrolling settings screen. Times
 * are relative ("5 minutes ago") and follow the clock every minute unless
 * [now] is given.
 *
 * @param devices the devices, in the order shown.
 * @param modifier the modifier of the card.
 * @param title replaces the built-in title "Devices".
 * @param description replaces the line under the title.
 * @param state ready, loading or failed. A ready, empty list shows the empty state.
 * @param retrying whether a retry is in progress, in the failed state.
 * @param confirmRemoval asks before removing. Turn it off when [onRemove] shows the app's own confirmation.
 * @param now the reference time for relative times, in epoch milliseconds. Without it they follow the clock.
 * @param onRetry retries loading; shows "Try again" in the failed state.
 * @param onRemove signs the device out and forgets it, given its id; remove it from [devices] when it returns.
 * Shows Remove on every device except the current one. Only one removal runs at a time; throwing shows the failure.
 * @param deviceIcon an icon for each device's kind, hidden from TalkBack and tinted with the danger strong colour
 * when blocked. Monarch bundles no icons; the kind is never the only information.
 * @param rowActions extra controls at the end of a row, such as a block or rename menu.
 * @param labels replacements for the other built-in strings.
 */
@Composable
public fun MonarchDeviceList(
    devices: List<MonarchDevice>,
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    state: MonarchDeviceListState = MonarchDeviceListState.Ready,
    retrying: Boolean = false,
    confirmRemoval: Boolean = true,
    now: Long? = null,
    onRetry: (() -> Unit)? = null,
    onRemove: (suspend (String) -> Unit)? = null,
    deviceIcon: (@Composable (MonarchDeviceKind) -> Unit)? = MonarchDefaultIcons.Device,
    rowActions: (@Composable RowScope.(MonarchDevice) -> Unit)? = null,
    labels: MonarchDeviceListLabels = MonarchDeviceListLabels(),
) {
    val tokens = MonarchDeviceListTokens
    val ready = state == MonarchDeviceListState.Ready
    val unnamed = monarchString(labels.unnamed, R.string.monarch_devicelist_unnamed)
    val removal = rememberDeviceRemoval(onRemove, labels)
    var pending by remember { mutableStateOf<MonarchDevice?>(null) }
    val clock = deviceListClock(
        now,
        ready && devices.isNotEmpty(),
        devices.flatMap { listOf(it.lastUsedMillis, it.firstSeenMillis) },
    )
    MonarchCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(tokens.SectionGap)) {
            MonarchSectionHeader(
                title = title ?: stringResource(R.string.monarch_devicelist_title),
                description = description ?: stringResource(R.string.monarch_devicelist_description),
            )
            removal.failure?.let { MonarchInlineNotice(message = it, tone = MonarchTone.Danger) }
            when {
                state == MonarchDeviceListState.Loading -> MonarchLoadingState(
                    label = monarchString(labels.loading, R.string.monarch_devicelist_loading),
                    variant = MonarchLoadingStateVariant.Rows,
                    rows = 2,
                )
                state == MonarchDeviceListState.Failed -> MonarchErrorState(
                    title = monarchString(labels.errorTitle, R.string.monarch_devicelist_error_title),
                    size = MonarchEmptyStateSize.Sm,
                    retrying = retrying,
                    onRetry = onRetry,
                )
                devices.isEmpty() -> MonarchEmptyState(
                    title = monarchString(labels.emptyTitle, R.string.monarch_devicelist_empty_title),
                    size = MonarchEmptyStateSize.Sm,
                    message = monarchString(labels.emptyMessage, R.string.monarch_devicelist_empty_message),
                )
                else -> {
                    val listLabel = monarchString(labels.listLabel, R.string.monarch_devicelist_list_label)
                    Column(
                        modifier = Modifier.semantics {
                            contentDescription = listLabel
                            isTraversalGroup = true
                        },
                        verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
                    ) {
                        devices.forEach { device ->
                            DeviceRow(
                                device = device,
                                name = tokens.displayName(device.name, unnamed),
                                now = clock,
                                removing = removal.removingId == device.id,
                                removeEnabled = removal.removingId == null,
                                onRemove = onRemove?.let {
                                    { if (confirmRemoval) pending = device else removal.run(device, unnamed) }
                                },
                                deviceIcon = deviceIcon,
                                rowActions = rowActions,
                                labels = labels,
                            )
                        }
                    }
                }
            }
        }
        // Always present, so a removal or a failure is announced once as a change of this live region.
        MonarchLiveStatus(text = removal.announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
    pending?.let { device ->
        DeviceConfirmDialog(
            name = tokens.displayName(device.name, unnamed),
            onConfirm = {
                pending = null
                removal.run(device, unnamed)
            },
            onDismiss = { pending = null },
            labels = labels,
        )
    }
}

@Composable
private fun DeviceRow(
    device: MonarchDevice,
    name: String,
    now: Long,
    removing: Boolean,
    removeEnabled: Boolean,
    onRemove: (() -> Unit)?,
    deviceIcon: (@Composable (MonarchDeviceKind) -> Unit)?,
    rowActions: (@Composable RowScope.(MonarchDevice) -> Unit)?,
    labels: MonarchDeviceListLabels,
) {
    val tokens = MonarchDeviceListTokens
    val remove = onRemove?.takeIf { !device.current }
    val colors = MonarchTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bgSubtle, MonarchTheme.shapes.md)
            .padding(tokens.RowGap),
        horizontalArrangement = Arrangement.spacedBy(tokens.RowGap),
        verticalAlignment = Alignment.Top,
    ) {
        if (deviceIcon != null) {
            CompositionLocalProvider(LocalContentColor provides tokens.iconColor(device.blocked, colors)) {
                Box(
                    modifier = Modifier
                        .size(tokens.IconTile)
                        .background(if (device.blocked) colors.dangerSubtle else colors.bgMuted, MonarchTheme.shapes.md)
                        .clearAndSetSemantics { },
                    contentAlignment = Alignment.Center,
                ) { deviceIcon(device.kind) }
            }
        }
        Column(Modifier.weight(1f)) { DeviceText(device, name, now, labels) }
        if (remove != null || rowActions != null) {
            Row(
                modifier = Modifier.align(Alignment.CenterVertically),
                horizontalArrangement = Arrangement.spacedBy(tokens.RowGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (remove != null) {
                    val removeName = labels.removeLabel?.invoke(name)
                        ?: stringResource(R.string.monarch_devicelist_remove_label, name)
                    MonarchButton(
                        text = monarchString(labels.remove, R.string.monarch_devicelist_remove),
                        onClick = remove,
                        modifier = Modifier.semantics { contentDescription = removeName },
                        variant = MonarchButtonVariant.Ghost,
                        size = MonarchButtonSize.Sm,
                        enabled = removeEnabled || removing,
                        loading = removing,
                    )
                }
                rowActions?.invoke(this, device)
            }
        }
    }
}

/** The name, badges, details and times of a device, read as one element. */
@Composable
private fun DeviceText(device: MonarchDevice, name: String, now: Long, labels: MonarchDeviceListLabels) {
    val tokens = MonarchDeviceListTokens
    val colors = MonarchTheme.colors
    val current = monarchString(labels.current, R.string.monarch_devicelist_current)
    val blocked = monarchString(labels.blocked, R.string.monarch_devicelist_blocked)
    val badges = listOfNotNull(current.takeIf { device.current }, blocked.takeIf { device.blocked })
    val detail = tokens.line(device.os, device.browser, device.location)
    val lastUsed = monarchString(labels.lastUsed, R.string.monarch_devicelist_last_used)
    val firstSeen = monarchString(labels.firstSeen, R.string.monarch_devicelist_first_seen)
    val times = tokens.line(
        device.lastUsedMillis?.let { "$lastUsed ${tokens.relativeTime(it, now)}" },
        device.firstSeenMillis?.let { "$firstSeen ${tokens.relativeTime(it, now)}" },
    )
    Column(
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = name
            stateDescription = tokens.spokenDetails(badges, detail, times)
        },
        verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(tokens.RowGap),
            verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = name, style = MonarchTheme.typography.bodyStrong, color = colors.textDefault)
            if (device.current) MonarchBadge(text = current, tone = MonarchTone.Success)
            if (device.blocked) MonarchBadge(text = blocked, tone = MonarchTone.Danger)
        }
        listOfNotNull(detail, times).forEach { line ->
            Text(text = line, style = MonarchTheme.typography.caption, color = colors.textMuted)
        }
    }
}

@Composable
private fun DeviceConfirmDialog(
    name: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    labels: MonarchDeviceListLabels,
) {
    MonarchDialog(
        onDismissRequest = onDismiss,
        title = labels.confirmTitle?.invoke(name) ?: stringResource(R.string.monarch_devicelist_confirm_title, name),
        description = monarchString(labels.confirmMessage, R.string.monarch_devicelist_confirm_message),
        actions = {
            MonarchButton(
                text = monarchString(labels.cancel, R.string.monarch_devicelist_cancel),
                onClick = onDismiss,
                variant = MonarchButtonVariant.Ghost,
            )
            MonarchButton(
                text = monarchString(labels.remove, R.string.monarch_devicelist_remove),
                onClick = onConfirm,
                variant = MonarchButtonVariant.Danger,
            )
        },
    )
}

/** The removal in progress, the last failure and announcement, and a way to start a removal. */
private class DeviceListRemoval(
    val removingId: String?,
    val failure: String?,
    val announcement: String,
    val run: (MonarchDevice, String) -> Unit,
)

@Composable
private fun rememberDeviceRemoval(
    onRemove: (suspend (String) -> Unit)?,
    labels: MonarchDeviceListLabels,
): DeviceListRemoval {
    val scope = rememberCoroutineScope()
    val current by rememberUpdatedState(onRemove)
    var removingId by remember { mutableStateOf<String?>(null) }
    var failure by remember { mutableStateOf<String?>(null) }
    var announcement by remember { mutableStateOf("") }
    val removed = stringResource(R.string.monarch_devicelist_removed)
    val failed = stringResource(R.string.monarch_devicelist_remove_failed)
    return DeviceListRemoval(removingId, failure, announcement) { device, unnamed ->
        val remove = current
        if (remove != null && removingId == null) {
            val name = MonarchDeviceListTokens.displayName(device.name, unnamed)
            removingId = device.id
            failure = null
            scope.launch {
                val outcome = runCatching { remove(device.id) }
                removingId = null
                val error = outcome.exceptionOrNull()
                if (error is CancellationException) throw error
                if (error == null) {
                    announcement = labels.removed?.invoke(name) ?: removed.format(name)
                } else {
                    val message = labels.removeFailed?.invoke(name) ?: failed.format(name)
                    failure = message
                    announcement = message
                }
            }
        }
    }
}

/** The reference time: [fixed], or the clock, redrawn as often as the freshest of [times] needs while [running]. */
@Composable
private fun deviceListClock(fixed: Long?, running: Boolean, times: List<Long?>): Long =
    rememberMonarchRelativeClock(times, fixed = fixed, running = running)
