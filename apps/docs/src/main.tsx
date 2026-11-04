import { StrictMode } from 'react';
import { createRoot, hydrateRoot } from 'react-dom/client';
import './styles/fonts.css';
import '@rookcom/monarch-tokens/css';
import '@rookcom/monarch-tokens/css/base';
import '@rookcom/monarch-react/styles.css';
import './styles/base.css';
import './styles/gallery.css';
import './styles/shell.css';
import './styles/content.css';
import './styles/examples.css';
// Every product accent, so the playground can preview any Rook product.
import.meta.glob('../../../node_modules/@rookcom/monarch-tokens/dist/css/products/*.css', {
  eager: true,
});
import { App } from './app/app.js';
import { preloadRoute, resolveRoute } from './app/routes.js';
import { ROUTE_ALIASES } from './lib/docs.js';
import { isTestRoute, legacyRedirect } from './lib/router.js';

const root = document.getElementById('root');
if (!root) throw new Error('#root missing');

// An old `#/components/button` link becomes `/components/button/` first.
const initial = legacyRedirect(ROUTE_ALIASES);
const app = (
  <StrictMode>
    <App initial={initial} />
  </StrictMode>
);

/**
 * Every page is prerendered (scripts/prerender.mjs) and records its route on
 * #root. When that is the page being opened, React hydrates it: the HTML on
 * screen stays and becomes interactive. Otherwise (a screenshot route, an old
 * hash link, an address the server answered with another page) the app
 * renders from scratch, exactly as a single-page app does.
 */
const rendered = root.dataset.route;
const path = ROUTE_ALIASES[initial.path] ?? initial.path;
const hydrate =
  !isTestRoute(path) &&
  rendered !== undefined &&
  (rendered === path || (rendered === '/404' && resolveRoute(path).notFound === true));

if (hydrate) {
  // The page's chunk is preloaded by the HTML; wait for it so hydration runs in one pass.
  void preloadRoute(path).then(() => hydrateRoot(root, app));
} else {
  // Styles of the prerendered page (not the one being rendered) must not reach
  // the screenshot routes, which render exactly as they did before prerendering.
  for (const link of document.querySelectorAll('link[data-route-css]')) link.remove();
  root.replaceChildren();
  delete root.dataset.route;
  createRoot(root).render(app);
}
