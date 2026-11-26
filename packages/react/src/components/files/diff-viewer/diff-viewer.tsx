'use client';

import {
  Fragment,
  forwardRef,
  useId,
  useMemo,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { Button } from '../../actions/button/index.js';
import { SegmentedControl } from '../../forms/segmented-control/index.js';
import { cx } from '../../../internal/cx.js';
import { diffSequences } from '../../../internal/diff.js';

/* ------------------------------------------------------------------ model */

/** One line of a line diff, with its line number on each side. */
export interface DiffViewerLine {
  kind: 'same' | 'added' | 'removed';
  text: string;
  /** Line number in `before` (same and removed lines). */
  oldNumber?: number | undefined;
  /** Line number in `after` (same and added lines). */
  newNumber?: number | undefined;
}

/** A piece of a changed line in a word diff. */
export interface DiffViewerSegment {
  kind: 'same' | 'added' | 'removed';
  text: string;
}

const toLines = (v: string | readonly string[]) =>
  typeof v === 'string' ? (v === '' ? [] : v.replace(/\r\n?/g, '\n').split('\n')) : [...v];

/**
 * Line diff (longest common subsequence) from `before` to `after`, numbered
 * on both sides. Pass strings (split on newlines) or arrays of lines or
 * blocks (for documents, one entry per paragraph). Inputs of more than a
 * million line pairs fall back to remove-all/add-all.
 */
export function diffTextLines(
  before: string | readonly string[],
  after: string | readonly string[],
): DiffViewerLine[] {
  let o = 0;
  let n = 0;
  return diffSequences(toLines(before), toLines(after), 1_000_000).map((op) => {
    if (op.type === 'equal')
      return { kind: 'same', text: op.value, oldNumber: ++o, newNumber: ++n };
    if (op.type === 'delete') return { kind: 'removed', text: op.value, oldNumber: ++o };
    return { kind: 'added', text: op.value, newNumber: ++n };
  });
}

const TOKEN = /\s+|[\p{L}\p{N}_]+|[^\s\p{L}\p{N}_]/gu;

/**
 * Word diff of two lines, for highlighting what changed inside a changed
 * line. Words, runs of spaces and single punctuation marks are the units.
 */
export function diffTextWords(before: string, after: string): DiffViewerSegment[] {
  const a = before.match(TOKEN) ?? [];
  const b = after.match(TOKEN) ?? [];
  const out: DiffViewerSegment[] = [];
  const kind = { equal: 'same', delete: 'removed', insert: 'added' } as const;
  for (const op of diffSequences(a, b, 250_000)) {
    const k = kind[op.type];
    const last = out[out.length - 1];
    if (last && last.kind === k) last.text += op.value;
    else out.push({ kind: k, text: op.value });
  }
  return out;
}

/** Counts added and removed lines. */
export function diffStats(lines: readonly DiffViewerLine[]): { added: number; removed: number } {
  let added = 0;
  let removed = 0;
  for (const l of lines) {
    if (l.kind === 'added') added++;
    else if (l.kind === 'removed') removed++;
  }
  return { added, removed };
}

const sameLines = (a: readonly DiffViewerLine[], b: readonly DiffViewerLine[]) =>
  a.length === b.length && a.every((l, i) => l.kind === b[i]?.kind && l.text === b[i]?.text);

type Row =
  | { type: 'line'; line: DiffViewerLine; hunk: number | null }
  | { type: 'pair'; old: DiffViewerLine | null; new: DiffViewerLine | null; hunk: number | null }
  | { type: 'gap'; id: number; lines: DiffViewerLine[] };

/** Groups consecutive changes into hunks and folds long unchanged runs. */
function buildRows(
  lines: readonly DiffViewerLine[],
  split: boolean,
  context: number,
  expanded: ReadonlySet<number>,
): { rows: Row[]; hunks: number } {
  const rows: Row[] = [];
  let hunks = 0;
  let gapId = 0;
  let i = 0;
  const pushSame = (run: DiffViewerLine[], atStart: boolean, atEnd: boolean) => {
    const lead = atStart ? 0 : context;
    const trail = atEnd ? 0 : context;
    const id = gapId++;
    const same = (l: DiffViewerLine): Row =>
      split ? { type: 'pair', old: l, new: l, hunk: null } : { type: 'line', line: l, hunk: null };
    if (run.length <= lead + trail + 1 || expanded.has(id)) {
      rows.push(...run.map(same));
      return;
    }
    rows.push(...run.slice(0, lead).map(same));
    rows.push({ type: 'gap', id, lines: run.slice(lead, run.length - trail) });
    rows.push(...run.slice(run.length - trail).map(same));
  };
  while (i < lines.length) {
    if ((lines[i] as DiffViewerLine).kind === 'same') {
      const start = i;
      while (i < lines.length && (lines[i] as DiffViewerLine).kind === 'same') i++;
      pushSame(lines.slice(start, i) as DiffViewerLine[], start === 0, i === lines.length);
      continue;
    }
    const hunk = hunks++;
    const removed: DiffViewerLine[] = [];
    const added: DiffViewerLine[] = [];
    while (i < lines.length && (lines[i] as DiffViewerLine).kind !== 'same') {
      const l = lines[i++] as DiffViewerLine;
      (l.kind === 'removed' ? removed : added).push(l);
    }
    if (split) {
      for (let k = 0; k < Math.max(removed.length, added.length); k++)
        rows.push({
          type: 'pair',
          old: removed[k] ?? null,
          new: added[k] ?? null,
          hunk: k === 0 ? hunk : null,
        });
    } else {
      [...removed, ...added].forEach((line, k) =>
        rows.push({ type: 'line', line, hunk: k === 0 ? hunk : null }),
      );
    }
  }
  return { rows, hunks };
}

/* ------------------------------------------------------------- component */

/** Every string the diff viewer shows or announces. */
export interface DiffViewerMessages {
  /** Table caption (visually hidden). */
  caption: (beforeLabel: string, afterLabel: string) => string;
  /** Default labels of the two sides. */
  before: string;
  after: string;
  /** Mode switch name and options. */
  mode: string;
  split: string;
  unified: string;
  /** Stats, e.g. "3 added, 1 removed". */
  stats: (added: number, removed: number) => string;
  /** Read before an added or removed line. */
  added: string;
  removed: string;
  /** Column headers (visually hidden). */
  oldLine: string;
  newLine: string;
  change: string;
  content: string;
  /** Expand a folded run. */
  expand: (count: number) => string;
  previous: string;
  next: string;
  /** Announced when moving between changes, e.g. "Change 2 of 5". */
  position: (index: number, total: number) => string;
  /** Shown when the two sides are the same. */
  noChanges: string;
}

export const DEFAULT_DIFF_VIEWER_MESSAGES: DiffViewerMessages = {
  caption: (a, b) => `Changes from ${a} to ${b}`,
  before: 'Before',
  after: 'After',
  mode: 'Layout',
  split: 'Side by side',
  unified: 'Unified',
  stats: (added, removed) => `${added} added, ${removed} removed`,
  added: 'Added',
  removed: 'Removed',
  oldLine: 'Line before',
  newLine: 'Line after',
  change: 'Change',
  content: 'Text',
  expand: (n) => `Show ${n} unchanged ${n === 1 ? 'line' : 'lines'}`,
  previous: 'Previous change',
  next: 'Next change',
  position: (i, n) => `Change ${i} of ${n}`,
  noChanges: 'No changes.',
};

export type DiffViewerMode = 'split' | 'unified';

export interface DiffViewerProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The earlier text: a string (split on newlines) or an array of lines or blocks. */
  before: string | readonly string[];
  /** The later text, in the same shape. */
  after: string | readonly string[];
  /** Controlled layout. */
  mode?: DiffViewerMode | undefined;
  /** Initial layout when uncontrolled. Default `unified`. */
  defaultMode?: DiffViewerMode | undefined;
  /** Called when the layout switch changes. */
  onModeChange?: ((mode: DiffViewerMode) => void) | undefined;
  /** Highlight changed words inside changed lines (`word`, default) or only whole lines. */
  granularity?: 'line' | 'word' | undefined;
  /** Unchanged lines kept around each change; longer runs fold. Default 3. `Infinity` never folds. */
  context?: number | undefined;
  /** Name of the earlier side, e.g. a version name. */
  beforeLabel?: string | undefined;
  /** Name of the later side. */
  afterLabel?: string | undefined;
  /** Show the layout switch. Default true. */
  showModeSwitch?: boolean | undefined;
  /** Hide line numbers (useful for document blocks). */
  hideLineNumbers?: boolean | undefined;
  /** Content at the end of the header row (actions such as Restore). */
  actions?: ReactNode;
  messages?: Partial<DiffViewerMessages> | undefined;
}

function Words({ segments, side }: { segments: DiffViewerSegment[]; side: 'old' | 'new' }) {
  return (
    <>
      {segments.map((s, i) =>
        s.kind === 'same' ? (
          <Fragment key={i}>{s.text}</Fragment>
        ) : s.kind === 'removed' && side === 'old' ? (
          <del key={i} className="mn-diff-viewer__word">
            {s.text}
          </del>
        ) : s.kind === 'added' && side === 'new' ? (
          <ins key={i} className="mn-diff-viewer__word">
            {s.text}
          </ins>
        ) : null,
      )}
    </>
  );
}

/**
 * Shows what changed between two texts or two versions of a document, side
 * by side or unified: added and removed lines, changed words inside them,
 * folded unchanged runs, totals, and previous/next change navigation.
 */
export const DiffViewer = forwardRef<HTMLDivElement, DiffViewerProps>(function DiffViewer(
  {
    before,
    after,
    mode: modeProp,
    defaultMode = 'unified',
    onModeChange,
    granularity = 'word',
    context = 3,
    beforeLabel,
    afterLabel,
    showModeSwitch = true,
    hideLineNumbers = false,
    actions,
    messages,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_DIFF_VIEWER_MESSAGES, ...messages };
  const id = `mn-diff-viewer-${useId().replace(/:/g, '')}`;
  const [innerMode, setInnerMode] = useState<DiffViewerMode>(defaultMode);
  const mode = modeProp ?? innerMode;
  const split = mode === 'split';
  const [expanded, setExpanded] = useState<ReadonlySet<number>>(new Set());
  const [position, setPosition] = useState(-1);
  const body = useRef<HTMLTableSectionElement>(null);

  const lines = useMemo(() => diffTextLines(before, after), [before, after]);
  // Expanded gaps and the "change 2 of 5" position are indexes into this diff.
  // When the texts change (another version is picked) they would point at other
  // gaps and hunks, so they start over. Compared by content: a parent that
  // passes new arrays with the same lines keeps what the reader expanded.
  const [shownLines, setShownLines] = useState(lines);
  if (shownLines !== lines) {
    setShownLines(lines);
    if (!sameLines(shownLines, lines)) {
      setExpanded(new Set());
      setPosition(-1);
    }
  }
  const stats = diffStats(lines);
  const { rows, hunks } = useMemo(
    () => buildRows(lines, split, Number.isFinite(context) ? Math.max(0, context) : 1e9, expanded),
    [lines, split, context, expanded],
  );

  // Word segments for changed lines that pair up (removed k with added k in a hunk).
  const words = useMemo(() => {
    const map = new Map<DiffViewerLine, DiffViewerSegment[]>();
    if (granularity !== 'word') return map;
    let i = 0;
    while (i < lines.length) {
      if ((lines[i] as DiffViewerLine).kind === 'same') {
        i++;
        continue;
      }
      const removed: DiffViewerLine[] = [];
      const added: DiffViewerLine[] = [];
      while (i < lines.length && (lines[i] as DiffViewerLine).kind !== 'same') {
        const l = lines[i++] as DiffViewerLine;
        (l.kind === 'removed' ? removed : added).push(l);
      }
      for (let k = 0; k < Math.min(removed.length, added.length); k++) {
        const segs = diffTextWords(
          (removed[k] as DiffViewerLine).text,
          (added[k] as DiffViewerLine).text,
        );
        map.set(removed[k] as DiffViewerLine, segs);
        map.set(added[k] as DiffViewerLine, segs);
      }
    }
    return map;
  }, [lines, granularity]);

  const setMode = (next: DiffViewerMode) => {
    if (modeProp === undefined) setInnerMode(next);
    onModeChange?.(next);
  };

  const goTo = (index: number) => {
    const target = body.current?.querySelector<HTMLElement>(`[data-hunk="${index}"]`);
    if (!target) return;
    setPosition(index);
    target.scrollIntoView?.({ block: 'center' });
    target.focus({ preventScroll: true });
  };

  const text = (line: DiffViewerLine | null, side: 'old' | 'new') => {
    if (!line) return null;
    const segs = line.kind !== 'same' ? words.get(line) : undefined;
    const content = segs ? <Words segments={segs} side={side} /> : line.text || ' ';
    if (line.kind === 'same') return content;
    const Tag = line.kind === 'added' ? 'ins' : 'del';
    return (
      <Tag className="mn-diff-viewer__change">
        <span className="mn-visually-hidden">{line.kind === 'added' ? m.added : m.removed}: </span>
        {content}
      </Tag>
    );
  };
  const marker = (kind: DiffViewerLine['kind']) =>
    kind === 'added' ? '+' : kind === 'removed' ? '−' : '';

  const beforeName = beforeLabel ?? m.before;
  const afterName = afterLabel ?? m.after;
  const numbers = !hideLineNumbers;
  const columns = numbers ? 4 : 2;

  return (
    <div ref={ref} {...rest} className={cx('mn-diff-viewer', className)} data-mode={mode}>
      <div className="mn-diff-viewer__header">
        <p className="mn-diff-viewer__stats">
          <span className="mn-diff-viewer__added" aria-hidden>
            +{stats.added}
          </span>{' '}
          <span className="mn-diff-viewer__removed" aria-hidden>
            −{stats.removed}
          </span>
          <span className="mn-visually-hidden">{m.stats(stats.added, stats.removed)}</span>
        </p>
        <div className="mn-diff-viewer__controls">
          {hunks > 1 ? (
            <>
              <Button
                size="sm"
                variant="ghost"
                onClick={() => goTo(position <= 0 ? hunks - 1 : position - 1)}
              >
                {m.previous}
              </Button>
              <Button size="sm" variant="ghost" onClick={() => goTo((position + 1) % hunks)}>
                {m.next}
              </Button>
            </>
          ) : null}
          {showModeSwitch ? (
            <SegmentedControl
              size="sm"
              aria-label={m.mode}
              value={mode}
              onValueChange={(v) => setMode(v as DiffViewerMode)}
              options={[
                { value: 'unified', label: m.unified },
                { value: 'split', label: m.split },
              ]}
            />
          ) : null}
          {actions}
        </div>
      </div>
      <span className="mn-visually-hidden" role="status">
        {position >= 0 ? m.position(position + 1, hunks) : ''}
      </span>
      {lines.length && (stats.added || stats.removed) ? (
        <div
          className="mn-diff-viewer__scroll"
          // A focusable region so keyboard users can scroll a wide diff.
          // eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex
          tabIndex={0}
          role="region"
          aria-labelledby={`${id}-caption`}
        >
          <table className="mn-diff-viewer__table">
            <caption id={`${id}-caption`} className="mn-visually-hidden">
              {m.caption(beforeName, afterName)}
            </caption>
            <thead className={split ? 'mn-diff-viewer__sides' : 'mn-visually-hidden'}>
              {split ? (
                <tr>
                  <th scope="col" colSpan={numbers ? 2 : 1}>
                    {beforeName}
                  </th>
                  <th scope="col" colSpan={numbers ? 2 : 1}>
                    {afterName}
                  </th>
                </tr>
              ) : (
                <tr>
                  {numbers ? (
                    <>
                      <th scope="col">{m.oldLine}</th>
                      <th scope="col">{m.newLine}</th>
                    </>
                  ) : null}
                  <th scope="col">{m.change}</th>
                  <th scope="col">{m.content}</th>
                </tr>
              )}
            </thead>
            <tbody ref={body}>
              {rows.map((row, i) => {
                if (row.type === 'gap')
                  return (
                    <tr key={`gap-${row.id}`} className="mn-diff-viewer__gap">
                      <td colSpan={columns}>
                        <button
                          type="button"
                          className="mn-diff-viewer__expand"
                          onClick={() => setExpanded((s) => new Set(s).add(row.id))}
                        >
                          {m.expand(row.lines.length)}
                        </button>
                      </td>
                    </tr>
                  );
                const hunkAttrs =
                  row.hunk !== null
                    ? {
                        'data-hunk': row.hunk,
                        tabIndex: -1,
                        'data-current': row.hunk === position || undefined,
                      }
                    : {};
                if (row.type === 'line') {
                  const l = row.line;
                  return (
                    <tr key={i} className="mn-diff-viewer__row" data-kind={l.kind} {...hunkAttrs}>
                      {numbers ? (
                        <>
                          <td className="mn-diff-viewer__number">{l.oldNumber ?? ''}</td>
                          <td className="mn-diff-viewer__number">{l.newNumber ?? ''}</td>
                        </>
                      ) : null}
                      <td className="mn-diff-viewer__marker" aria-hidden>
                        {marker(l.kind)}
                      </td>
                      <td className="mn-diff-viewer__text">
                        {text(l, l.kind === 'added' ? 'new' : 'old')}
                      </td>
                    </tr>
                  );
                }
                const same = row.old !== null && row.old === row.new;
                return (
                  <tr
                    key={i}
                    className="mn-diff-viewer__row"
                    data-kind={same ? 'same' : 'change'}
                    {...hunkAttrs}
                  >
                    {numbers ? (
                      <td className="mn-diff-viewer__number">{row.old?.oldNumber ?? ''}</td>
                    ) : null}
                    <td
                      className="mn-diff-viewer__text"
                      data-side="old"
                      data-kind={row.old?.kind ?? 'empty'}
                    >
                      {text(row.old, 'old')}
                    </td>
                    {numbers ? (
                      <td className="mn-diff-viewer__number">{row.new?.newNumber ?? ''}</td>
                    ) : null}
                    <td
                      className="mn-diff-viewer__text"
                      data-side="new"
                      data-kind={row.new?.kind ?? 'empty'}
                    >
                      {text(row.new, 'new')}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      ) : (
        <p className="mn-diff-viewer__empty">{m.noChanges}</p>
      )}
    </div>
  );
});
