package com.rookhq.monarch.components.layout.notificationsbell

/** The form of a `MonarchNotificationsBell`. The same names exist on every platform. */
public enum class MonarchNotificationsBellVariant {
    /** A 48dp icon button with the count on the bell, for a top app bar. The default. */
    Icon,

    /** A row with the bell, the visible title and the count at the end, for a drawer or a menu. */
    Row,
}
