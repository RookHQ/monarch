'use client';

import {
  forwardRef,
  useId,
  useState,
  type HTMLAttributes,
  type MouseEvent as ReactMouseEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { ReorderableList } from '../../interaction/reorderable-list/index.js';
import { useConfirm } from '../../overlays/confirm-dialog/index.js';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '../../overlays/dropdown-menu/index.js';
import { usePrompt } from '../../overlays/prompt-dialog/index.js';

/** One page in a group. */
export interface PageTreePage {
  id: string;
  title: string;
  /** Where the page opens. Rendered as a link (through `renderLink` if given). */
  href?: string | undefined;
}

/** A named group of pages (a section or chapter). */
export interface PageTreeGroup {
  id: string;
  label: string;
  pages: PageTreePage[];
}

/** What changed in a reorder. */
export interface PageTreeMove {
  kind: 'page' | 'group';
  id: string;
  /** For pages: the group it left and the group it joined. */
  fromGroupId?: string | undefined;
  toGroupId?: string | undefined;
  /** New index within its group (pages) or among groups (groups). */
  toIndex: number;
}

/** Visible and announced strings. Override them to translate. */
export interface PageTreeMessages {
  label: string;
  untitled: string;
  number: (n: number) => string;
  addPageTo: (group: string) => string;
  addFirstPage: string;
  noPages: string;
  addGroup: string;
  groupActions: (group: string) => string;
  pageActions: (page: string) => string;
  renameGroup: string;
  moveGroupUp: string;
  moveGroupDown: string;
  deleteGroup: string;
  moveTo: string;
  deletePage: string;
  renameTitle: string;
  addGroupTitle: string;
  groupNameLabel: string;
  newGroupName: string;
  renameSubmit: string;
  addGroupSubmit: string;
  deleteGroupTitle: (group: string) => string;
  deleteGroupConsequence: (pages: number) => string;
  deletePageTitle: (page: string) => string;
  cannotUndo: string;
  deleteGroupConfirm: string;
  deletePageConfirm: string;
  collapse: string;
  expand: string;
  groupMoved: (group: string, position: number, total: number) => string;
  pageMovedTo: (page: string, group: string) => string;
}

export const DEFAULT_PAGE_TREE_MESSAGES: PageTreeMessages = {
  label: 'Pages',
  untitled: 'Untitled page',
  number: (n) => String(n).padStart(2, '0'),
  addPageTo: (group) => `Add page to ${group}`,
  addFirstPage: 'Add first page',
  noPages: 'No pages yet.',
  addGroup: 'Add group',
  groupActions: (group) => `Actions for ${group}`,
  pageActions: (page) => `Actions for ${page}`,
  renameGroup: 'Rename group',
  moveGroupUp: 'Move group up',
  moveGroupDown: 'Move group down',
  deleteGroup: 'Delete group',
  moveTo: 'Move to',
  deletePage: 'Delete page',
  renameTitle: 'Rename group',
  addGroupTitle: 'Add a group',
  groupNameLabel: 'Group name',
  newGroupName: 'New group',
  renameSubmit: 'Rename',
  addGroupSubmit: 'Add group',
  deleteGroupTitle: (group) => `Delete the “${group}” group?`,
  deleteGroupConsequence: (n) => `Its ${n === 1 ? 'page is' : `${n} pages are`} deleted too`,
  deletePageTitle: (page) => `Delete “${page}”?`,
  cannotUndo: 'This cannot be undone.',
  deleteGroupConfirm: 'Delete group',
  deletePageConfirm: 'Delete page',
  collapse: 'Hide pages',
  expand: 'Show pages',
  groupMoved: (group, p, n) => `${group} moved to position ${p} of ${n}.`,
  pageMovedTo: (page, group) => `${page} moved to ${group}.`,
};

export interface PageTreeProps extends Omit<HTMLAttributes<HTMLElement>, 'onSelect'> {
  /** Groups and their pages, in order. */
  groups: PageTreeGroup[];
  /** The page being viewed: marked `aria-current="page"`. */
  activePageId?: string | null | undefined;
  /** Called when a page is chosen (also for linked pages, before navigation). */
  onSelectPage?: ((pageId: string) => void) | undefined;
  /** Renders page links, for example with the router's Link. Default: `<a href>`. */
  renderLink?:
    | ((
        page: PageTreePage,
        props: {
          className: string;
          'aria-current'?: 'page';
          onClick: (e: ReactMouseEvent) => void;
          children: ReactNode;
        },
      ) => ReactNode)
    | undefined;
  /** Shows page numbers (01, 02, …) running across groups. Default true. */
  numbered?: boolean | undefined;
  /**
   * Makes the tree editable: pages reorder within a group by drag or
   * keyboard, move between groups from their menu, and groups move up and
   * down. Called with the new groups; update `groups` and persist.
   */
  onReorder?: ((groups: PageTreeGroup[], move: PageTreeMove) => void) | undefined;
  /** Shows "Add page" on each group. */
  onAddPage?: ((groupId: string) => void | Promise<void>) | undefined;
  /** Shows "Add group"; called with the name from a prompt. */
  onAddGroup?: ((label: string) => void | Promise<void>) | undefined;
  /** Shows "Rename group"; called with the name from a prompt. */
  onRenameGroup?: ((groupId: string, label: string) => void | Promise<void>) | undefined;
  /** Shows "Delete group" behind a confirmation (type the name when it has pages). */
  onDeleteGroup?: ((groupId: string) => void | Promise<void>) | undefined;
  /** Shows "Delete page" behind a confirmation. Hidden when only one page is left. */
  onDeletePage?: ((pageId: string) => void | Promise<void>) | undefined;
  /** Collapsed to a single "Show pages" button. */
  collapsed?: boolean | undefined;
  /** Shows the hide/show toggle. */
  onCollapsedChange?: ((collapsed: boolean) => void) | undefined;
  /** Content under the list, for example "All documents" and "Edit" links. */
  footer?: ReactNode;
  /** Override visible and announced strings. */
  messages?: Partial<PageTreeMessages> | undefined;
}

function PanelGlyph({ open }: { open: boolean }) {
  return (
    <svg
      viewBox="0 0 24 24"
      width="16"
      height="16"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      <rect x="3" y="3" width="18" height="18" rx="2" />
      <path d="M9 3v18" />
      <path d={open ? 'M16 15l-3-3 3-3' : 'M14 9l3 3-3 3'} />
    </svg>
  );
}
function MoreGlyph() {
  return (
    <svg
      viewBox="0 0 24 24"
      width="16"
      height="16"
      fill="currentColor"
      aria-hidden="true"
      focusable="false"
    >
      <circle cx="5" cy="12" r="1.6" />
      <circle cx="12" cy="12" r="1.6" />
      <circle cx="19" cy="12" r="1.6" />
    </svg>
  );
}
function PlusGlyph() {
  return (
    <svg
      viewBox="0 0 24 24"
      width="16"
      height="16"
      fill="none"
      stroke="currentColor"
      strokeWidth={2}
      strokeLinecap="round"
      aria-hidden="true"
      focusable="false"
    >
      <path d="M12 5v14M5 12h14" />
    </svg>
  );
}

const clone = (groups: PageTreeGroup[]) => groups.map((g) => ({ ...g, pages: [...g.pages] }));

/**
 * The pages of a multi-page document, in named groups, with running page
 * numbers. Read-only it is navigation; with callbacks it becomes the
 * organiser: reorder pages by drag or keyboard, move them between groups,
 * reorder, rename and delete groups (with confirmation), add pages and groups.
 */
export const PageTree = forwardRef<HTMLElement, PageTreeProps>(function PageTree(
  {
    groups,
    activePageId,
    onSelectPage,
    renderLink,
    numbered = true,
    onReorder,
    onAddPage,
    onAddGroup,
    onRenameGroup,
    onDeleteGroup,
    onDeletePage,
    collapsed = false,
    onCollapsedChange,
    footer,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_PAGE_TREE_MESSAGES, ...messagesProp };
  const id = useId();
  const { confirm, element: confirmElement } = useConfirm();
  const { prompt, element: promptElement } = usePrompt();
  const [announce, setAnnounce] = useState('');
  const editable = !!onReorder;
  const totalPages = groups.reduce((n, g) => n + g.pages.length, 0);
  const numberOf = new Map<string, number>();
  groups.flatMap((g) => g.pages).forEach((p, i) => numberOf.set(p.id, i + 1));
  const titleOf = (p: PageTreePage) => p.title.trim() || m.untitled;

  const toggle = onCollapsedChange ? (
    <IconButton
      size="sm"
      label={collapsed ? m.expand : m.collapse}
      aria-expanded={!collapsed}
      icon={<PanelGlyph open={!collapsed} />}
      onClick={() => onCollapsedChange(!collapsed)}
      className="mn-page-tree__toggle"
    />
  ) : null;

  if (collapsed && onCollapsedChange) {
    return (
      <nav
        ref={ref}
        aria-label={m.label}
        {...rest}
        data-collapsed=""
        className={cx('mn-page-tree', className)}
      >
        {toggle}
      </nav>
    );
  }

  const pageLink = (page: PageTreePage) => {
    const current = page.id === activePageId;
    const children = (
      <>
        {numbered ? (
          <span className="mn-page-tree__number" aria-hidden="true">
            {m.number(numberOf.get(page.id) ?? 0)}
          </span>
        ) : null}
        <span className="mn-page-tree__title">{titleOf(page)}</span>
      </>
    );
    const onClick = () => onSelectPage?.(page.id);
    const common = {
      className: 'mn-page-tree__page',
      ...(current ? { 'aria-current': 'page' as const } : {}),
      onClick,
      children,
    };
    if (page.href && renderLink) return renderLink(page, common);
    if (page.href)
      return (
        <a href={page.href} {...common}>
          {children}
        </a>
      );
    return (
      <button type="button" {...common}>
        {children}
      </button>
    );
  };

  const movePage = (pageId: string, fromGroupId: string, toGroupId: string) => {
    if (!onReorder) return;
    const next = clone(groups);
    const from = next.find((g) => g.id === fromGroupId);
    const to = next.find((g) => g.id === toGroupId);
    const index = from?.pages.findIndex((p) => p.id === pageId) ?? -1;
    if (!from || !to || index < 0) return;
    const [page] = from.pages.splice(index, 1);
    if (!page) return;
    to.pages.push(page);
    onReorder(next, {
      kind: 'page',
      id: pageId,
      fromGroupId,
      toGroupId,
      toIndex: to.pages.length - 1,
    });
    setAnnounce(m.pageMovedTo(titleOf(page), to.label));
  };

  const moveGroup = (groupId: string, delta: number) => {
    if (!onReorder) return;
    const next = clone(groups);
    const from = next.findIndex((g) => g.id === groupId);
    const to = from + delta;
    if (from < 0 || to < 0 || to >= next.length) return;
    const [group] = next.splice(from, 1);
    if (!group) return;
    next.splice(to, 0, group);
    onReorder(next, { kind: 'group', id: groupId, toIndex: to });
    setAnnounce(m.groupMoved(group.label, to + 1, next.length));
    // Keep focus on the moved group's menu button.
    window.requestAnimationFrame(() =>
      document.getElementById(`${id}-group-${groupId}-menu`)?.focus(),
    );
  };

  const renameGroup = async (group: PageTreeGroup) => {
    if (!onRenameGroup) return;
    await prompt({
      title: m.renameTitle,
      label: m.groupNameLabel,
      defaultValue: group.label,
      required: true,
      submitLabel: m.renameSubmit,
      onSubmit: async (value) => {
        const label = value.trim();
        if (label && label !== group.label) await onRenameGroup(group.id, label);
      },
    });
  };

  const addGroup = async () => {
    if (!onAddGroup) return;
    await prompt({
      title: m.addGroupTitle,
      label: m.groupNameLabel,
      defaultValue: m.newGroupName,
      required: true,
      submitLabel: m.addGroupSubmit,
      onSubmit: async (value) => {
        const label = value.trim();
        if (label) await onAddGroup(label);
      },
    });
  };

  const deleteGroup = async (group: PageTreeGroup) => {
    if (!onDeleteGroup) return;
    const count = group.pages.length;
    await confirm({
      tone: 'danger',
      title: m.deleteGroupTitle(group.label),
      description: m.cannotUndo,
      ...(count
        ? { consequences: [m.deleteGroupConsequence(count)], confirmPhrase: group.label }
        : {}),
      confirmLabel: m.deleteGroupConfirm,
      onConfirm: () => onDeleteGroup(group.id),
    });
  };

  const deletePage = async (page: PageTreePage) => {
    if (!onDeletePage) return;
    await confirm({
      tone: 'danger',
      title: m.deletePageTitle(titleOf(page)),
      description: m.cannotUndo,
      confirmLabel: m.deletePageConfirm,
      onConfirm: () => onDeletePage(page.id),
    });
  };

  const pageMenu = (page: PageTreePage, group: PageTreeGroup) => {
    const others = groups.filter((g) => g.id !== group.id);
    const canMove = editable && others.length > 0;
    const canDelete = !!onDeletePage && totalPages > 1;
    if (!canMove && !canDelete) return null;
    return (
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <IconButton
            size="xs"
            label={m.pageActions(titleOf(page))}
            icon={<MoreGlyph />}
            className="mn-page-tree__menu"
          />
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          {canMove ? (
            <>
              <DropdownMenuLabel>{m.moveTo}</DropdownMenuLabel>
              {others.map((g) => (
                <DropdownMenuItem key={g.id} onSelect={() => movePage(page.id, group.id, g.id)}>
                  {g.label}
                </DropdownMenuItem>
              ))}
            </>
          ) : null}
          {canMove && canDelete ? <DropdownMenuSeparator /> : null}
          {canDelete ? (
            <DropdownMenuItem tone="danger" onSelect={() => void deletePage(page)}>
              {m.deletePage}
            </DropdownMenuItem>
          ) : null}
        </DropdownMenuContent>
      </DropdownMenu>
    );
  };

  const groupMenu = (group: PageTreeGroup, index: number) => {
    const items: ReactNode[] = [];
    if (onRenameGroup)
      items.push(
        <DropdownMenuItem key="rename" onSelect={() => void renameGroup(group)}>
          {m.renameGroup}
        </DropdownMenuItem>,
      );
    if (editable && index > 0)
      items.push(
        <DropdownMenuItem key="up" onSelect={() => moveGroup(group.id, -1)}>
          {m.moveGroupUp}
        </DropdownMenuItem>,
      );
    if (editable && index < groups.length - 1)
      items.push(
        <DropdownMenuItem key="down" onSelect={() => moveGroup(group.id, 1)}>
          {m.moveGroupDown}
        </DropdownMenuItem>,
      );
    if (onDeleteGroup && groups.length > 1) {
      if (items.length) items.push(<DropdownMenuSeparator key="sep" />);
      items.push(
        <DropdownMenuItem key="delete" tone="danger" onSelect={() => void deleteGroup(group)}>
          {m.deleteGroup}
        </DropdownMenuItem>,
      );
    }
    if (!items.length) return null;
    return (
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <IconButton
            id={`${id}-group-${group.id}-menu`}
            size="xs"
            label={m.groupActions(group.label)}
            icon={<MoreGlyph />}
            className="mn-page-tree__menu"
          />
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">{items}</DropdownMenuContent>
      </DropdownMenu>
    );
  };

  return (
    <nav
      ref={ref}
      aria-label={m.label}
      {...rest}
      data-editable={editable || undefined}
      className={cx('mn-page-tree', className)}
    >
      {toggle ? (
        <div className="mn-page-tree__head">
          <span className="mn-page-tree__heading" aria-hidden="true">
            {m.label}
          </span>
          {toggle}
        </div>
      ) : null}
      <div className="mn-page-tree__groups">
        {groups.map((group, index) => {
          const labelId = `${id}-group-${group.id}`;
          return (
            <div
              key={group.id}
              role="group"
              aria-labelledby={labelId}
              className="mn-page-tree__group"
            >
              <div className="mn-page-tree__group-head">
                <span id={labelId} className="mn-page-tree__group-label">
                  {group.label}
                </span>
                {editable || onAddPage || onRenameGroup || onDeleteGroup ? (
                  <span className="mn-page-tree__count" aria-hidden="true">
                    {group.pages.length}
                  </span>
                ) : null}
                <span className="mn-page-tree__group-actions">
                  {onAddPage ? (
                    <IconButton
                      size="xs"
                      label={m.addPageTo(group.label)}
                      icon={<PlusGlyph />}
                      className="mn-page-tree__menu"
                      onClick={() => void onAddPage(group.id)}
                    />
                  ) : null}
                  {groupMenu(group, index)}
                </span>
              </div>
              {group.pages.length === 0 ? (
                onAddPage ? (
                  <Button
                    size="sm"
                    variant="ghost"
                    leadingIcon={<PlusGlyph />}
                    className="mn-page-tree__empty"
                    onClick={() => void onAddPage(group.id)}
                  >
                    {m.addFirstPage}
                  </Button>
                ) : (
                  <p className="mn-page-tree__empty">{m.noPages}</p>
                )
              ) : editable ? (
                <ReorderableList
                  items={group.pages}
                  getKey={(p) => p.id}
                  getLabel={titleOf}
                  onReorder={(pages, move) => {
                    const next = clone(groups).map((g) =>
                      g.id === group.id ? { ...g, pages } : g,
                    );
                    onReorder?.(next, {
                      kind: 'page',
                      id: move.item.id,
                      fromGroupId: group.id,
                      toGroupId: group.id,
                      toIndex: move.to,
                    });
                  }}
                  renderItem={(page) => (
                    <span
                      className="mn-page-tree__row"
                      data-active={page.id === activePageId || undefined}
                    >
                      {pageLink(page)}
                      {pageMenu(page, group)}
                    </span>
                  )}
                  className="mn-page-tree__pages"
                />
              ) : (
                <ol className="mn-page-tree__pages">
                  {group.pages.map((page) => (
                    <li
                      key={page.id}
                      className="mn-page-tree__row"
                      data-active={page.id === activePageId || undefined}
                    >
                      {pageLink(page)}
                      {onDeletePage ? pageMenu(page, group) : null}
                    </li>
                  ))}
                </ol>
              )}
            </div>
          );
        })}
      </div>
      {onAddGroup ? (
        <Button
          size="sm"
          variant="ghost"
          leadingIcon={<PlusGlyph />}
          className="mn-page-tree__add-group"
          onClick={() => void addGroup()}
        >
          {m.addGroup}
        </Button>
      ) : null}
      {footer ? <div className="mn-page-tree__footer">{footer}</div> : null}
      <span className="mn-visually-hidden" aria-live="polite">
        {announce}
      </span>
      {confirmElement}
      {promptElement}
    </nav>
  );
});
