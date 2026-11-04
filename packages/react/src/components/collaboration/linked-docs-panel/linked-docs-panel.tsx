'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type FormEvent,
  type HTMLAttributes,
  type MouseEvent,
  type ReactNode,
} from 'react';
import { Badge } from '../../data-display/badge/index.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { EntityPicker } from '../../forms/entity-picker/index.js';
import { Field, FieldError, FieldLabel } from '../../forms/form-field/index.js';
import { Input } from '../../forms/text-field/index.js';
import { Skeleton } from '../../feedback/skeleton/index.js';
import { type ComboboxOption, type ComboboxSearchContext } from '../../forms/combobox/index.js';
import { cx } from '../../../internal/cx.js';
import { PlusGlyph } from '../../../internal/shell/icons.js';
import { CloseGlyph, DocGlyph, LinkGlyph } from '../../../internal/shell/panel-icons.js';

/** A doc from Rook Dossier, or an external link. */
export type LinkedDocKind = 'doc' | 'link';

export interface LinkedDocItem {
  /** The link's id (unique per row). */
  id: string;
  kind: LinkedDocKind;
  /** For a doc: the Dossier document id, used to leave linked docs out of the picker. */
  docId?: string | undefined;
  title: string;
  /** Where "open" goes. */
  href: string;
  /** Secondary text, e.g. "Updated 2 hours ago". Default for links: the host name. */
  meta?: ReactNode;
  /** Short status, e.g. "Draft". */
  status?: string | undefined;
  /** The Space the doc lives in; drives the filter chips and grouping. */
  space?: { id: string; name: string } | null | undefined;
  /** Replaces the kind's icon. Decorative. */
  icon?: ReactNode;
  /** Open in a new tab. Default true. */
  external?: boolean | undefined;
  /** Allow unlinking this row. Default true. */
  unlinkable?: boolean | undefined;
}

export interface LinkedDocsPanelMessages {
  title: string;
  linkDoc: string;
  addLink: string;
  newDoc: string;
  searchDocs: string;
  noDocs: string;
  empty: string;
  emptyReadOnly: string;
  loading: string;
  filterBySpace: string;
  all: string;
  noSpace: string;
  /** `n` more rows are hidden by the filter. */
  hiddenByFilter: (n: number) => string;
  showAll: string;
  unlink: (title: string) => string;
  unlinkFailed: string;
  linkFailed: string;
  urlLabel: string;
  urlInvalid: string;
  titleLabel: string;
  titleOptional: string;
  add: string;
  cancel: string;
  newTab: string;
  retry: string;
}

const DEFAULT_MESSAGES: LinkedDocsPanelMessages = {
  title: 'Docs',
  linkDoc: 'Link doc',
  addLink: 'Add link',
  newDoc: 'New doc',
  searchDocs: 'Search docs…',
  noDocs: 'No docs match.',
  empty: 'Nothing linked yet. Link a spec, runbook or notes, or add a link to any page.',
  emptyReadOnly: 'Nothing linked yet.',
  loading: 'Loading linked docs…',
  filterBySpace: 'Filter by Space',
  all: 'All',
  noSpace: 'No Space',
  hiddenByFilter: (n) => `${n} more in other Spaces.`,
  showAll: 'Show all',
  unlink: (t) => `Unlink ${t}`,
  unlinkFailed: 'That didn’t unlink. Try again.',
  linkFailed: 'That didn’t link. Try again.',
  urlLabel: 'Link',
  urlInvalid: 'Enter a full web address that starts with https://',
  titleLabel: 'Title',
  titleOptional: 'optional',
  add: 'Add',
  cancel: 'Cancel',
  newTab: 'opens in a new tab',
  retry: 'Try again',
};

const NO_SPACE = '\u0000none';

export interface LinkedDocsPanelProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** What is linked to the record. `null` while loading. */
  items: LinkedDocItem[] | null;
  /** Something went wrong loading or changing links (shown as an alert). */
  error?: ReactNode;
  /** With `error`, shows Try again. */
  onRetry?: (() => void) | undefined;
  /** Replaces the list, e.g. "You don't have access to Rook Dossier in this workspace." */
  notice?: ReactNode;
  /** Hide linking, adding and unlinking. */
  readOnly?: boolean | undefined;
  /** Doc options for the picker (static list). */
  docOptions?: ComboboxOption[] | undefined;
  /** Search docs on the server. Already-linked docs are left out of the results. */
  onSearchDocs?:
    ((query: string, context: ComboboxSearchContext) => Promise<ComboboxOption[]>) | undefined;
  /** Link the chosen doc (its option value). Shows "Link doc" with `docOptions` or `onSearchDocs`. */
  onLinkDoc?: ((docId: string) => unknown) | undefined;
  /** Doc ids already linked (left out of the picker). Default: the `docId` of the `doc` items. */
  linkedDocIds?: readonly string[] | undefined;
  /** "New doc" in the picker footer. */
  onCreateDoc?: (() => unknown) | undefined;
  /** Add an external link. Shows "Add link". */
  onAddLink?: ((link: { url: string; title: string }) => unknown) | undefined;
  /** Unlink a row. A returned promise shows progress; a rejection shows an error. */
  onUnlink?: ((item: LinkedDocItem) => unknown) | undefined;
  /** Called when a row is opened (the link still navigates unless you prevent it). */
  onOpen?: ((item: LinkedDocItem, event: MouseEvent<HTMLAnchorElement>) => void) | undefined;
  /** Show Space filter chips when items span more than one Space. Default true. */
  spaceFilter?: boolean | undefined;
  /** The Space filter (controlled): a Space id, `''` for all, or `null` for no Space. */
  filter?: string | null | undefined;
  defaultFilter?: string | null | undefined;
  onFilterChange?: ((filter: string | null) => void) | undefined;
  /** Heading level of the title. Default `h3`. */
  headingLevel?: 'h2' | 'h3' | 'h4' | undefined;
  /** Interface text, for translation. */
  messages?: Partial<LinkedDocsPanelMessages> | undefined;
}

function isThenable(v: unknown): v is PromiseLike<unknown> {
  return !!v && typeof (v as { then?: unknown }).then === 'function';
}

/** A full http(s) URL, or null. */
export function parseLinkUrl(raw: string): string | null {
  const s = raw.trim();
  if (!s) return null;
  try {
    const u = new URL(s);
    if ((u.protocol !== 'https:' && u.protocol !== 'http:') || !u.hostname) return null;
    return u.href;
  } catch {
    return null;
  }
}

function hostOf(href: string): string {
  try {
    return new URL(href).hostname.replace(/^www\./, '');
  } catch {
    return '';
  }
}

/**
 * The shared panel of docs and links attached to a record (a ticket, a task, a
 * campaign): Rook Dossier docs and external links, with Space filter chips,
 * open, unlink, link a doc (search picker, New doc) and add a link. The
 * product supplies the items and does the linking; the panel never fetches.
 */
export const LinkedDocsPanel = forwardRef<HTMLElement, LinkedDocsPanelProps>(
  function LinkedDocsPanel(
    {
      items,
      error,
      onRetry,
      notice,
      readOnly = false,
      docOptions,
      onSearchDocs,
      onLinkDoc,
      linkedDocIds,
      onCreateDoc,
      onAddLink,
      onUnlink,
      onOpen,
      spaceFilter = true,
      filter: filterProp,
      defaultFilter = '',
      onFilterChange,
      headingLevel: Heading = 'h3',
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const messages = useMemo(() => ({ ...DEFAULT_MESSAGES, ...messagesProp }), [messagesProp]);
    const id = useId();
    const titleId = `${id}-title`;
    const [innerFilter, setInnerFilter] = useState<string | null>(defaultFilter);
    const filterValue = filterProp !== undefined ? filterProp : innerFilter;
    const [busyId, setBusyId] = useState<string | null>(null);
    const [actionError, setActionError] = useState<string | null>(null);
    const [adding, setAdding] = useState(false);
    const listRef = useRef<HTMLUListElement>(null);
    const headerRef = useRef<HTMLDivElement>(null);
    const addButtonRef = useRef<HTMLButtonElement>(null);
    const pendingFocus = useRef<string | null | undefined>(undefined);

    const all = useMemo(() => items ?? [], [items]);
    const chips = useMemo(() => {
      const m = new Map<string, { key: string; name: string; n: number }>();
      for (const it of all) {
        const key = it.space?.id ?? NO_SPACE;
        const cur = m.get(key) ?? { key, name: it.space?.name ?? messages.noSpace, n: 0 };
        cur.n += 1;
        m.set(key, cur);
      }
      return [...m.values()].sort((a, b) =>
        a.key === NO_SPACE ? 1 : b.key === NO_SPACE ? -1 : a.name.localeCompare(b.name),
      );
    }, [all, messages.noSpace]);
    const showChips = spaceFilter && chips.length > 1;
    // A filter for a Space that no longer has rows shows everything.
    const activeKey =
      !showChips || filterValue === ''
        ? ''
        : filterValue === null
          ? NO_SPACE
          : chips.some((c) => c.key === filterValue)
            ? filterValue
            : '';
    const shown = activeKey ? all.filter((it) => (it.space?.id ?? NO_SPACE) === activeKey) : all;
    const hiddenCount = all.length - shown.length;

    const setFilter = (key: string) => {
      const next = key === NO_SPACE ? null : key;
      if (filterProp === undefined) setInnerFilter(next);
      onFilterChange?.(next);
    };

    // After an unlink, focus goes to the next row (or the previous, or the header).
    useEffect(() => {
      if (pendingFocus.current === undefined) return;
      const target = pendingFocus.current;
      pendingFocus.current = undefined;
      const row = target
        ? listRef.current?.querySelector<HTMLElement>(`[data-item-id="${CSS.escape(target)}"] a`)
        : null;
      (
        row ??
        headerRef.current?.querySelector<HTMLElement>('button') ??
        headerRef.current
      )?.focus();
    }, [items]);

    const unlink = (item: LinkedDocItem) => {
      if (!onUnlink || busyId) return;
      setActionError(null);
      const index = shown.findIndex((x) => x.id === item.id);
      const neighbour = shown[index + 1] ?? shown[index - 1];
      const done = () => {
        pendingFocus.current = neighbour?.id ?? null;
        setBusyId(null);
      };
      let result: unknown;
      try {
        result = onUnlink(item);
      } catch {
        setActionError(messages.unlinkFailed);
        return;
      }
      if (!isThenable(result)) {
        done();
        return;
      }
      setBusyId(item.id);
      result.then(done, () => {
        setBusyId(null);
        setActionError(messages.unlinkFailed);
      });
    };

    const linked = useMemo(
      () =>
        new Set(linkedDocIds ?? all.flatMap((x) => (x.kind === 'doc' && x.docId ? [x.docId] : []))),
      [linkedDocIds, all],
    );
    const canLinkDoc = !readOnly && !!onLinkDoc && !!(docOptions || onSearchDocs);
    const search = useMemo(
      () =>
        onSearchDocs
          ? async (q: string, ctx: ComboboxSearchContext) =>
              (await onSearchDocs(q, ctx)).filter((o) => !linked.has(o.value))
          : undefined,
      [onSearchDocs, linked],
    );

    const linkDoc = (docId: string | null) => {
      if (!docId || !onLinkDoc) return;
      setActionError(null);
      try {
        const r = onLinkDoc(docId);
        if (isThenable(r)) r.then(undefined, () => setActionError(messages.linkFailed));
      } catch {
        setActionError(messages.linkFailed);
      }
    };

    const loading = items === null;
    const shownError = error ?? actionError;

    return (
      <section
        ref={ref}
        {...rest}
        aria-labelledby={titleId}
        aria-busy={loading || undefined}
        className={cx('mn-linked-docs-panel', className)}
        data-read-only={readOnly || undefined}
      >
        <div className="mn-linked-docs-panel__header" ref={headerRef} tabIndex={-1}>
          <Heading id={titleId} className="mn-linked-docs-panel__title">
            {messages.title}
            {all.length > 0 ? (
              <Badge size="sm" className="mn-linked-docs-panel__count">
                {all.length}
              </Badge>
            ) : null}
          </Heading>
          {!readOnly && !notice ? (
            <div className="mn-linked-docs-panel__actions">
              {canLinkDoc ? (
                <EntityPicker
                  aria-label={messages.linkDoc}
                  {...(docOptions
                    ? { options: docOptions.filter((o) => !linked.has(o.value)) }
                    : {})}
                  {...(search ? { onSearch: search } : {})}
                  value={null}
                  onValueChange={linkDoc}
                  searchPlaceholder={messages.searchDocs}
                  emptyMessage={messages.noDocs}
                  align="end"
                  trigger={
                    <Button size="sm" variant="secondary" leadingIcon={<DocGlyph />}>
                      {messages.linkDoc}
                    </Button>
                  }
                  footer={
                    onCreateDoc
                      ? ({ close }) => (
                          <div className="mn-linked-docs-panel__picker-footer">
                            <Button
                              size="sm"
                              variant="ghost"
                              block
                              leadingIcon={<PlusGlyph />}
                              onClick={() => {
                                close();
                                onCreateDoc();
                              }}
                            >
                              {messages.newDoc}
                            </Button>
                          </div>
                        )
                      : undefined
                  }
                />
              ) : null}
              {onAddLink ? (
                <Button
                  ref={addButtonRef}
                  size="sm"
                  variant="ghost"
                  leadingIcon={<LinkGlyph />}
                  aria-expanded={adding}
                  aria-controls={adding ? `${id}-add` : undefined}
                  onClick={() => setAdding((a) => !a)}
                >
                  {messages.addLink}
                </Button>
              ) : null}
            </div>
          ) : null}
        </div>

        {adding && onAddLink && !readOnly ? (
          <AddLinkForm
            id={`${id}-add`}
            messages={messages}
            onAdd={onAddLink}
            onClose={() => {
              setAdding(false);
              requestAnimationFrame(() => addButtonRef.current?.focus());
            }}
          />
        ) : null}

        {notice ? (
          <div className="mn-linked-docs-panel__notice">{notice}</div>
        ) : (
          <>
            {showChips ? (
              <div
                className="mn-linked-docs-panel__chips"
                role="group"
                aria-label={messages.filterBySpace}
              >
                <button
                  type="button"
                  className="mn-linked-docs-panel__chip"
                  aria-pressed={activeKey === ''}
                  onClick={() => setFilter('')}
                >
                  {messages.all} <span aria-hidden>·</span> {all.length}
                </button>
                {chips.map((c) => (
                  <button
                    key={c.key}
                    type="button"
                    className="mn-linked-docs-panel__chip"
                    aria-pressed={activeKey === c.key}
                    onClick={() => setFilter(c.key)}
                  >
                    {c.name} <span aria-hidden>·</span> {c.n}
                  </button>
                ))}
              </div>
            ) : null}

            {loading ? (
              <div className="mn-linked-docs-panel__loading" role="status">
                <span className="mn-visually-hidden">{messages.loading}</span>
                <Skeleton height="2.5rem" />
                <Skeleton height="2.5rem" />
              </div>
            ) : all.length === 0 ? (
              <p className="mn-linked-docs-panel__empty">
                {readOnly ? messages.emptyReadOnly : messages.empty}
              </p>
            ) : (
              <ul className="mn-linked-docs-panel__list" ref={listRef}>
                {shown.map((it) => {
                  const external = it.external ?? true;
                  const meta = it.meta ?? (it.kind === 'link' ? hostOf(it.href) : undefined);
                  return (
                    <li
                      key={it.id}
                      className="mn-linked-docs-panel__row"
                      data-item-id={it.id}
                      data-kind={it.kind}
                      data-busy={busyId === it.id || undefined}
                    >
                      <span className="mn-linked-docs-panel__icon" aria-hidden>
                        {it.icon ?? (it.kind === 'doc' ? <DocGlyph /> : <LinkGlyph />)}
                      </span>
                      <a
                        className="mn-linked-docs-panel__link"
                        href={it.href}
                        {...(external ? { target: '_blank', rel: 'noopener noreferrer' } : {})}
                        onClick={onOpen ? (e) => onOpen(it, e) : undefined}
                      >
                        <span className="mn-linked-docs-panel__name">{it.title}</span>
                        {meta ? <span className="mn-linked-docs-panel__meta">{meta}</span> : null}
                        {external ? (
                          <span className="mn-visually-hidden"> ({messages.newTab})</span>
                        ) : null}
                      </a>
                      {it.status ? (
                        <Badge size="sm" className="mn-linked-docs-panel__status">
                          {it.status}
                        </Badge>
                      ) : null}
                      {!readOnly && onUnlink && it.unlinkable !== false ? (
                        <IconButton
                          size="sm"
                          className="mn-linked-docs-panel__unlink"
                          label={messages.unlink(it.title)}
                          icon={<CloseGlyph />}
                          loading={busyId === it.id}
                          disabled={busyId !== null && busyId !== it.id}
                          onClick={() => unlink(it)}
                        />
                      ) : null}
                    </li>
                  );
                })}
              </ul>
            )}

            {hiddenCount > 0 ? (
              <p className="mn-linked-docs-panel__hidden">
                {messages.hiddenByFilter(hiddenCount)}{' '}
                <button
                  type="button"
                  className="mn-linked-docs-panel__show-all"
                  onClick={() => setFilter('')}
                >
                  {messages.showAll}
                </button>
              </p>
            ) : null}
          </>
        )}

        {shownError ? (
          <div className="mn-linked-docs-panel__error" role="alert">
            <span>{shownError}</span>
            {error && onRetry ? (
              <Button size="xs" variant="ghost" onClick={onRetry}>
                {messages.retry}
              </Button>
            ) : null}
          </div>
        ) : null}
      </section>
    );
  },
);

function AddLinkForm({
  id,
  messages,
  onAdd,
  onClose,
}: {
  id: string;
  messages: LinkedDocsPanelMessages;
  onAdd: (link: { url: string; title: string }) => unknown;
  onClose: () => void;
}) {
  const [url, setUrl] = useState('');
  const [title, setTitle] = useState('');
  const [invalid, setInvalid] = useState(false);
  const [failed, setFailed] = useState(false);
  const [busy, setBusy] = useState(false);
  const urlRef = useRef<HTMLInputElement>(null);
  useEffect(() => urlRef.current?.focus(), []);

  const submit = (e: FormEvent) => {
    e.preventDefault();
    if (busy) return;
    const clean = parseLinkUrl(url);
    if (!clean) {
      setInvalid(true);
      urlRef.current?.focus();
      return;
    }
    setFailed(false);
    const link = { url: clean, title: title.trim() || hostOf(clean) };
    let r: unknown;
    try {
      r = onAdd(link);
    } catch {
      setFailed(true);
      return;
    }
    if (!isThenable(r)) {
      onClose();
      return;
    }
    setBusy(true);
    r.then(
      () => onClose(),
      () => {
        setBusy(false);
        setFailed(true);
      },
    );
  };

  return (
    // Escape anywhere in the form cancels it (the form's own keyboard shortcut).
    // eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions
    <form
      id={id}
      className="mn-linked-docs-panel__form"
      noValidate
      onSubmit={submit}
      onKeyDown={(e) => {
        if (e.key === 'Escape') {
          e.stopPropagation();
          onClose();
        }
      }}
    >
      <Field invalid={invalid} required>
        <FieldLabel>{messages.urlLabel}</FieldLabel>
        <Input
          ref={urlRef}
          type="url"
          inputMode="url"
          autoComplete="url"
          placeholder="https://"
          value={url}
          onChange={(e) => {
            setUrl(e.target.value);
            if (invalid) setInvalid(false);
          }}
        />
        {invalid ? <FieldError>{messages.urlInvalid}</FieldError> : null}
      </Field>
      <Field>
        <FieldLabel>
          {messages.titleLabel}{' '}
          <span className="mn-linked-docs-panel__optional">({messages.titleOptional})</span>
        </FieldLabel>
        <Input value={title} maxLength={300} onChange={(e) => setTitle(e.target.value)} />
      </Field>
      {failed ? (
        <p className="mn-linked-docs-panel__form-error" role="alert">
          {messages.linkFailed}
        </p>
      ) : null}
      <div className="mn-linked-docs-panel__form-actions">
        <Button type="submit" size="sm" variant="primary" loading={busy}>
          {messages.add}
        </Button>
        <Button size="sm" variant="ghost" onClick={onClose}>
          {messages.cancel}
        </Button>
      </div>
    </form>
  );
}
