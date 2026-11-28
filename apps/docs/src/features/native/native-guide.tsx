import { PageFrame } from '../../layout/page-frame.js';

/**
 * A native guide (`docs/guides/native-*.md`, NATIVE-COMPATIBILITY.md) rendered
 * to HTML at build time by plugins/native.ts. A guide whose file does not
 * exist has no route (app/routes.tsx), so `guide` is null only in theory.
 */
export function NativeGuide({
  guide,
}: {
  guide: { title: string; lead: string | null; html: string } | null;
}) {
  if (!guide)
    return (
      <PageFrame eyebrow="iOS and Android" title="Not written yet" toc={false}>
        <p className="docs-text">This guide is not in the repository yet.</p>
      </PageFrame>
    );
  return (
    <PageFrame
      eyebrow="iOS and Android"
      title={guide.title}
      lead={guide.lead ? <span dangerouslySetInnerHTML={{ __html: guide.lead }} /> : undefined}
    >
      <div className="docs-prose" dangerouslySetInnerHTML={{ __html: guide.html }} />
    </PageFrame>
  );
}
