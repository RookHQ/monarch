package com.rookhq.monarch.components.forms.durationinput

/** The largest unit a `MonarchDurationInput` writes. */
public enum class MonarchDurationInputUnit {
    /** 26 hours is written "26h". */
    Hours,

    /** 26 hours is written "1d 2h", with the input's minutes per day. */
    Days,
}
