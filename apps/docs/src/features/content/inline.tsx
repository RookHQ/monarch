import type { ReactNode } from 'react';

/** Renders `code` spans written with backticks in docs strings. */
export function Inline({ text }: { text: string }) {
  return (
    <>
      {text.split(/(`[^`]+`)/).map((part, i) =>
        part.startsWith('`') && part.endsWith('`') ? (
          <code key={i} className="docs-code-inline">
            {part.slice(1, -1)}
          </code>
        ) : (
          part
        ),
      )}
    </>
  );
}

export const inline = (items?: string[]) => items?.map((t) => <Inline key={t} text={t} />);

export function List({ items }: { items?: ReactNode[] | undefined }) {
  return items?.length ? (
    <ul className="docs-list">
      {items.map((item, i) => (
        <li key={i}>{item}</li>
      ))}
    </ul>
  ) : null;
}

export function Notes({ notes }: { notes?: string[] | undefined }) {
  return notes?.length ? (
    <>
      {notes.map((n) => (
        <p key={n} className="docs-text">
          <Inline text={n} />
        </p>
      ))}
    </>
  ) : null;
}

export function Table({
  head,
  rows,
  label,
}: {
  head: string[];
  rows: ReactNode[][];
  /** Names the scroll region for assistive technology; defaults to the column headings. */
  label?: string;
}) {
  return (
    // A wide table scrolls sideways, so its wrapper is a named, focusable region
    // that keyboard users can scroll (WCAG 2.1.1; axe scrollable-region-focusable).
    <div
      className="docs-table-scroll"
      role="region"
      aria-label={label ?? head.join(', ')}
      tabIndex={0}
    >
      <table className="docs-table">
        <thead>
          <tr>
            {head.map((h) => (
              <th key={h} scope="col">
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((r, i) => (
            <tr key={i}>
              {r.map((c, j) => (
                <td key={j}>{c}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
