import gettingStarted from '../../../../../docs/guides/getting-started.md?guide';
import theming from '../../../../../docs/guides/theming.md?guide';
import migration from '../../../../../docs/guides/migration.md?guide';
import marketing from '../../../../../docs/guides/marketing.md?guide';
import patterns from '../../../../../docs/guides/patterns.md?guide';
import conformance from '../../../../../docs/guides/conformance.md?guide';
import { PageFrame } from '../../layout/page-frame.js';

/** Guides from `docs/guides`, rendered to HTML at build time (plugins/guides.ts), so the site cannot drift from the repository. */
const GUIDES = {
  'getting-started': gettingStarted,
  theming,
  migration,
  marketing,
  patterns,
  conformance,
} as const;
export type GuideId = keyof typeof GUIDES;

export function GuidePage({ guide, eyebrow }: { guide: GuideId; eyebrow: string }) {
  const { title, lead, html } = GUIDES[guide];
  return (
    <PageFrame
      eyebrow={eyebrow}
      title={title}
      lead={lead ? <span dangerouslySetInnerHTML={{ __html: lead }} /> : undefined}
    >
      <div className="docs-prose" dangerouslySetInnerHTML={{ __html: html }} />
    </PageFrame>
  );
}
