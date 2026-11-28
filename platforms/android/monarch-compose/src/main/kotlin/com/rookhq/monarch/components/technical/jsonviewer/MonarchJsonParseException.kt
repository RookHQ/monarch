package com.rookhq.monarch.components.technical.jsonviewer

/**
 * Thrown by `MonarchJsonValue.parse` when the text is not valid JSON or is nested deeper than 100 levels.
 *
 * @property offset the position of the problem, in UTF-16 characters from the start of the text.
 */
public class MonarchJsonParseException(public val offset: Int) :
    IllegalArgumentException("Invalid JSON at character $offset")
