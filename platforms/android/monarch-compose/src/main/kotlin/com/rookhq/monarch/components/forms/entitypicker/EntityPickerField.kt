package com.rookhq.monarch.components.forms.entitypicker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.iconbutton.MonarchIconButton
import com.rookhq.monarch.components.forms.formfield.MonarchFormField
import com.rookhq.monarch.components.identity.avatar.MonarchAvatar
import com.rookhq.monarch.components.identity.avatar.MonarchAvatarSize
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.monarchSurface
import com.rookhq.monarch.theme.MonarchTheme

/** The field of an entity picker inside a form field, and its sheet while open. */
@Composable
internal fun EntityPickerField(
    config: EntityPickerConfig,
    known: SnapshotStateMap<String, MonarchEntityPickerEntity>,
    onChange: (List<String>) -> Unit,
    modifier: Modifier,
) {
    val tokens = MonarchEntityPickerTokens
    val colors = MonarchTheme.colors
    val labels = config.labels
    val locale = LocalConfiguration.current.locales[0]
    var expanded by rememberSaveable { mutableStateOf(config.initiallyOpen) }
    val chosen = config.selected.mapNotNull { id ->
        config.selectedEntities.firstOrNull { it.id == id } ?: config.entities.firstOrNull { it.id == id } ?: known[id]
    }
    val summaryFormat = stringResource(R.string.monarch_entitypicker_summary)
    val summary = EntityPickerSelection.summary(chosen.map { it.name }) { first, others ->
        labels.summary?.invoke(first, others) ?: String.format(locale, summaryFormat, first, others)
    }
    val text = summary ?: config.placeholder ?: monarchString(labels.select, R.string.monarch_entitypicker_select)
    val none = monarchString(labels.none, R.string.monarch_entitypicker_none)
    val clearName = monarchString(labels.clear, R.string.monarch_entitypicker_clear)
    val enabled = config.enabled
    MonarchFormField(
        label = config.label,
        modifier = modifier,
        enabled = enabled,
        required = config.required,
        helperText = config.helperText,
        errorText = config.errorText,
    ) { controlModifier ->
        val shape = MonarchShapeDefaults.Control
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = tokens.MinHeight)
                .monarchSurface(tokens.fieldColors(config.errorText != null, enabled, colors), shape),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = controlModifier
                    .weight(1f)
                    .heightIn(min = tokens.MinHeight)
                    .clip(shape)
                    .clickable(enabled = enabled, role = Role.Button) { expanded = true }
                    .semantics { stateDescription = chosen.joinToString(", ") { it.name }.ifEmpty { none } }
                    .padding(horizontal = tokens.FieldPadding),
                horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                chosen.firstOrNull()?.let { first -> EntityPickerMark(first) }
                Text(
                    text = text,
                    modifier = Modifier.clearAndSetSemantics { },
                    style = MonarchTheme.typography.body,
                    color = tokens.valueColor(summary != null, enabled, colors),
                )
            }
            val clearIcon = config.clearIcon.takeIf {
                chosen.isNotEmpty() && enabled && config.clearOptionLabel == null
            }
            EntityPickerTrailing(clearIcon, clearName) { onChange(emptyList()) }
        }
    }
    if (expanded && enabled) {
        EntityPickerSheet(
            config = config,
            onChoose = { entity ->
                known[entity.id] = entity
                if (config.multiple) {
                    onChange(EntityPickerSelection.toggle(config.selected, entity.id))
                } else {
                    onChange(listOf(entity.id))
                    expanded = false
                }
            },
            onClear = {
                onChange(emptyList())
                expanded = false
            },
            onClose = { expanded = false },
        )
    }
}

/** The glyph of an entity kind when it has one, else its avatar. */
@Composable
internal fun EntityPickerMark(entity: MonarchEntityPickerEntity) {
    val icon = entity.icon
    if (icon != null) {
        Box(Modifier.clearAndSetSemantics { }) { icon() }
    } else {
        MonarchAvatar(
            image = entity.image,
            name = entity.name,
            shape = entity.shape,
            size = MonarchAvatarSize.Sm,
            status = entity.status,
            decorative = true,
        )
    }
}

/**
 * The end of the field: a clear button while a value is chosen and there is no clearing row in the list, else
 * the chevron of a list field, as the web draws it.
 */
@Composable
private fun EntityPickerTrailing(clearIcon: (@Composable () -> Unit)?, clearName: String, onClear: () -> Unit) {
    if (clearIcon != null) {
        MonarchIconButton(label = clearName, onClick = onClear, size = MonarchButtonSize.Sm) {
            Box(Modifier.clearAndSetSemantics { }, content = { clearIcon() })
        }
    } else {
        Box(Modifier.padding(end = MonarchEntityPickerTokens.FieldPadding).clearAndSetSemantics { }) {
            MonarchDefaultIcons.ChevronDown()
        }
    }
}
