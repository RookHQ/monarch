import { useId, useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import type { Example } from '../../lib/docs.js';
import { CodeIcon } from '../../ui/icons.js';
import { CodeBlock } from '../code/code-block.js';
import { PreviewBoundary } from './preview-boundary.js';

/**
 * One live example: the preview on a canvas, then a "Show code" disclosure
 * with the example's extracted source (plugins/extract-examples.ts).
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
  return (
    // A named group, not a section: example titles may repeat a reference section's title.
    <div role="group" className="docs-example" data-size={size} aria-labelledby={headingId} id={id}>
      <Heading id={headingId} className="docs-example__title">
        {example.title}
      </Heading>
      <div className="docs-example__frame">
        <div className="docs-example__preview" data-stack={example.stack || undefined}>
          <PreviewBoundary name={example.title}>{example.render()}</PreviewBoundary>
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
