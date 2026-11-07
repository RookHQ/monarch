/** Internal glyphs. Monarch ships no icon set; apps keep using lucide-react. */
import type { SVGProps } from 'react';

const base = {
  width: 16,
  height: 16,
  viewBox: '0 0 16 16',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.75,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  'aria-hidden': true,
  focusable: false,
} as const;

export function CheckGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <path d="M3.5 8.5l3 3 6-7" />
    </svg>
  );
}

export function DashGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <path d="M4 8h8" />
    </svg>
  );
}

export function ChevronDownGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <path d="M4 6l4 4 4-4" />
    </svg>
  );
}

export function ChevronRightGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <path d="M6 4l4 4-4 4" />
    </svg>
  );
}

export function CloseGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <path d="M4 4l8 8M12 4l-8 8" />
    </svg>
  );
}

export function DotGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <circle cx="8" cy="8" r="2.5" fill="currentColor" stroke="none" />
    </svg>
  );
}

export function InfoGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <circle cx="8" cy="8" r="6.25" />
      <path d="M8 7.25v3.5M8 5.25h.01" />
    </svg>
  );
}

export function WarningGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <path d="M8 2.25l6.25 11H1.75z" />
      <path d="M8 6.5v3M8 11.25h.01" />
    </svg>
  );
}

export function SuccessGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <circle cx="8" cy="8" r="6.25" />
      <path d="M5.5 8.25l1.75 1.75 3.25-3.75" />
    </svg>
  );
}

export function CopyGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <rect x="5.25" y="5.25" width="8" height="8" rx="1.5" />
      <path d="M10.75 5.25V3.75a1.5 1.5 0 0 0-1.5-1.5h-5.5a1.5 1.5 0 0 0-1.5 1.5v5.5a1.5 1.5 0 0 0 1.5 1.5h1.5" />
    </svg>
  );
}

export function EyeGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <path d="M1.75 8s2.25-4.5 6.25-4.5S14.25 8 14.25 8 12 12.5 8 12.5 1.75 8 1.75 8z" />
      <circle cx="8" cy="8" r="1.75" />
    </svg>
  );
}

export function EyeOffGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <path d="M6.6 3.65A6 6 0 0 1 8 3.5c4 0 6.25 4.5 6.25 4.5a11 11 0 0 1-1.6 2.2M9.4 9.4a1.75 1.75 0 0 1-2.8-2.05M4.2 4.95A10.6 10.6 0 0 0 1.75 8S4 12.5 8 12.5a5.9 5.9 0 0 0 3.05-.85" />
      <path d="M2 2l12 12" />
    </svg>
  );
}

export function ErrorGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <circle cx="8" cy="8" r="6.25" />
      <path d="M6 6l4 4M10 6l-4 4" />
    </svg>
  );
}

/** A half-filled circle: "partly working". */
export function PartialGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <circle cx="8" cy="8" r="6.25" />
      <path d="M8 1.75a6.25 6.25 0 0 1 0 12.5z" fill="currentColor" />
    </svg>
  );
}

export function WrenchGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <path d="M9.9 2.4a3.25 3.25 0 0 0-3.6 4.4L2.4 10.7a1.4 1.4 0 0 0 2 2l3.9-3.9a3.25 3.25 0 0 0 4.4-3.6l-2 2-1.9-.5-.5-1.9z" />
    </svg>
  );
}

export function HelpGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <circle cx="8" cy="8" r="6.25" strokeDasharray="2.2 1.8" />
      <path d="M6.4 6.3a1.65 1.65 0 0 1 3.2.55c0 1.1-1.6 1.4-1.6 2.4M8 11.25h.01" />
    </svg>
  );
}

export function LockGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <rect x="3.25" y="7" width="9.5" height="6.75" rx="1.5" />
      <path d="M5.5 7V5a2.5 2.5 0 0 1 5 0v2" />
    </svg>
  );
}

export function OfflineGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <path d="M2 2l12 12" />
      <path d="M1.75 6.25a9 9 0 0 1 3-1.9M7.5 4a9 9 0 0 1 6.75 2.25M4 8.75a5.5 5.5 0 0 1 2.4-1.3M10.6 8a5.5 5.5 0 0 1 1.4.75M6.25 11.25a2.5 2.5 0 0 1 3.5 0" />
    </svg>
  );
}

export function SearchGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <circle cx="7" cy="7" r="4.5" />
      <path d="M10.5 10.5l3 3" />
    </svg>
  );
}

export function RetryGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <path d="M13.25 8a5.25 5.25 0 1 1-1.54-3.71" />
      <path d="M13.25 2.75v3h-3" />
    </svg>
  );
}

export function PencilGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <path d="M10.5 3.5l2 2M3 13l.5-2.5 7.25-7.25a1.4 1.4 0 0 1 2 2L5.5 12.5 3 13z" />
    </svg>
  );
}

export function MenuGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <path d="M2.75 4.5h10.5M2.75 8h10.5M2.75 11.5h10.5" />
    </svg>
  );
}

export function CalendarGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <rect x="2.25" y="3.25" width="11.5" height="10.5" rx="1.75" />
      <path d="M2.25 6.75h11.5M5.5 1.75v3M10.5 1.75v3" />
    </svg>
  );
}

export function ClockGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <circle cx="8" cy="8" r="6.25" />
      <path d="M8 4.75V8l2.25 1.5" />
    </svg>
  );
}

// Wave 4 data glyphs (DataTable, FilterBar, BulkActionBar, SavedViews).
export function MoreGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <circle cx="3.5" cy="8" r="1.1" fill="currentColor" stroke="none" />
      <circle cx="8" cy="8" r="1.1" fill="currentColor" stroke="none" />
      <circle cx="12.5" cy="8" r="1.1" fill="currentColor" stroke="none" />
    </svg>
  );
}

export function ColumnsGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <rect x="2.5" y="3" width="11" height="10" rx="1.5" />
      <path d="M6.25 3v10M9.75 3v10" />
    </svg>
  );
}

export function FilterGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <path d="M2.5 4h11M4.75 8h6.5M7 12h2" />
    </svg>
  );
}

export function PlusGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <path d="M8 3.5v9M3.5 8h9" />
    </svg>
  );
}

export function StarGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <path d="M8 2.5l1.7 3.45 3.8.55-2.75 2.68.65 3.78L8 11.18l-3.4 1.78.65-3.78L2.5 6.5l3.8-.55L8 2.5z" />
    </svg>
  );
}

// Collaboration (wave 6 group A): NotificationCenter, MessageTimeline.
export function BellGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <path d="M4 6.5a4 4 0 0 1 8 0c0 3.5 1.5 4.75 1.5 4.75h-11S4 10 4 6.5z" />
      <path d="M6.75 13.5a1.4 1.4 0 0 0 2.5 0" />
    </svg>
  );
}

export function ArrowDownGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} {...props}>
      <path d="M8 3v10M4 9l4 4 4-4" />
    </svg>
  );
}

/**
 * The AI identity mark (a robot head, lucide-style Bot), used by every
 * `./ai` component. Never use a sparkle for AI (icons/no-sparkles).
 */
export function BotGlyph(props: SVGProps<SVGSVGElement>) {
  return (
    <svg {...base} strokeWidth={1.5} aria-hidden focusable={false} {...props}>
      <path d="M8 2.5V5" />
      <rect x="3" y="5" width="10" height="8" rx="2" />
      <path d="M1.5 9h1.5M13 9h1.5M6 8.25v1.25M10 8.25v1.25" />
    </svg>
  );
}
