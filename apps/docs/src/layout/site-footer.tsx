import { href } from '../lib/router.js';
import { REPO_URL } from './top-bar.js';

/** Site footer: licence and source, nothing else. */
export function SiteFooter() {
  return (
    <footer className="docs-footer">
      <div className="docs-footer__inner">
        <p>
          Monarch is made by Rook and open source under the{' '}
          <a href={`${REPO_URL}/blob/main/LICENSE`}>MIT License</a>.
        </p>
        <ul className="docs-footer__links">
          <li>
            <a href={href('/getting-started')}>Get started</a>
          </li>
          <li>
            <a href={href('/components')}>Components</a>
          </li>
          <li>
            <a href={REPO_URL}>Source on GitHub</a>
          </li>
        </ul>
      </div>
    </footer>
  );
}
