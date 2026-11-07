'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useMemo,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { RelativeTime, type DateInput } from '../../data-display/relative-time/index.js';
import { Skeleton } from '../../feedback/skeleton/index.js';
import { Input } from '../../forms/text-field/index.js';
import { Avatar } from '../../identity/avatar/index.js';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../../navigation/tabs/index.js';
import { ConfirmDialog } from '../../overlays/confirm-dialog/index.js';

/* ----------------------------------------------------------------- diff */

/** One line of a version diff. */
export interface VersionDiffLine {
  kind: 'same' | 'added' | 'removed';
  text: string;
}

/** A run of unchanged lines folded away by `collapseVersionDiff`. */
export interface VersionDiffGap {
  kind: 'gap';
  count: number;
}

/**
 * Line diff (longest common subsequence) from `before` to `after`. Compare
 * blocks or paragraphs, not characters: what a reader recognises as "what
 * changed". Inputs over a million line pairs fall back to remove-all/add-all.
 */
export function diffVersionLines(
  before: readonly string[],
  after: readonly string[],
): VersionDiffLine[] {
  const n = before.length;
  const k = after.length;
  if (n * k > 1_000_000) {
    return [
      ...before.map((text) => ({ kind: 'removed' as const, text })),
      ...after.map((text) => ({ kind: 'added' as const, text })),
    ];
  }
  const lcs: Uint32Array[] = Array.from({ length: n + 1 }, () => new Uint32Array(k + 1));
  for (let i = n - 1; i >= 0; i--) {
    const row = lcs[i] as Uint32Array;
    const below = lcs[i + 1] as Uint32Array;
    for (let j = k - 1; j >= 0; j--) {
      row[j] =
        before[i] === after[j]
          ? (below[j + 1] as number) + 1
          : Math.max(below[j] as number, row[j + 1] as number);
    }
  }
  const out: VersionDiffLine[] = [];
  let i = 0;
  let j = 0;
  while (i < n && j < k) {
    if (before[i] === after[j]) {
      out.push({ kind: 'same', text: before[i] as string });
      i++;
      j++;
    } else if (((lcs[i + 1] as Uint32Array)[j] ?? 0) >= ((lcs[i] as Uint32Array)[j + 1] ?? 0)) {
      out.push({ kind: 'removed', text: before[i] as string });
      i++;
    } else {
      out.push({ kind: 'added', text: after[j] as string });
      j++;
    }
  }
  while (i < n) out.push({ kind: 'removed', text: before[i++] as string });
  while (j < k) out.push({ kind: 'added', text: after[j++] as string });
  return out;
}

/** Counts added and removed lines. */
export function versionDiffStats(lines: readonly VersionDiffLine[]): {
  added: number;
  removed: number;
} {
  let added = 0;
  let removed = 0;
  for (const l of lines) {
    if (l.kind === 'added') added++;
    else if (l.kind === 'removed') removed++;
  }
  return { added, removed };
}

/**
 * Folds long runs of unchanged lines into gaps, keeping `context` lines on
 * each side, like a unified diff.
 */
export function collapseVersionDiff(
  lines: readonly VersionDiffLine[],
  context = 2,
): (VersionDiffLine | VersionDiffGap)[] {
  const out: (VersionDiffLine | VersionDiffGap)[] = [];
  let run: VersionDiffLine[] = [];
  const flush = () => {
    if (run.length <= context * 2 + 1) out.push(...run);
    else {
      out.push(...run.slice(0, context));
      out.push({ kind: 'gap', count: run.length - context * 2 });
      out.push(...run.slice(-context));
    }
    run = [];
  };
  for (const line of lines) {
    if (line.kind === 'same') run.push(line);
    else {
      flush();
      out.push(line);
    }
  }
  flush();
  return out;
}

/* ---------------------------------------------------------------- panel */

/** One saved version. */
export interface VersionHistoryItem {
  id: string;
  createdAt: DateInput;
  author: { name: string; avatarUrl?: string | null | undefined };
  /** The name someone gave this version. */
  note?: string | undefined;
  /** Saved automatically rather than by a person. */
  automatic?: boolean | undefined;
}

/** Visible and announced strings. Override them to translate. */
export interface VersionHistoryPanelMessages {
  title: string;
  close: string;
  back: string;
  noteLabel: string;
  notePlaceholder: string;
  save: string;
  saved: string;
  saveFailed: string;
  empty: string;
  current: string;
  automatic: string;
  versions: string;
  changes: string;
  fullText: string;
  vsNow: string;
  added: (n: number) => string;
  removed: (n: number) => string;
  identical: string;
  gap: (n: number) => string;
  addedLine: string;
  removedLine: string;
  restore: string;
  restoreTitle: string;
  restoreDescription: string;
  restoreConfirm: string;
  restored: string;
  loading: string;
}

export const DEFAULT_VERSION_HISTORY_PANEL_MESSAGES: VersionHistoryPanelMessages = {
  title: 'History',
  close: 'Close history',
  back: 'Back to version list',
  noteLabel: 'Version name',
  notePlaceholder: 'Name this version (optional)',
  save: 'Save version',
  saved: 'Version saved',
  saveFailed: 'The version could not be saved. Try again.',
  empty: 'No versions yet. Save one to mark this moment.',
  current: 'Current',
  automatic: 'auto',
  versions: 'Versions',
  changes: 'Changes',
  fullText: 'Full text',
  vsNow: 'compared with now',
  added: (n) => `${n} added`,
  removed: (n) => `${n} removed`,
  identical: 'This version is identical to the current one.',
  gap: (n) => `${n} unchanged ${n === 1 ? 'line' : 'lines'}`,
  addedLine: 'Added: ',
  removedLine: 'Removed: ',
  restore: 'Restore this version',
  restoreTitle: 'Restore this version?',
  restoreDescription: 'The current content is replaced with this version.',
  restoreConfirm: 'Restore',
  restored: 'Version restored',
  loading: 'Loading',
};

export interface VersionHistoryPanelProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** Saved versions, newest first. */
  versions: VersionHistoryItem[];
  /** Which version matches the content now. Default: the first. */
  currentId?: string | null | undefined;
  /** The version being viewed (controlled). `null` shows the list. */
  selectedId?: string | null | undefined;
  /** The version viewed first when uncontrolled. */
  defaultSelectedId?: string | null | undefined;
  /** Called when someone opens a version or goes back to the list. Load its lines here. */
  onSelectedIdChange?: ((id: string | null) => void) | undefined;
  /** The content now, one entry per block or paragraph. */
  currentLines: readonly string[];
  /** The selected version’s content. Leave undefined while it loads. */
  selectedLines?: readonly string[] | undefined;
  /** The version list is loading. */
  loading?: boolean | undefined;
  /** Shows "Save version" with an optional name. Reject to show an error. */
  onSaveVersion?: ((note: string) => void | Promise<void>) | undefined;
  /** Shows "Restore this version" behind a confirmation. Reject to keep the dialog open with the error. */
  onRestore?: ((id: string) => void | Promise<void>) | undefined;
  /** Shows a close button. */
  onClose?: (() => void) | undefined;
  /** Heading. Default "History". */
  title?: ReactNode;
  /** Override visible and announced strings. */
  messages?: Partial<VersionHistoryPanelMessages> | undefined;
}

function CloseIcon() {
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
      <path d="M18 6 6 18M6 6l12 12" />
    </svg>
  );
}
function BackIcon() {
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
      <path d="M19 12H5M12 19l-7-7 7-7" />
    </svg>
  );
}

/**
 * Version history for a document or record: a list of saved versions, a
 * line diff of any version against the content now, the full text, and
 * restore behind a confirmation. Data and callbacks in; the product loads
 * versions and their content.
 */
export const VersionHistoryPanel = forwardRef<HTMLElement, VersionHistoryPanelProps>(
  function VersionHistoryPanel(
    {
      versions,
      currentId,
      selectedId: selectedProp,
      defaultSelectedId = null,
      onSelectedIdChange,
      currentLines,
      selectedLines,
      loading = false,
      onSaveVersion,
      onRestore,
      onClose,
      title,
      messages: messagesProp,
      className,
      ...rest
    },
    ref,
  ) {
    const m = { ...DEFAULT_VERSION_HISTORY_PANEL_MESSAGES, ...messagesProp };
    const headingId = useId();
    const [ownSelected, setOwnSelected] = useState<string | null>(defaultSelectedId);
    const selectedId = selectedProp === undefined ? ownSelected : selectedProp;
    const selected = selectedId ? versions.find((v) => v.id === selectedId) : undefined;
    const current = currentId === undefined ? versions[0]?.id : currentId;
    const [note, setNote] = useState('');
    const [saving, setSaving] = useState(false);
    const [saveError, setSaveError] = useState<string | null>(null);
    const [announce, setAnnounce] = useState('');
    const [tab, setTab] = useState<'changes' | 'full'>('changes');
    const lastOpened = useRef<string | null>(null);
    const listRef = useRef<HTMLUListElement>(null);
    const backRef = useRef<HTMLButtonElement>(null);
    const focusAfter = useRef<'back' | 'row' | null>(null);

    const select = (id: string | null) => {
      if (id) lastOpened.current = id;
      focusAfter.current = id ? 'back' : 'row';
      setTab('changes');
      if (selectedProp === undefined) setOwnSelected(id);
      onSelectedIdChange?.(id);
    };

    useEffect(() => {
      const target = focusAfter.current;
      if (!target) return;
      focusAfter.current = null;
      if (target === 'back') backRef.current?.focus();
      else if (lastOpened.current) {
        const rows =
          listRef.current?.querySelectorAll<HTMLButtonElement>('[data-version-id]') ?? [];
        Array.from(rows)
          .find((b) => b.dataset.versionId === lastOpened.current)
          ?.focus();
      }
    }, [selectedId]);

    const diff = useMemo(
      () => (selectedLines ? diffVersionLines(selectedLines, currentLines) : null),
      [selectedLines, currentLines],
    );
    const stats = diff ? versionDiffStats(diff) : null;
    const rows = diff ? collapseVersionDiff(diff) : [];

    const save = async () => {
      if (!onSaveVersion || saving) return;
      setSaving(true);
      setSaveError(null);
      try {
        await onSaveVersion(note.trim());
        setNote('');
        setAnnounce(m.saved);
      } catch (e) {
        setSaveError(e instanceof Error && e.message ? e.message : m.saveFailed);
      } finally {
        setSaving(false);
      }
    };

    const loadingLines = (
      <div role="status" aria-label={m.loading} className="mn-version-history-panel__loading">
        <Skeleton shape="text" width="80%" />
        <Skeleton shape="text" width="64%" />
        <Skeleton shape="text" width="72%" />
      </div>
    );

    const header = (
      <div className="mn-version-history-panel__head">
        {selected ? (
          <IconButton
            ref={backRef}
            size="sm"
            label={m.back}
            icon={<BackIcon />}
            onClick={() => select(null)}
          />
        ) : null}
        <h2 id={headingId} className="mn-version-history-panel__title">
          {selected ? <RelativeTime date={selected.createdAt} tooltip /> : (title ?? m.title)}
        </h2>
        {onClose ? (
          <IconButton
            size="sm"
            label={m.close}
            icon={<CloseIcon />}
            onClick={onClose}
            className="mn-version-history-panel__close"
          />
        ) : null}
      </div>
    );

    return (
      <aside
        ref={ref}
        aria-labelledby={headingId}
        {...rest}
        data-view={selected ? 'detail' : 'list'}
        className={cx('mn-version-history-panel', className)}
      >
        {header}
        {selected ? (
          <>
            <p className="mn-version-history-panel__byline">
              <Avatar
                name={selected.author.name}
                src={selected.author.avatarUrl ?? null}
                size="xs"
                decorative
              />
              <span>
                {selected.author.name}
                {selected.note
                  ? ` · ${selected.note}`
                  : selected.automatic
                    ? ` · ${m.automatic}`
                    : ''}
              </span>
            </p>
            <Tabs
              value={tab}
              onValueChange={(v) => setTab(v as 'changes' | 'full')}
              className="mn-version-history-panel__tabs"
            >
              <TabsList>
                <TabsTrigger value="changes">{m.changes}</TabsTrigger>
                <TabsTrigger value="full">{m.fullText}</TabsTrigger>
              </TabsList>
              <TabsContent value="changes" className="mn-version-history-panel__body">
                {!selectedLines ? (
                  loadingLines
                ) : stats && stats.added + stats.removed === 0 ? (
                  <p className="mn-version-history-panel__hint">{m.identical}</p>
                ) : (
                  <>
                    {stats ? (
                      <p className="mn-version-history-panel__stats">
                        <span data-kind="added">+{stats.added}</span>
                        <span className="mn-visually-hidden">{m.added(stats.added)}, </span>
                        <span data-kind="removed">−{stats.removed}</span>
                        <span className="mn-visually-hidden">{m.removed(stats.removed)}, </span>
                        <span>{m.vsNow}</span>
                      </p>
                    ) : null}
                    <ol className="mn-version-history-panel__diff">
                      {rows.map((row, i) =>
                        row.kind === 'gap' ? (
                          <li key={i} className="mn-version-history-panel__gap">
                            {m.gap(row.count)}
                          </li>
                        ) : (
                          <li
                            key={i}
                            className="mn-version-history-panel__line"
                            data-kind={row.kind}
                          >
                            <span className="mn-version-history-panel__marker" aria-hidden="true">
                              {row.kind === 'added' ? '+' : row.kind === 'removed' ? '−' : ''}
                            </span>
                            {row.kind === 'added' ? (
                              <ins>
                                <span className="mn-visually-hidden">{m.addedLine}</span>
                                {row.text}
                              </ins>
                            ) : row.kind === 'removed' ? (
                              <del>
                                <span className="mn-visually-hidden">{m.removedLine}</span>
                                {row.text}
                              </del>
                            ) : (
                              <span>{row.text}</span>
                            )}
                          </li>
                        ),
                      )}
                    </ol>
                  </>
                )}
              </TabsContent>
              <TabsContent value="full" className="mn-version-history-panel__body">
                {!selectedLines
                  ? loadingLines
                  : selectedLines.map((line, i) => (
                      <p key={i} className="mn-version-history-panel__text">
                        {line}
                      </p>
                    ))}
              </TabsContent>
            </Tabs>
            {onRestore && selected.id !== current ? (
              <div className="mn-version-history-panel__foot">
                <ConfirmDialog
                  trigger={
                    <Button variant="primary" block>
                      {m.restore}
                    </Button>
                  }
                  title={m.restoreTitle}
                  description={m.restoreDescription}
                  confirmLabel={m.restoreConfirm}
                  onConfirm={async () => {
                    await onRestore(selected.id);
                    setAnnounce(m.restored);
                    select(null);
                  }}
                />
              </div>
            ) : null}
          </>
        ) : (
          <>
            {onSaveVersion ? (
              <form
                className="mn-version-history-panel__save"
                onSubmit={(e) => {
                  e.preventDefault();
                  void save();
                }}
              >
                <Input
                  value={note}
                  onChange={(e) => setNote(e.target.value)}
                  placeholder={m.notePlaceholder}
                  aria-label={m.noteLabel}
                  maxLength={200}
                  size="sm"
                  disabled={saving}
                />
                <Button type="submit" size="sm" loading={saving}>
                  {m.save}
                </Button>
              </form>
            ) : null}
            {saveError ? (
              <p className="mn-version-history-panel__error" role="alert">
                {saveError}
              </p>
            ) : null}
            <div className="mn-version-history-panel__body">
              {loading ? (
                <div
                  role="status"
                  aria-label={m.loading}
                  className="mn-version-history-panel__loading"
                >
                  <Skeleton shape="text" width="80%" />
                  <Skeleton shape="text" width="64%" />
                </div>
              ) : versions.length === 0 ? (
                <p className="mn-version-history-panel__hint">{m.empty}</p>
              ) : (
                <ul
                  ref={listRef}
                  className="mn-version-history-panel__list"
                  aria-label={m.versions}
                >
                  {versions.map((v) => (
                    <li key={v.id}>
                      <button
                        type="button"
                        className="mn-version-history-panel__row"
                        data-version-id={v.id}
                        onClick={() => select(v.id)}
                      >
                        <Avatar
                          name={v.author.name}
                          src={v.author.avatarUrl ?? null}
                          size="sm"
                          decorative
                        />
                        <span className="mn-version-history-panel__row-text">
                          <span className="mn-version-history-panel__row-top">
                            <RelativeTime date={v.createdAt} tooltip={false} />
                            {v.id === current ? (
                              <span className="mn-version-history-panel__current">{m.current}</span>
                            ) : null}
                          </span>
                          <span className="mn-version-history-panel__meta">
                            {v.author.name}
                            {v.note ? ` · ${v.note}` : v.automatic ? ` · ${m.automatic}` : ''}
                          </span>
                        </span>
                      </button>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </>
        )}
        <span className="mn-visually-hidden" aria-live="polite">
          {announce}
        </span>
      </aside>
    );
  },
);
