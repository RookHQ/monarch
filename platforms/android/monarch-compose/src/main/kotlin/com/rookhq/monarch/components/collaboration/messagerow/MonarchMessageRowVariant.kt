package com.rookhq.monarch.components.collaboration.messagerow

/** The kind of a `MonarchMessageRow`. */
public enum class MonarchMessageRowVariant {
    /** A message in the conversation. */
    Standard,

    /** A note only teammates can see: a warning tint and a bar along the start edge. */
    Internal,

    /** An event written by the server: a centered caption with the time, without avatar, author or actions. */
    System,
}
