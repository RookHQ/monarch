import { PageFrame } from '../layout/page-frame.js';
import { href } from '../lib/router.js';

export function NotFoundPage() {
  return (
    <PageFrame title="Page not found" toc={false}>
      <p className="docs-text">
        There is no page at this address. <a href={href('/')}>Back to the start</a>, or search with
        the box at the top of the page.
      </p>
    </PageFrame>
  );
}
