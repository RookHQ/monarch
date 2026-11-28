import { Badge } from '@rookcom/monarch-react';
import { COVERAGE, PATTERN, STATUS, type Coverage, type Status } from './labels.js';

/**
 * Available, Experimental, Planned or Deprecated. The word carries the
 * meaning; the tone and the outline (planned: not there yet) only support it.
 */
export function StatusBadge({ status }: { status: Status }) {
  const { label, tone } = STATUS[status];
  return (
    <Badge size="sm" tone={tone} variant={status === 'planned' ? 'outline' : 'subtle'}>
      {label}
    </Badge>
  );
}

/**
 * Status of an inventory entry: in the patterns layer, "Pattern
 * (experimental)" stands in for the status, which is always experimental there.
 */
export function EntryStatusBadge({
  status,
  pattern,
}: {
  status: Status;
  pattern?: boolean | undefined;
}) {
  return pattern ? (
    <Badge size="sm" tone="warning" variant="subtle">
      {PATTERN.label}
    </Badge>
  ) : (
    <StatusBadge status={status} />
  );
}

/** How the platform covers the component. */
export function CoverageBadge({ coverage }: { coverage: Coverage }) {
  return (
    <Badge size="sm" variant="outline">
      {COVERAGE[coverage].label}
    </Badge>
  );
}
