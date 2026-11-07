'use client';

import {
  forwardRef,
  useEffect,
  useMemo,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronRightGlyph } from '../../../internal/icons.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { NativeSelect } from '../../forms/form-field/index.js';
import { AiCodeBlock } from '../ai-code-block/index.js';
import { diffWords } from '../ai-edit-proposal/index.js';

/** One saved version of an artifact. */
export interface AiArtifactVersionInfo {
  /** Version number, counting from 1. */
  version: number;
  /** When it was made: ISO 8601 string, epoch milliseconds or a Date. */
  createdAt: string | number | Date;
  /** Optional name, e.g. "Shorter intro". */
  label?: string | undefined;
}

/** Every string AiArtifactVersions shows or announces. English defaults. */
export interface AiArtifactVersionsMessages {
  /** Accessible name of the group. */
  label: string;
  /** "Version 3 of 5". */
  position: (version: number, total: number) => string;
  previous: string;
  next: string;
  /** Accessible name of the version select. */
  select: string;
  /** One option of the select, e.g. "v3 · Shorter intro · 12 Mar, 14:05". */
  option: (info: { version: number; label?: string | undefined; date: string }) => string;
  /** Formats `createdAt`. */
  formatDate: (date: Date) => string;
  /** Compare button. */
  compare: string;
  /** Announced politely when the version changes. */
  announce: (version: number, total: number) => string;
}

export const AI_ARTIFACT_VERSIONS_MESSAGES: AiArtifactVersionsMessages = {
  label: 'Versions',
  position: (version, total) => `Version ${version} of ${total}`,
  previous: 'Previous version',
  next: 'Next version',
  select: 'Choose a version',
  option: ({ version, label, date }) => [`v${version}`, label, date].filter(Boolean).join(' · '),
  formatDate: (date) =>
    new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(date),
  compare: 'Compare with previous',
  announce: (version, total) => `Showing version ${version} of ${total}`,
};

function toDate(value: string | number | Date): Date | null {
  const d = value instanceof Date ? value : new Date(value);
  return Number.isNaN(d.getTime()) ? null : d;
}

export interface AiArtifactVersionsProps extends Omit<HTMLAttributes<HTMLDivElement>, 'onChange'> {
  /** Every version of the artifact, in any order; shown oldest to newest. */
  versions: readonly AiArtifactVersionInfo[];
  /** The version shown now. */
  value: number;
  /** Called with the chosen version number. */
  onChange: (version: number) => void;
  /**
   * Shows "Compare with previous". Called with the previous and the current
   * version numbers; show an `AiArtifactDiff` of the two.
   */
  onCompare?: ((from: number, to: number) => void) | undefined;
  /** The compare view is showing. Sets `aria-pressed` on the compare button. */
  comparing?: boolean | undefined;
  /** Strings, for localisation. */
  messages?: Partial<AiArtifactVersionsMessages> | undefined;
}

/**
 * Steps through the versions of an artifact: previous and next buttons, the
 * position ("Version 3 of 5") and a select to jump to any version, plus an
 * optional "Compare with previous". Controlled; announces the change politely.
 */
export const AiArtifactVersions = forwardRef<HTMLDivElement, AiArtifactVersionsProps>(
  function AiArtifactVersions(
    { versions, value, onChange, onCompare, comparing, messages: messagesProp, className, ...rest },
    ref,
  ) {
    const m = { ...AI_ARTIFACT_VERSIONS_MESSAGES, ...messagesProp };
    const sorted = useMemo(() => [...versions].sort((a, b) => a.version - b.version), [versions]);
    const total = sorted.length;
    const index = sorted.findIndex((v) => v.version === value);
    const prev = index > 0 ? sorted[index - 1] : undefined;
    const next = index !== -1 && index < total - 1 ? sorted[index + 1] : undefined;

    // Announce only changes, not the first render.
    const [announcement, setAnnouncement] = useState('');
    const last = useRef(value);
    useEffect(() => {
      if (last.current === value) return;
      last.current = value;
      setAnnouncement(m.announce(value, total));
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [value]);

    const options = sorted.map((v) => {
      const date = toDate(v.createdAt);
      return {
        value: String(v.version),
        label: m.option({
          version: v.version,
          label: v.label,
          date: date ? m.formatDate(date) : '',
        }),
      };
    });

    return (
      <div
        ref={ref}
        role="group"
        aria-label={m.label}
        {...rest}
        className={cx('mn-ai-artifact-versions', className)}
      >
        <IconButton
          size="sm"
          label={m.previous}
          icon={<ChevronRightGlyph className="mn-ai-artifact-versions__prev" />}
          disabled={!prev}
          onClick={() => prev && onChange(prev.version)}
        />
        <span className="mn-ai-artifact-versions__position">{m.position(value, total)}</span>
        <IconButton
          size="sm"
          label={m.next}
          icon={<ChevronRightGlyph className="mn-ai-artifact-versions__next" />}
          disabled={!next}
          onClick={() => next && onChange(next.version)}
        />
        <NativeSelect
          size="sm"
          aria-label={m.select}
          className="mn-ai-artifact-versions__select"
          wrapperClassName="mn-ai-artifact-versions__select-wrap"
          value={String(value)}
          options={options}
          onChange={(e) => onChange(Number(e.currentTarget.value))}
        />
        {onCompare ? (
          <Button
            size="sm"
            variant="ghost"
            className="mn-ai-artifact-versions__compare"
            aria-pressed={comparing ?? undefined}
            disabled={!prev}
            onClick={() => prev && onCompare(prev.version, value)}
          >
            {m.compare}
          </Button>
        ) : null}
        <span className="mn-visually-hidden" role="status" aria-live="polite">
          {announcement}
        </span>
      </div>
    );
  },
);

// ---------------------------------------------------------------- diff

/**
 * A unified, line-level diff of two texts (longest common subsequence over
 * lines), as `AiCodeBlock` renders with `language="diff"`: unchanged lines
 * start with a space, removed lines with `-`, added lines with `+`. No hunk
 * headers; every line is included. Very large inputs fall back to removing
 * every old line and adding every new one.
 */
export function diffLines(before: string, after: string): string {
  const a = before.split('\n');
  const b = after.split('\n');
  const n = a.length;
  const k = b.length;
  if (n * k > 1_000_000) {
    return [...a.map((l) => `-${l}`), ...b.map((l) => `+${l}`)].join('\n');
  }
  // lcs[i][j]: length of the LCS of a[i..] and b[j..].
  const lcs: Uint32Array[] = Array.from({ length: n + 1 }, () => new Uint32Array(k + 1));
  for (let i = n - 1; i >= 0; i--) {
    const row = lcs[i] as Uint32Array;
    const below = lcs[i + 1] as Uint32Array;
    for (let j = k - 1; j >= 0; j--) {
      row[j] =
        a[i] === b[j]
          ? (below[j + 1] as number) + 1
          : Math.max(below[j] as number, row[j + 1] as number);
    }
  }
  const out: string[] = [];
  let i = 0;
  let j = 0;
  while (i < n && j < k) {
    if (a[i] === b[j]) {
      out.push(` ${a[i]}`);
      i++;
      j++;
    } else if (((lcs[i + 1] as Uint32Array)[j] ?? 0) >= ((lcs[i] as Uint32Array)[j + 1] ?? 0)) {
      out.push(`-${a[i]}`);
      i++;
    } else {
      out.push(`+${b[j]}`);
      j++;
    }
  }
  while (i < n) out.push(`-${a[i++]}`);
  while (j < k) out.push(`+${b[j++]}`);
  return out.join('\n');
}

/** Strings for AiArtifactDiff. */
export interface AiArtifactDiffMessages {
  /** Accessible name of the diff. */
  label: (from: number | undefined, to: number | undefined) => string;
  /** Read before removed and added text in the word diff. */
  removed: string;
  added: string;
  /** Shown when the two versions are the same. */
  same: string;
}

export const AI_ARTIFACT_DIFF_MESSAGES: AiArtifactDiffMessages = {
  label: (from, to) =>
    from !== undefined && to !== undefined
      ? `Changes from version ${from} to version ${to}`
      : 'Changes',
  removed: 'Removed: ',
  added: 'Added: ',
  same: 'No changes between these versions.',
};

export interface AiArtifactDiffProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The older content. */
  before: string;
  /** The newer content. */
  after: string;
  /** Version numbers, for the accessible name. */
  from?: number | undefined;
  to?: number | undefined;
  /**
   * `words` (default): prose with removed and added words marked inline
   * (`diffWords`). `lines`: a line diff in an `AiCodeBlock`, for code.
   */
  mode?: 'words' | 'lines' | undefined;
  /** Strings, for localisation. */
  messages?: Partial<AiArtifactDiffMessages> | undefined;
}

/**
 * What changed between two versions of an artifact: removed and added words
 * inline for prose, or a line diff in an `AiCodeBlock` for code. Removals and
 * additions are marked with text as well as colour.
 */
export const AiArtifactDiff = forwardRef<HTMLDivElement, AiArtifactDiffProps>(
  function AiArtifactDiff(
    { before, after, from, to, mode = 'words', messages: messagesProp, className, ...rest },
    ref,
  ) {
    const m = { ...AI_ARTIFACT_DIFF_MESSAGES, ...messagesProp };
    const label = m.label(from, to);
    const same = before === after;
    const body: ReactNode = useMemo(() => {
      if (same) return null;
      if (mode === 'lines') {
        return (
          <AiCodeBlock
            code={diffLines(before, after)}
            language="diff"
            filename={label}
            showWrapToggle={false}
          />
        );
      }
      return (
        <p className="mn-ai-artifact-diff__text">
          {diffWords(before, after).map((seg, i) =>
            seg.type === 'equal' ? (
              <span key={i}>{seg.text}</span>
            ) : seg.type === 'delete' ? (
              <del key={i} className="mn-ai-artifact-diff__del">
                <span className="mn-visually-hidden">{m.removed}</span>
                {seg.text}
              </del>
            ) : (
              <ins key={i} className="mn-ai-artifact-diff__ins">
                <span className="mn-visually-hidden">{m.added}</span>
                {seg.text}
              </ins>
            ),
          )}
        </p>
      );
    }, [same, mode, before, after, label, m.removed, m.added]);

    return (
      <div
        ref={ref}
        role="group"
        aria-label={label}
        {...rest}
        className={cx('mn-ai-artifact-diff', className)}
        data-mode={mode}
      >
        {same ? <p className="mn-ai-artifact-diff__same">{m.same}</p> : body}
      </div>
    );
  },
);
