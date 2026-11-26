import { useMemo, useState } from 'react';
import { FilterChip } from '@rookcom/monarch-react';
import {
  FAMILY_TITLES,
  docsPages,
  isMarketing,
  reactVersion,
  useDetails,
  type DocsPageMeta,
} from '../../lib/docs.js';
import { PageFrame } from '../../layout/page-frame.js';
import { ComponentCard } from '../../features/examples/component-card.js';

const ALL = 'all';

/**
 * A card per component (or per pattern), grouped by taxonomy family, with a
 * filter per family. Counts are the number of docs files, nothing else.
 */
export function OverviewPage({ layer }: { layer: 'all' | 'patterns' }) {
  const pages = useMemo(
    () =>
      layer === 'patterns'
        ? docsPages.filter((p) => p.layer === 'patterns' && !isMarketing(p))
        : docsPages,
    [layer],
  );
  const families = useMemo(
    () => Object.keys(FAMILY_TITLES).filter((f) => pages.some((p) => p.family === f)),
    [pages],
  );
  const { details } = useDetails();
  const [family, setFamily] = useState<string>(ALL);
  const shown = families.filter((f) => family === ALL || f === family);
  const byFamily = (f: string): DocsPageMeta[] => pages.filter((p) => p.family === f);
  const isPatterns = layer === 'patterns';

  return (
    <PageFrame
      eyebrow={isPatterns ? 'Patterns' : 'Components'}
      title={isPatterns ? 'Patterns' : 'Components'}
      lead={
        isPatterns ? (
          <>
            {pages.length} patterns: compositions of components for screens most applications have.
            They carry layout, semantics and accessibility; data, routing and state stay in the app.
          </>
        ) : (
          <>
            {pages.length} components and patterns in{' '}
            <code className="docs-code-inline">@rookcom/monarch-react</code> {reactVersion}. Each
            page is generated from the component's docs file and its TypeScript declarations.
          </>
        )
      }
      toc={false}
      wide
    >
      <div className="docs-filters" role="group" aria-label="Filter by category">
        <FilterChip
          name="All"
          count={pages.length}
          pressed={family === ALL}
          onPressedChange={() => setFamily(ALL)}
        />
        {families.map((f) => (
          <FilterChip
            key={f}
            name={FAMILY_TITLES[f] ?? f}
            count={byFamily(f).length}
            pressed={family === f}
            onPressedChange={(on) => setFamily(on ? f : ALL)}
          />
        ))}
      </div>
      {shown.map((f) => (
        <section key={f} className="docs-family" aria-labelledby={`family-${f}`}>
          <h2 id={`family-${f}`} className="docs-family__title">
            {FAMILY_TITLES[f] ?? f}
            <span className="docs-family__count">{byFamily(f).length}</span>
          </h2>
          <ul className="docs-cards">
            {byFamily(f).map((p) => (
              <ComponentCard key={p.id} page={p} summary={details[p.id]?.summary} />
            ))}
          </ul>
        </section>
      ))}
    </PageFrame>
  );
}
