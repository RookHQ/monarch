import { useMemo } from 'react';
import { CopyButton } from '@rookcom/monarch-react';
import { highlight, type Token } from './highlight.js';

/**
 * A highlighted, copyable code block. The `<pre>` is a focusable, named region
 * so long lines can be scrolled from the keyboard (axe `scrollable-region-focusable`).
 */
export function CodeBlock({
  code,
  label,
  copy = true,
  id,
  hidden,
  lang,
  tokenize,
}: {
  code: string;
  /** Accessible name of the region, unique on the page ("Code: Variants"). */
  label: string;
  /** Show a copy button in the corner. */
  copy?: boolean;
  id?: string;
  hidden?: boolean;
  /** The language, when it is not TSX: the fence language of the page's Markdown copy. */
  lang?: string;
  /** Highlighter for `lang`; the TSX one when absent. */
  tokenize?: (code: string) => Token[];
}) {
  const tokens = useMemo(() => (tokenize ?? highlight)(code), [code, tokenize]);
  return (
    <div className="docs-code" id={id} hidden={hidden}>
      <pre
        className="docs-code__pre"
        tabIndex={0}
        role="region"
        aria-label={label}
        data-lang={lang}
      >
        <code>
          {tokens.map((t, i) =>
            t.kind ? (
              <span key={i} className="docs-tok" data-kind={t.kind}>
                {t.text}
              </span>
            ) : (
              t.text
            ),
          )}
        </code>
      </pre>
      {copy ? (
        <CopyButton
          className="docs-code__copy"
          value={code}
          size="xs"
          variant="ghost"
          iconOnly
          label={`Copy ${label.replace(/^Code: /, '')} code`}
          copiedLabel="Copied"
        />
      ) : null}
    </div>
  );
}
