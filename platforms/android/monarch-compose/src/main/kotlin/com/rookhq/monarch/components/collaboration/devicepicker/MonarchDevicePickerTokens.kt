package com.rookhq.monarch.components.collaboration.devicepicker

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.forms.select.MonarchSelectOption
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.tokens.MonarchTokens

/** The notice a device picker shows. */
internal enum class DevicePickerNotice { NotDetermined, Denied, Unavailable }

/** Component tokens and the pure rules of MonarchDevicePicker. */
internal object MonarchDevicePickerTokens {
    /** The value of the System default option: devices with an empty id are left out, so it never clashes. */
    const val SYSTEM_DEFAULT: String = ""

    /** The space between the notice and the fields. */
    fun gap(size: MonarchDevicePickerSize): Dp = when (size) {
        MonarchDevicePickerSize.Sm -> MonarchTokens.Space.space2.dp
        MonarchDevicePickerSize.Md -> MonarchTokens.Space.space4.dp
    }

    /** The size of the notice's button. */
    fun buttonSize(size: MonarchDevicePickerSize): MonarchButtonSize = when (size) {
        MonarchDevicePickerSize.Sm -> MonarchButtonSize.Sm
        MonarchDevicePickerSize.Md -> MonarchButtonSize.Md
    }

    /**
     * The options of one field: System default, then the listed devices (a blank name numbered by position), and
     * a selected device that is no longer listed, numbered after the others so the choice stays visible.
     */
    fun options(
        devices: List<MonarchDevicePickerDevice>,
        selected: String?,
        systemDefault: String,
        unnamed: (Int) -> String,
    ): List<MonarchSelectOption<String>> {
        val listed = devices.filter { it.id.isNotEmpty() }.distinctBy { it.id }
        val named = listed.mapIndexed { index, device ->
            MonarchSelectOption(device.id, device.name.takeIf { it.isNotBlank() } ?: unnamed(index + 1))
        }
        val missing = selected?.takeIf { id -> id.isNotEmpty() && listed.none { it.id == id } }
            ?.let { listOf(MonarchSelectOption(it, unnamed(listed.size + 1))) }
            .orEmpty()
        return listOf(MonarchSelectOption(SYSTEM_DEFAULT, systemDefault)) + named + missing
    }

    /** The value a field shows: the selected id, or System default for `null` or an empty id. */
    fun selection(selected: String?): String = selected?.takeIf { it.isNotEmpty() } ?: SYSTEM_DEFAULT

    /** What the field reports for an option: `null` for System default. */
    fun choice(value: String): String? = value.takeIf { it != SYSTEM_DEFAULT }

    /**
     * The notice to show: the access state when it is not granted; with access, Unavailable when no shown field
     * that needs it has a device; otherwise none.
     */
    fun notice(access: MonarchDevicePickerAccess, inputsShown: Boolean, anyInput: Boolean): DevicePickerNotice? =
        when (access) {
            MonarchDevicePickerAccess.NotDetermined -> DevicePickerNotice.NotDetermined
            MonarchDevicePickerAccess.Denied -> DevicePickerNotice.Denied
            MonarchDevicePickerAccess.Unavailable -> DevicePickerNotice.Unavailable
            MonarchDevicePickerAccess.Granted ->
                if (inputsShown && !anyInput) DevicePickerNotice.Unavailable else null
        }

    /** The tone of a notice: info before asking, warning when refused, neutral when there is nothing to choose. */
    fun tone(notice: DevicePickerNotice): MonarchTone = when (notice) {
        DevicePickerNotice.NotDetermined -> MonarchTone.Info
        DevicePickerNotice.Denied -> MonarchTone.Warning
        DevicePickerNotice.Unavailable -> MonarchTone.Neutral
    }

    /** Whether the microphone and camera fields can be used: only with access. */
    fun inputsEnabled(access: MonarchDevicePickerAccess, enabled: Boolean): Boolean =
        enabled && access == MonarchDevicePickerAccess.Granted
}
