import { useOptionalConsent } from '@rookcom/monarch-react';
import { CookieSettingsIcon } from '../features/consent/docs-consent.js';
import { MAKER_LINK, NEW_TAB } from '../lib/external.js';
import { href } from '../lib/router.js';
import { REPO_URL } from './top-bar.js';

/** Site footer: licence and source, plus privacy links when the hosted site runs analytics. */
export function SiteFooter() {
  const consent = useOptionalConsent();
  return (
    <footer className="docs-footer">
      <div className="docs-footer__inner">
        <p>
          Monarch is made by <a {...MAKER_LINK}>Rook</a> and open source under the{' '}
          <a {...NEW_TAB} href={`${REPO_URL}/blob/main/LICENSE`}>
            Apache License 2.0
          </a>
          . The Rook and Monarch names and logos are trademarks of Rook Platforms.
        </p>
        <ul className="docs-footer__links">
          <li>
            <a href={href('/getting-started')}>Get started</a>
          </li>
          <li>
            <a href={href('/components')}>Components</a>
          </li>
          <li>
            <a {...NEW_TAB} href={REPO_URL}>
              Source on GitHub
            </a>
          </li>
          <li>
            <a {...NEW_TAB} href={`${REPO_URL}/blob/main/TRADEMARK.md`}>
              Trademarks
            </a>
          </li>
          {consent ? (
            <>
              <li>
                <a {...NEW_TAB} href="https://rookhq.com/privacy">
                  Privacy
                </a>
              </li>
              <li>
                <CookieSettingsIcon />
              </li>
            </>
          ) : null}
        </ul>
      </div>
    </footer>
  );
}
