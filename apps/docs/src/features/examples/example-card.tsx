import { useId, useRef, useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import type { Example } from '../../lib/docs.js';
import { CodeIcon } from '../../ui/icons.js';
import { CodeBlock } from '../code/code-block.js';
import { PreviewBoundary } from './preview-boundary.js';
import { useNearViewport } from './use-near-viewport.js';

/**
 * One live example: the preview on a canvas, then a "Show code" disclosure
 * with the example's extracted source (plugins/extract-examples.ts). The
 * headline example (`size="lg"`) is rendered at once, and so is part of the
 * prerendered page; any other preview mounts when it nears the viewport, and
 * until then its canvas keeps a reserved height (examples.css).
 */
export function ExampleCard({
  example,
  code,
  size = 'md',
  headingLevel = 3,
  id,
}: {
  example: Example;
  code: string;
  /** `lg`: the page's headline preview, taller and centred. */
  size?: 'md' | 'lg';
  headingLevel?: 2 | 3;
  /** Anchor id for the table of contents. */
  id?: string;
}) {
  const [open, setOpen] = useState(false);
  const codeId = useId();
  const headingId = useId();
  const Heading = headingLevel === 2 ? 'h2' : 'h3';
  const preview = useRef<HTMLDivElement>(null);
  const near = useNearViewport(preview) || size === 'lg';
  return (
    // A named group, not a section: example titles may repeat a reference section's title.
    <div role="group" className="docs-example" data-size={size} aria-labelledby={headingId} id={id}>
      <Heading id={headingId} className="docs-example__title">
        {example.title}
      </Heading>
      <div className="docs-example__frame">
        <div
          ref={preview}
          className="docs-example__preview"
          data-stack={example.stack || undefined}
          data-pending={near ? undefined : ''}
        >
          {near ? <PreviewBoundary name={example.title}>{example.render()}</PreviewBoundary> : null}
        </div>
        <div className="docs-example__bar">
          <Button
            size="sm"
            variant="ghost"
            leadingIcon={<CodeIcon />}
            aria-expanded={open}
            aria-controls={codeId}
            onClick={() => setOpen((o) => !o)}
          >
            {open ? 'Hide code' : 'Show code'}
          </Button>
        </div>
        <CodeBlock id={codeId} hidden={!open} code={code} label={`Code: ${example.title}`} />
      </div>
    </div>
  );
}
