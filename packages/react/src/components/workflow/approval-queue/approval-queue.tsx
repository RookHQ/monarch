'use client';

import {
  forwardRef,
  useEffect,
  useRef,
  useId,
  useMemo,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { CheckGlyph, CloseGlyph, EyeGlyph, SearchGlyph } from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';
import { Badge } from '../../data-display/badge/index.js';
import { RelativeTime, type DateInput } from '../../data-display/relative-time/index.js';
import { EmptyState } from '../../feedback/empty-state/index.js';
import { Checkbox } from '../../forms/checkbox/index.js';
import { Input } from '../../forms/text-field/index.js';
import { Avatar } from '../../identity/avatar/index.js';
import { useConfirm } from '../../overlays/confirm-dialog/index.js';

/** One request waiting for a decision. */
export interface ApprovalQueueItem {
  /** Stable, unique id. */
  id: string;
  /** The person who asked. */
  requester: {
    name: string;
    avatarUrl?: string | null | undefined;
    /** Second line under the name, e.g. a team or an employee code. */
    detail?: ReactNode;
  };
  /** What is being asked, e.g. "Expense claim" or "Leave, 3 days". */
  title: ReactNode;
  /** Short facts such as an amount or a period. */
  summary?: ReactNode;
  /** When it was submitted. */
  submittedAt?: DateInput | undefined;
  /** Past its deadline: shows an overdue badge. */
  overdue?: boolean | undefined;
  /** Extra badges, e.g. "Resubmitted". */
  badges?: ReactNode;
  /** Plain text used by the search box. Default: the requester's name and a text `title`. */
  searchText?: string | undefined;
  /** False when the viewer can see but not decide this request. Default true. */
  canDecide?: boolean | undefined;
}

/** Labels. Override them to translate. */
export interface ApprovalQueueMessages {
  /** Accessible name of the list. */
  label: string;
  approve: string;
  reject: string;
  review: string;
  /** Accessible names that say which request a button acts on. */
  approveItem: (requester: string) => string;
  rejectItem: (requester: string) => string;
  reviewItem: (requester: string) => string;
  selectItem: (requester: string) => string;
  selectAll: string;
  selected: (count: number) => string;
  approveSelected: string;
  overdue: string;
  search: string;
  searchPlaceholder: string;
  emptyTitle: string;
  emptyDescription: string;
  noMatchesTitle: string;
  rejectTitle: (requester: string) => string;
  rejectDescription: string;
  rejectReason: string;
  rejectReasonPlaceholder: string;
  bulkTitle: (count: number) => string;
  bulkDescription: string;
  bulkConfirm: string;
  /** Announced after a decision. */
  approved: (requester: string) => string;
  rejected: (requester: string) => string;
  bulkApproved: (count: number) => string;
  /** Shown on a row whose decision failed. */
  failed: string;
}

const DEFAULT_MESSAGES: ApprovalQueueMessages = {
  label: 'Requests waiting for you',
  approve: 'Approve',
  reject: 'Send back',
  review: 'Review',
  approveItem: (r) => `Approve request from ${r}`,
  rejectItem: (r) => `Send back request from ${r}`,
  reviewItem: (r) => `Review request from ${r}`,
  selectItem: (r) => `Select request from ${r}`,
  selectAll: 'Select all',
  selected: (n) => `${n} selected`,
  approveSelected: 'Approve selected',
  overdue: 'Past due',
  search: 'Search requests',
  searchPlaceholder: 'Search by name',
  emptyTitle: 'Nothing to review',
  emptyDescription: 'Requests that need your decision show up here.',
  noMatchesTitle: 'No requests match your search',
  rejectTitle: (r) => `Send back the request from ${r}?`,
  rejectDescription: 'They will be told it was not approved and can change it and resubmit.',
  rejectReason: 'Reason',
  rejectReasonPlaceholder: 'Let them know what to change',
  bulkTitle: (n) => `Approve ${n} ${n === 1 ? 'request' : 'requests'}?`,
  bulkDescription: 'Each request moves to its next step, or is approved if this was the last one.',
  bulkConfirm: 'Approve all',
  approved: (r) => `Approved the request from ${r}.`,
  rejected: (r) => `Sent back the request from ${r}.`,
  bulkApproved: (n) => `Approved ${n} ${n === 1 ? 'request' : 'requests'}.`,
  failed: 'That did not work. Try again.',
};

export interface ApprovalQueueProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The requests, in the order to show them. Remove an item once it is decided. */
  items: readonly ApprovalQueueItem[];
  /** Approves one request. Return a promise to show a pending state until it settles. */
  onApprove?: ((item: ApprovalQueueItem) => unknown) | undefined;
  /**
   * Sends one request back with the reason from the confirmation dialog.
   * Return a promise to keep the dialog pending; a rejection shows its error there.
   */
  onReject?: ((item: ApprovalQueueItem, reason: string | undefined) => unknown) | undefined;
  /** Opens the full request. Adds a Review button. */
  onReview?: ((item: ApprovalQueueItem) => void) | undefined;
  /** Approves several selected requests after a confirmation. Adds selection checkboxes. */
  onApproveMany?: ((items: ApprovalQueueItem[]) => unknown) | undefined;
  /** Whether a reason is needed to send a request back. Default `required`. */
  rejectReason?: 'required' | 'optional' | 'none' | undefined;
  /** Show a search box that filters by requester. Default false. */
  searchable?: boolean | undefined;
  /** Show at most this many rows (for a dashboard card); put a "See all" link in `footer`. */
  limit?: number | undefined;
  /** Content under the list, such as a "See all requests" link. */
  footer?: ReactNode;
  /** Reference time for relative times; pass it for identical server rendering. */
  now?: DateInput | undefined;
  /** BCP 47 locale for times. */
  locale?: string | undefined;
  /** Override labels. */
  messages?: Partial<ApprovalQueueMessages> | undefined;
}

const textOf = (node: ReactNode) =>
  typeof node === 'string' || typeof node === 'number' ? String(node) : '';

/**
 * A list of requests waiting on the viewer, with approve, send back (with a
 * reason) and review actions, optional search and bulk approval. The product
 * performs each decision and removes decided requests from `items`.
 */
export const ApprovalQueue = forwardRef<HTMLDivElement, ApprovalQueueProps>(function ApprovalQueue(
  {
    items,
    onApprove,
    onReject,
    onReview,
    onApproveMany,
    rejectReason = 'required',
    searchable = false,
    limit,
    footer,
    now,
    locale,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_MESSAGES, ...messagesProp };
  const id = useId();
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState<ReadonlySet<string>>(new Set());
  const [busy, setBusy] = useState<ReadonlySet<string>>(new Set());
  const [failed, setFailed] = useState<ReadonlySet<string>>(new Set());
  const { confirm, element: dialog } = useConfirm();
  const rootRef = useRef<HTMLDivElement | null>(null);
  const opener = useRef<Element | null>(null);
  const setRoot = (node: HTMLDivElement | null) => {
    rootRef.current = node;
    if (typeof ref === 'function') ref(node);
    else if (ref) ref.current = node;
  };
  // A decided row usually disappears with its focused button: keep focus in the queue.
  const rescueFocus = () => {
    const node = opener.current;
    if (!node || node.isConnected) return;
    opener.current = null;
    const active = document.activeElement;
    if (!active || active === document.body) rootRef.current?.focus();
  };
  useEffect(rescueFocus);
  // When the button that opened a dialog is gone, focus the queue instead of the page.
  const keepFocus = () => {
    const opener = document.activeElement;
    return (event: Event) => {
      if (opener?.isConnected) return;
      event.preventDefault();
      rootRef.current?.focus();
    };
  };
  const [announcement, setAnnouncement] = useState('');
  const selectable = !!onApproveMany;

  const visible = useMemo(() => {
    const q = query.trim().toLowerCase();
    const matched = q
      ? items.filter((it) =>
          (it.searchText ?? `${it.requester.name} ${textOf(it.title)}`).toLowerCase().includes(q),
        )
      : items;
    return limit !== undefined ? matched.slice(0, limit) : matched;
  }, [items, query, limit]);

  // Selection only ever names items still in the list.
  const live = new Set(items.map((i) => i.id));
  const chosen = items.filter((i) => selected.has(i.id) && i.canDecide !== false);
  const decidable = visible.filter((i) => i.canDecide !== false);
  const allChecked = decidable.length > 0 && decidable.every((i) => selected.has(i.id));
  const someChecked = decidable.some((i) => selected.has(i.id));

  const mark = (set: typeof setBusy, key: string, on: boolean) =>
    set((s) => {
      const n = new Set(s);
      if (on) n.add(key);
      else n.delete(key);
      return n;
    });

  async function approve(item: ApprovalQueueItem) {
    if (!onApprove) return;
    const button = typeof document !== 'undefined' ? document.activeElement : null;
    mark(setBusy, item.id, true);
    mark(setFailed, item.id, false);
    try {
      await onApprove(item);
      opener.current = button;
      rescueFocus();
      setAnnouncement(m.approved(item.requester.name));
      mark(setSelected, item.id, false);
    } catch {
      mark(setFailed, item.id, true);
    } finally {
      mark(setBusy, item.id, false);
    }
  }

  function reject(item: ApprovalQueueItem) {
    if (!onReject) return;
    void confirm({
      tone: 'danger',
      title: m.rejectTitle(item.requester.name),
      description: m.rejectDescription,
      confirmLabel: m.reject,
      ...(rejectReason !== 'none'
        ? {
            reason: {
              label: m.rejectReason,
              placeholder: m.rejectReasonPlaceholder,
              required: rejectReason === 'required',
            },
            initialFocus: 'field' as const,
          }
        : {}),
      onCloseAutoFocus: keepFocus(),
      onConfirm: async ({ reason }) => {
        mark(setFailed, item.id, false);
        await onReject(item, reason);
        setAnnouncement(m.rejected(item.requester.name));
        mark(setSelected, item.id, false);
      },
    });
  }

  function approveMany() {
    if (!onApproveMany) return;
    const list = chosen;
    void confirm({
      title: m.bulkTitle(list.length),
      description: m.bulkDescription,
      confirmLabel: m.bulkConfirm,
      onCloseAutoFocus: keepFocus(),
      onConfirm: async () => {
        await onApproveMany(list);
        setAnnouncement(m.bulkApproved(list.length));
        setSelected(new Set());
      },
    });
  }

  const toolbar = searchable || selectable;

  return (
    <div ref={setRoot} tabIndex={-1} {...rest} className={cx('mn-approval-queue', className)}>
      {toolbar && items.length > 0 ? (
        <div className="mn-approval-queue__toolbar">
          {searchable ? (
            <div className="mn-approval-queue__search">
              <SearchGlyph />
              <Input
                type="search"
                aria-label={m.search}
                placeholder={m.searchPlaceholder}
                value={query}
                onChange={(e) => setQuery(e.target.value)}
              />
            </div>
          ) : null}
          {selectable ? (
            <div className="mn-approval-queue__bulk">
              <Checkbox
                label={m.selectAll}
                checked={allChecked ? true : someChecked ? 'indeterminate' : false}
                disabled={decidable.length === 0}
                onCheckedChange={(v) =>
                  setSelected(v === true ? new Set(decidable.map((i) => i.id)) : new Set())
                }
              />
              <span className="mn-approval-queue__count" aria-live="polite">
                {m.selected(chosen.length)}
              </span>
              <Button
                size="sm"
                variant="success"
                leadingIcon={<CheckGlyph />}
                disabled={chosen.length === 0}
                onClick={approveMany}
              >
                {m.approveSelected}
              </Button>
            </div>
          ) : null}
        </div>
      ) : null}

      {items.length === 0 ? (
        <EmptyState
          size="compact"
          kind="success"
          headingLevel="p"
          icon={<CheckGlyph />}
          title={m.emptyTitle}
          description={m.emptyDescription}
        />
      ) : visible.length === 0 ? (
        <EmptyState
          size="compact"
          kind="filtered"
          headingLevel="p"
          icon={<SearchGlyph />}
          title={m.noMatchesTitle}
          onClearFilters={() => setQuery('')}
        />
      ) : (
        <ul className="mn-approval-queue__list" aria-label={m.label}>
          {visible.map((item) => {
            const name = item.requester.name;
            const isBusy = busy.has(item.id);
            const canDecide = item.canDecide !== false;
            const nameId = `${id}-${item.id}-name`;
            return (
              <li
                key={item.id}
                className="mn-approval-queue__item"
                data-selected={
                  (selectable && selected.has(item.id) && live.has(item.id)) || undefined
                }
                data-overdue={item.overdue || undefined}
                data-busy={isBusy || undefined}
              >
                {selectable ? (
                  <Checkbox
                    className="mn-approval-queue__select"
                    aria-label={m.selectItem(name)}
                    checked={selected.has(item.id)}
                    disabled={!canDecide}
                    onCheckedChange={(v) => mark(setSelected, item.id, v === true)}
                  />
                ) : null}
                <Avatar name={name} src={item.requester.avatarUrl ?? null} size="sm" decorative />
                <div className="mn-approval-queue__text">
                  <p id={nameId} className="mn-approval-queue__name">
                    {name}
                  </p>
                  {item.requester.detail ? (
                    <p className="mn-approval-queue__detail">{item.requester.detail}</p>
                  ) : null}
                  <p className="mn-approval-queue__meta">
                    <span className="mn-approval-queue__title">{item.title}</span>
                    {item.submittedAt !== undefined ? (
                      <>
                        <span aria-hidden>·</span>
                        <RelativeTime
                          date={item.submittedAt}
                          {...(now !== undefined ? { now } : {})}
                          {...(locale ? { locale } : {})}
                          tooltip={false}
                        />
                      </>
                    ) : null}
                    {item.overdue ? (
                      <Badge tone="danger" size="sm">
                        {m.overdue}
                      </Badge>
                    ) : null}
                    {item.badges}
                  </p>
                  {item.summary ? (
                    <p className="mn-approval-queue__summary">{item.summary}</p>
                  ) : null}
                  {failed.has(item.id) ? (
                    <p className="mn-approval-queue__error" role="alert">
                      {m.failed}
                    </p>
                  ) : null}
                </div>
                <div className="mn-approval-queue__actions">
                  {onReview ? (
                    <Button
                      size="sm"
                      variant="ghost"
                      leadingIcon={<EyeGlyph />}
                      aria-label={m.reviewItem(name)}
                      onClick={() => onReview(item)}
                    >
                      {m.review}
                    </Button>
                  ) : null}
                  {onReject && canDecide ? (
                    <Button
                      size="sm"
                      variant="secondary"
                      leadingIcon={<CloseGlyph />}
                      aria-label={m.rejectItem(name)}
                      disabled={isBusy}
                      onClick={() => reject(item)}
                    >
                      {m.reject}
                    </Button>
                  ) : null}
                  {onApprove && canDecide ? (
                    <Button
                      size="sm"
                      variant="success"
                      leadingIcon={<CheckGlyph />}
                      aria-label={m.approveItem(name)}
                      loading={isBusy}
                      onClick={() => void approve(item)}
                    >
                      {m.approve}
                    </Button>
                  ) : null}
                </div>
              </li>
            );
          })}
        </ul>
      )}
      {footer ? <div className="mn-approval-queue__footer">{footer}</div> : null}
      <span className="mn-visually-hidden" role="status" aria-live="polite">
        {announcement}
      </span>

      {dialog}
    </div>
  );
});
