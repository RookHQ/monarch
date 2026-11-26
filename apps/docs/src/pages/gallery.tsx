import { useEffect } from 'react';
import { useAllDocs } from '../lib/docs.js';
import { GALLERY } from './gallery-ids.js';
import { renderExample } from '../features/examples/render-example.js';

export { GALLERY };

/**
 * Visual-regression surface: every example of every gallery component, no site
 * chrome. Each component is one `[data-gallery-id]` section and is screenshotted
 * on its own, so adding a component adds baselines without changing the others.
 * `#/__gallery/<id>` renders a single component. `#/__gallery/<id>/live` renders it
 * without overlay stages (used by the overlay shots): an example that starts open
 * then opens as the real, portalled overlay.
 * Theme/contrast/product come from <html> attributes set by the test via the
 * normal cookies, so the gallery renders exactly as an app would.
 *
 * Every docs file is loaded before the first render, so moving between
 * `#/__gallery/<id>` routes never waits on a chunk.
 */
export function GalleryPage({ only, live = false }: { only?: string; live?: boolean } = {}) {
  const all = useAllDocs();
  useEffect(() => {
    document.title = 'Gallery · Monarch';
  }, []);
  const ids = only ? [only] : GALLERY;
  return (
    <main className="gallery" aria-label="Component gallery" data-testid="gallery">
      <h1 className="mn-visually-hidden">Monarch component gallery</h1>
      {ids.map((id) => {
        const loaded = all[id];
        if (!loaded) throw new Error(`gallery: no docs file for ${id}`);
        const { docs } = loaded;
        const examples = 'items' in docs.sections.examples ? docs.sections.examples.items : [];
        return (
          <section
            key={id}
            className="gallery-component"
            data-gallery-id={id}
            aria-label={docs.title}
          >
            {examples.map((ex) => (
              <div
                key={ex.title}
                className="gallery-example"
                data-stack={ex.stack || undefined}
                role="group"
                aria-label={ex.title}
              >
                <h2>
                  {docs.title} · {ex.title}
                </h2>
                {live ? ex.render() : renderExample(ex)}
              </div>
            ))}
          </section>
        );
      })}
    </main>
  );
}
