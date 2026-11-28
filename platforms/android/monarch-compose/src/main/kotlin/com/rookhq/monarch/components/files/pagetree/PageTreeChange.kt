package com.rookhq.monarch.components.files.pagetree

import java.util.Locale

/** A new order of the groups and what moved. */
internal class PageTreeChange(val groups: List<MonarchPageTreeGroup>, val move: MonarchPageTreeMove)

/** The running number of every page across the groups, from 1. */
internal fun pageTreeNumbers(groups: List<MonarchPageTreeGroup>): Map<String, Int> =
    groups.flatMap { it.pages }.mapIndexed { index, page -> page.id to index + 1 }.toMap()

/** The default page number: two digits at least, in the digits of [locale]. */
internal fun pageTreeNumber(value: Int, locale: Locale): String = String.format(locale, "%02d", value)

/** The shown title: the title, or [untitled] when it is blank. */
internal fun pageTreeTitle(page: MonarchPageTreePage, untitled: String): String =
    page.title.trim().ifEmpty { untitled }

/** The number of pages in every group. */
internal fun pageTreeTotalPages(groups: List<MonarchPageTreeGroup>): Int = groups.sumOf { it.pages.size }

/** A page can be deleted while it is not the last page of the document. */
internal fun pageTreeCanDeletePage(groups: List<MonarchPageTreeGroup>): Boolean = pageTreeTotalPages(groups) > 1

/** A group can be deleted while it is not the only group. */
internal fun pageTreeCanDeleteGroup(groups: List<MonarchPageTreeGroup>): Boolean = groups.size > 1

/** Moves a page inside its group from [from] to [to]; `null` when nothing changes. */
internal fun pageTreeMovePageWithin(
    groups: List<MonarchPageTreeGroup>,
    groupId: String,
    from: Int,
    to: Int,
): PageTreeChange? {
    val group = groups.firstOrNull { it.id == groupId } ?: return null
    if (from == to || from !in group.pages.indices || to !in group.pages.indices) return null
    val pages = group.pages.toMutableList()
    val page = pages.removeAt(from)
    pages.add(to, page)
    val next = groups.map { if (it.id == groupId) MonarchPageTreeGroup(it.id, it.label, pages) else it }
    return PageTreeChange(next, MonarchPageTreeMove(MonarchPageTreeMoveKind.Page, page.id, groupId, groupId, to))
}

/** Moves a page to the end of another group; `null` when either is missing or they are the same. */
internal fun pageTreeMovePageTo(
    groups: List<MonarchPageTreeGroup>,
    pageId: String,
    toGroupId: String,
): PageTreeChange? {
    val from = groups.firstOrNull { group -> group.pages.any { it.id == pageId } }
    val to = groups.firstOrNull { it.id == toGroupId }
    if (from == null || to == null || from.id == to.id) return null
    val page = from.pages.first { it.id == pageId }
    val next = groups.map {
        when (it.id) {
            from.id -> MonarchPageTreeGroup(it.id, it.label, it.pages - page)
            to.id -> MonarchPageTreeGroup(it.id, it.label, it.pages + page)
            else -> it
        }
    }
    val move = MonarchPageTreeMove(MonarchPageTreeMoveKind.Page, pageId, from.id, to.id, to.pages.size)
    return PageTreeChange(next, move)
}

/** Moves a group by [delta] places; `null` when it would leave the list. */
internal fun pageTreeMoveGroup(groups: List<MonarchPageTreeGroup>, groupId: String, delta: Int): PageTreeChange? {
    val from = groups.indexOfFirst { it.id == groupId }
    val to = from + delta
    if (from < 0 || to !in groups.indices || delta == 0) return null
    val next = groups.toMutableList()
    next.add(to, next.removeAt(from))
    return PageTreeChange(next, MonarchPageTreeMove(MonarchPageTreeMoveKind.Group, groupId, null, null, to))
}

/** Whether a typed name renames the group: not blank and different from [current]. */
internal fun pageTreeRenamed(name: String, current: String): String? =
    name.trim().takeIf { it.isNotEmpty() && it != current }
