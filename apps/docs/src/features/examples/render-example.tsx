import type { ReactNode } from 'react';
import { OverlayStage } from '../../../../../packages/react/src/internal/overlay-stage.js';
import type { Example } from '../../lib/docs.js';

/**
 * An example's live render. With `stage`, the render sits in an overlay stage
 * so the overlay it opens stays in the preview, open and in the flow.
 */
export function renderExample(example: Example): ReactNode {
  const out = example.render();
  if (!example.stage) return out;
  const place = typeof example.stage === 'object' ? example.stage : {};
  return (
    <OverlayStage
      {...(place.side ? { side: place.side } : {})}
      {...(place.align ? { align: place.align } : {})}
    >
      {out}
    </OverlayStage>
  );
}
