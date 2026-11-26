import { Suspense, useRef } from 'react';
import { examplesOf, useDocs, type DocsPageMeta } from '../../lib/docs.js';
import { href } from '../../lib/router.js';
import { PreviewBoundary } from './preview-boundary.js';
import { useNearViewport } from './use-near-viewport.js';

/** The first example of a docs file, as a non-interactive thumbnail. */
function Thumbnail({ id }: { id: string }) {
  const { docs } = useDocs(id);
  const example = examplesOf(docs)[0];
  if (!example) return null;
  return (
    <div className="docs-card__stage" data-stack={example.stack || undefined}>
      <PreviewBoundary name={docs.title}>{example.render()}</PreviewBoundary>
    </div>
  );
}

/**
 * Overview card: a live, scaled-down preview of the component's first example,
 * its name (the card's one link) and summary (from the page details, which the
 * page using the card loads). The preview renders once the card
 * nears the viewport, in a frame of fixed height, so nothing shifts. It is
 * `inert` and hidden from assistive technology: it is a picture of the
 * component, and the page it links to has the real, interactive examples.
 */
export function ComponentCard({
  page,
  summary,
  headingLevel = 3,
}: {
  page: DocsPageMeta;
  summary: string | undefined;
  headingLevel?: 2 | 3;
}) {
  const ref = useRef<HTMLDivElement>(null);
  const visible = useNearViewport(ref, '400px');
  const Heading = headingLevel === 2 ? 'h2' : 'h3';
  return (
    <li className="docs-card">
      <div ref={ref} className="docs-card__preview" aria-hidden inert>
        {visible ? (
          <Suspense fallback={null}>
            <Thumbnail id={page.id} />
          </Suspense>
        ) : null}
      </div>
      <div className="docs-card__body">
        <Heading className="docs-card__title">
          <a className="docs-card__link" href={href(`/components/${page.id}`)}>
            {page.title}
          </a>
        </Heading>
        <p className="docs-card__summary">{summary}</p>
      </div>
    </li>
  );
}
