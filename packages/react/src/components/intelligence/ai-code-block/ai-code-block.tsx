'use client';

import { forwardRef, useMemo, useState, type HTMLAttributes, type ReactNode } from 'react';
import { cx } from '../../../internal/cx.js';
import { Button } from '../../actions/button/index.js';
import { CopyButton } from '../../actions/copy-button/index.js';
import {
  resolveCodeLanguage,
  tokenizeCode,
  tokensToLines,
  type CodeToken,
} from './ai-code-block.tokenize.js';

export {
  tokenizeCode,
  resolveCodeLanguage,
  type CodeToken,
  type CodeTokenType,
} from './ai-code-block.tokenize.js';

/** Every user-visible string in AiCodeBlock. */
export interface AiCodeBlockMessages {
  /** Copy button label. Default "Copy". */
  copy: string;
  /** Copy button label after copying. Default "Copied". */
  copied: string;
  /** Copy button label after a failed copy. Default "Couldn't copy". */
  copyError: string;
  /** Wrap toggle label (a pressed/not pressed button). Default "Wrap lines". */
  wrap: string;
  /** Apply action label. Default "Apply". */
  apply: string;
  /** Insert action label. Default "Insert". */
  insert: string;
  /** Spoken before an added diff line. Default "Added". */
  added: string;
  /** Spoken before a removed diff line. Default "Removed". */
  removed: string;
  /**
   * Names the code: the scroll region's label and the hidden target of the
   * copy, apply and insert buttons. Default: the filename, else
   * "TypeScript code" (from the language), else "Code".
   */
  label: (info: { filename?: string | undefined; language?: string | undefined }) => string;
}

const LANGUAGE_NAMES: Record<string, string> = {
  js: 'JavaScript',
  javascript: 'JavaScript',
  mjs: 'JavaScript',
  cjs: 'JavaScript',
  jsx: 'JSX',
  ts: 'TypeScript',
  typescript: 'TypeScript',
  tsx: 'TSX',
  json: 'JSON',
  jsonc: 'JSON',
  py: 'Python',
  python: 'Python',
  sh: 'Shell',
  bash: 'Bash',
  zsh: 'Shell',
  shell: 'Shell',
  console: 'Shell',
  css: 'CSS',
  scss: 'SCSS',
  html: 'HTML',
  xml: 'XML',
  svg: 'SVG',
  sql: 'SQL',
  go: 'Go',
  golang: 'Go',
  rust: 'Rust',
  rs: 'Rust',
  diff: 'Diff',
  yaml: 'YAML',
  yml: 'YAML',
  md: 'Markdown',
  markdown: 'Markdown',
};

/** A display name for a language id ("ts" → "TypeScript"); unknown ids are returned as given. */
export function codeLanguageName(language: string | undefined): string | undefined {
  const id = (language ?? '').trim();
  if (!id) return undefined;
  return LANGUAGE_NAMES[id.toLowerCase()] ?? id;
}

export const AI_CODE_BLOCK_MESSAGES: AiCodeBlockMessages = {
  copy: 'Copy',
  copied: 'Copied',
  copyError: "Couldn't copy",
  wrap: 'Wrap lines',
  apply: 'Apply',
  insert: 'Insert',
  added: 'Added',
  removed: 'Removed',
  label: ({ filename, language }) => {
    if (filename) return filename;
    const name = codeLanguageName(language);
    return name ? `${name} code` : 'Code';
  },
};

export interface AiCodeBlockProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The code to show, exactly as it should be copied. */
  code: string;
  /**
   * Language id ("ts", "python", "bash", "diff"…). Picks the highlighter and
   * the header label. Unknown languages render as plain text.
   */
  language?: string | undefined;
  /** Shown in the header instead of the language, and used to name the block. */
  filename?: string | undefined;
  /** Show line numbers in a gutter. They are hidden from screen readers and from selection. Default false. */
  lineNumbers?: boolean;
  /**
   * Render as a diff: `+` and `-` lines get the diff colours and a visible
   * `+` / `−` marker, `@@` lines are hunk headers. Default: true when
   * `language` is "diff" or "patch".
   */
  diff?: boolean;
  /** Turn syntax colouring off. Default true. */
  highlight?: boolean;
  /** Wrap long lines (controlled). Without it the block scrolls sideways. */
  wrap?: boolean;
  /** Initial wrap state when uncontrolled. Default false. */
  defaultWrap?: boolean;
  /** Called when the wrap toggle is pressed. */
  onWrapChange?: (wrap: boolean) => void;
  /** Show the wrap toggle. Default true. */
  showWrapToggle?: boolean;
  /** Show the copy button. Default true. */
  copyable?: boolean;
  /** Shows an "Apply" action that receives the code (for example, apply an edit to a file). */
  onApply?: (code: string) => void;
  /** Shows an "Insert" action that receives the code (for example, insert at the cursor). */
  onInsert?: (code: string) => void;
  /**
   * The code is still arriving (a streaming answer): Apply and Insert are
   * disabled until it is complete. Copy stays available.
   */
  incomplete?: boolean;
  /** Extra controls placed in the header before the built-in actions. */
  actions?: ReactNode;
  /** Replaces any of the English strings. */
  messages?: Partial<AiCodeBlockMessages> | undefined;
}

type DiffKind = 'add' | 'remove' | 'hunk' | 'meta' | 'context';

function diffKinds(lines: readonly string[]): DiffKind[] {
  let inHunk = false;
  return lines.map((line) => {
    if (line.startsWith('@@')) {
      inHunk = true;
      return 'hunk';
    }
    if (
      !inHunk &&
      /^(diff |index |\+\+\+ |--- |new file|deleted file|similarity|rename )/.test(line)
    )
      return 'meta';
    if (line.startsWith('+')) return 'add';
    if (line.startsWith('-')) return 'remove';
    return 'context';
  });
}

function renderTokens(tokens: readonly CodeToken[]): ReactNode {
  return tokens.map((t, i) =>
    t.type === 'plain' ? (
      t.text
    ) : (
      <span key={i} className="mn-ai-code-block__token" data-token={t.type}>
        {t.text}
      </span>
    ),
  );
}

/**
 * Shows a code sample with a filename or language header, copy, a wrap
 * toggle, optional line numbers, light syntax colouring and a diff mode.
 * Optional Apply and Insert actions hand the code back to the product. The
 * code scrolls sideways inside its own focusable, labelled region, so the
 * page never scrolls horizontally. Works anywhere, not only in AI answers.
 */
export const AiCodeBlock = forwardRef<HTMLDivElement, AiCodeBlockProps>(function AiCodeBlock(
  {
    code,
    language,
    filename,
    lineNumbers = false,
    diff,
    highlight = true,
    wrap: wrapProp,
    defaultWrap = false,
    onWrapChange,
    showWrapToggle = true,
    copyable = true,
    onApply,
    onInsert,
    incomplete = false,
    actions,
    messages,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...AI_CODE_BLOCK_MESSAGES, ...messages };
  const [wrapState, setWrapState] = useState(defaultWrap);
  const wrap = wrapProp ?? wrapState;
  const isDiff = diff ?? /^(diff|patch)$/i.test(language ?? '');
  const label = m.label({ filename, language });
  const title = filename ?? codeLanguageName(language);

  const lines = useMemo(() => {
    const source = code.endsWith('\n') ? code.slice(0, -1) : code;
    if (isDiff) {
      const raw = source.split('\n');
      const kinds = diffKinds(raw);
      return raw.map((text, i) => {
        const kind = kinds[i] as DiffKind;
        const body =
          kind === 'add' || kind === 'remove' || (kind === 'context' && text.startsWith(' '))
            ? text.slice(1)
            : text;
        return { kind, tokens: body ? [{ type: 'plain', text: body } as CodeToken] : [] };
      });
    }
    const tokens =
      highlight && resolveCodeLanguage(language)
        ? tokenizeCode(source, language)
        : [{ type: 'plain' as const, text: source }];
    return tokensToLines(tokens).map((t) => ({ kind: undefined, tokens: t }));
  }, [code, language, highlight, isDiff]);

  const toggleWrap = () => {
    const next = !wrap;
    if (wrapProp === undefined) setWrapState(next);
    onWrapChange?.(next);
  };

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-ai-code-block', className)}
      data-wrap={wrap || undefined}
      data-diff={isDiff || undefined}
      data-line-numbers={lineNumbers || undefined}
      data-incomplete={incomplete || undefined}
    >
      <div className="mn-ai-code-block__header">
        <span className="mn-ai-code-block__title" title={title}>
          {title}
        </span>
        <div className="mn-ai-code-block__actions">
          {actions}
          {showWrapToggle ? (
            <Button
              size="sm"
              variant="ghost"
              aria-pressed={wrap}
              onClick={toggleWrap}
              className="mn-ai-code-block__wrap"
            >
              {m.wrap}
            </Button>
          ) : null}
          {copyable ? (
            <CopyButton
              value={code}
              size="sm"
              variant="ghost"
              label={m.copy}
              copiedLabel={m.copied}
              errorLabel={m.copyError}
              target={label}
            />
          ) : null}
          {onInsert ? (
            <Button
              size="sm"
              variant="secondary"
              disabled={incomplete}
              onClick={() => onInsert(code)}
            >
              {m.insert}
              <span className="mn-visually-hidden"> {label}</span>
            </Button>
          ) : null}
          {onApply ? (
            <Button size="sm" variant="primary" disabled={incomplete} onClick={() => onApply(code)}>
              {m.apply}
              <span className="mn-visually-hidden"> {label}</span>
            </Button>
          ) : null}
        </div>
      </div>
      {/* The region is focusable so keyboard users can scroll long lines. */}
      {/* eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex */}
      <div className="mn-ai-code-block__scroll" role="region" aria-label={label} tabIndex={0}>
        <pre className="mn-ai-code-block__pre">
          <code className="mn-ai-code-block__code" data-language={language || undefined}>
            {lines.map((line, i) => (
              <span key={i} className="mn-ai-code-block__line" data-diff={line.kind}>
                {lineNumbers ? (
                  <span className="mn-ai-code-block__number" aria-hidden="true">
                    {i + 1}
                  </span>
                ) : null}
                {isDiff ? (
                  <span className="mn-ai-code-block__marker" aria-hidden="true">
                    {line.kind === 'add' ? '+' : line.kind === 'remove' ? '−' : ' '}
                  </span>
                ) : null}
                <span className="mn-ai-code-block__content">
                  {line.kind === 'add' || line.kind === 'remove' ? (
                    <span className="mn-visually-hidden">
                      {line.kind === 'add' ? m.added : m.removed}:{' '}
                    </span>
                  ) : null}
                  {line.tokens.length ? renderTokens(line.tokens) : ' '}
                </span>
              </span>
            ))}
          </code>
        </pre>
      </div>
    </div>
  );
});
