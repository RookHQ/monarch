import { useEffect } from 'react';
import { pageById } from '../lib/components.js';
import { GALLERY } from './gallery-ids.js';

export { GALLERY };

/**
 * Visual-regression surface: every example of every gallery component, no site
 * chrome. Each component is one `[data-gallery-id]` section and is screenshotted
 * on its own, so adding a component adds baselines without changing the others.
 * `#/__gallery/<id>` renders a single component (used by the overlay shots).
 * Theme/contrast/product come from <html> attributes set by the test via the
 * normal cookies, so the gallery renders exactly as an app would.
 */
export function GalleryPage({ only }: { only?: string } = {}) {
  useEffect(() => {
    document.title = 'Gallery · Monarch';
  }, []);
  const ids = only ? [only] : GALLERY;
  return (
    <main className="gallery" aria-label="Component gallery" data-testid="gallery">
      <h1 className="mn-visually-hidden">Monarch component gallery</h1>
      {ids.map((id) => {
        const page = pageById[id];
        if (!page) throw new Error(`gallery: no docs file for ${id}`);
        const { docs } = page;
        const examples = 'items' in docs.sections.examples ? docs.sections.examples.items : [];
        return (
          <section
            key={id}
            className="gallery-component"
            data-gallery-id={id}
            aria-label={docs.title}
          >
            {examples.map((ex) => (
              <div key={ex.title} className="gallery-example" role="group" aria-label={ex.title}>
                <h2>
                  {docs.title} · {ex.title}
                </h2>
                {ex.render()}
              </div>
            ))}
          </section>
        );
      })}
    </main>
  );
}
