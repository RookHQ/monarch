import { Suspense, lazy, useEffect, useState, type ComponentType } from 'react';
import type { NativeComponentDocs } from '../../../plugins/native.js';

const loadSections = () => import('../../features/native/native-sections.js');

/** The prerender step renders the sections in place and waits for their data. */
const Prerendered = /* @__PURE__ */ lazy(() =>
  loadSections().then((m) => ({ default: m.PrerenderedNativeSections })),
);

interface Live {
  Sections: ComponentType<{ title: string; docs: NativeComponentDocs }>;
  docs: NativeComponentDocs;
}

/**
 * The prerendered sections of the page being opened, read once when this
 * module loads, which is before hydration (the page's chunk is awaited
 * first). A page reached by client-side navigation has none.
 */
const prerendered = ((): { route: string; html: string } | null => {
  if (typeof document === 'undefined') return null;
  const root = document.getElementById('root');
  const slot = root?.querySelector('.docs-native-slot');
  return root?.dataset.route && slot ? { route: root.dataset.route, html: slot.innerHTML } : null;
})();

/**
 * In the browser the prerendered sections stay exactly as the HTML has them:
 * this element's content is that same HTML, so React hydrates around it and
 * renders nothing inside. Once the page has hydrated, their code and the
 * component's native data load (the tabs are at the top of the page, so they
 * are always in view), and the live sections replace the static ones in one
 * commit, with the same markup. A page reached by client-side navigation has
 * no HTML to keep.
 */
function BrowserSlot({ id, title }: { id: string; title: string }) {
  const [live, setLive] = useState<Live | null>(null);
  const html = prerendered?.route === `/components/${id}` ? prerendered.html : '';
  useEffect(() => {
    let cancelled = false;
    loadSections()
      .then((m) => m.loadNative(id).then((docs) => ({ Sections: m.LiveNativeSections, docs })))
      .then(
        (loaded) => {
          if (!cancelled) setLive(loaded);
        },
        // Offline or a failed request: the prerendered sections stay.
        () => undefined,
      );
    return () => {
      cancelled = true;
    };
  }, [id]);
  if (live)
    return (
      <div key="live" className="docs-native-slot">
        <live.Sections title={title} docs={live.docs} />
      </div>
    );
  return (
    <div key="static" className="docs-native-slot" dangerouslySetInnerHTML={{ __html: html }} />
  );
}

/**
 * The Web | iOS | Android tabs of a component page, with the iOS and Android
 * panels. They are in the prerendered HTML (for readers, crawlers and the
 * Markdown copy), but their code and data are not part of what the page loads
 * up front, and they never hold up hydration of the rest.
 */
export function NativeSlot({ id, title }: { id: string; title: string }) {
  if (import.meta.env.SSR)
    return (
      <div className="docs-native-slot">
        <Suspense fallback={null}>
          <Prerendered id={id} title={title} />
        </Suspense>
      </div>
    );
  return <BrowserSlot id={id} title={title} />;
}
