/**
 * Decorative icons for docs-site examples only. Docs files may import this;
 * component code may not (.dependency-cruiser.cjs). Excluded from the build.
 * Monarch ships no icon set: apps pass their own (for AI features, lucide `Bot`).
 */
import type { SVGProps } from 'react';

export const PlusIcon = () => (
  <svg
    width="16"
    height="16"
    viewBox="0 0 16 16"
    fill="none"
    stroke="currentColor"
    strokeWidth="1.75"
    aria-hidden
  >
    <path d="M8 3v10M3 8h10" strokeLinecap="round" />
  </svg>
);

export const MoreIcon = () => (
  <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor" aria-hidden>
    <circle cx="3" cy="8" r="1.5" />
    <circle cx="8" cy="8" r="1.5" />
    <circle cx="13" cy="8" r="1.5" />
  </svg>
);

const stroke = {
  width: '16',
  height: '16',
  viewBox: '0 0 16 16',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: '1.75',
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  'aria-hidden': true,
} as const;

export const ListIcon = () => (
  <svg {...stroke}>
    <path d="M5.5 4h8M5.5 8h8M5.5 12h8M2.5 4h.01M2.5 8h.01M2.5 12h.01" />
  </svg>
);

export const BoardIcon = () => (
  <svg {...stroke}>
    <rect x="2" y="2.5" width="4" height="11" rx="1" />
    <rect x="10" y="2.5" width="4" height="7" rx="1" />
  </svg>
);

export const GridIcon = () => (
  <svg {...stroke}>
    <rect x="2.5" y="2.5" width="4.5" height="4.5" rx="1" />
    <rect x="9" y="2.5" width="4.5" height="4.5" rx="1" />
    <rect x="2.5" y="9" width="4.5" height="4.5" rx="1" />
    <rect x="9" y="9" width="4.5" height="4.5" rx="1" />
  </svg>
);

export const ClockIcon = () => (
  <svg {...stroke}>
    <circle cx="8" cy="8" r="6.25" />
    <path d="M8 4.75V8l2 1.5" />
  </svg>
);

export const TrashIcon = () => (
  <svg {...stroke}>
    <path d="M2.75 4.25h10.5M6.25 4.25V2.75h3.5v1.5M4.25 4.25l.6 8.5c.05.7.6 1.25 1.3 1.25h3.7c.7 0 1.25-.55 1.3-1.25l.6-8.5" />
  </svg>
);

export const PencilIcon = () => (
  <svg {...stroke}>
    <path d="M10.5 2.75l2.75 2.75L6 12.75 2.75 13.25 3.25 10z" />
  </svg>
);

export const FilterIcon = () => (
  <svg {...stroke}>
    <path d="M2.5 3.5h11M4.5 8h7M6.5 12.5h3" />
  </svg>
);

// Rook app shell examples (./shell): navigation glyphs and a generic app mark.
const shellBase = {
  width: 18,
  height: 18,
  viewBox: '0 0 16 16',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.5,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  'aria-hidden': true,
  focusable: false,
} as const;

type P = SVGProps<SVGSVGElement>;

export const HomeIcon = (p: P) => (
  <svg {...shellBase} {...p}>
    <path d="M2.5 7 8 2.5 13.5 7v6a.5.5 0 0 1-.5.5H10V10H6v3.5H3a.5.5 0 0 1-.5-.5V7Z" />
  </svg>
);
export const InboxIcon = (p: P) => (
  <svg {...shellBase} {...p}>
    <path d="M2 9.5 3.8 3.5h8.4L14 9.5V13H2V9.5Z" />
    <path d="M2 9.5h3.5l1 1.5h3l1-1.5H14" />
  </svg>
);
export const CalendarIcon = (p: P) => (
  <svg {...shellBase} {...p}>
    <rect x="2.5" y="3.5" width="11" height="10" rx="1.5" />
    <path d="M2.5 6.5h11M5.5 2v3M10.5 2v3" />
  </svg>
);
export const UsersIcon = (p: P) => (
  <svg {...shellBase} {...p}>
    <circle cx="6" cy="5.5" r="2.25" />
    <path d="M2 13c.5-2.2 2-3.5 4-3.5s3.5 1.3 4 3.5M10.5 3.5a2.25 2.25 0 0 1 0 4.2M12 9.8c1 .5 1.7 1.6 2 3.2" />
  </svg>
);
export const GearIcon = (p: P) => (
  <svg {...shellBase} {...p}>
    <circle cx="8" cy="8" r="2" />
    <path d="M8 1.8v1.6M8 12.6v1.6M1.8 8h1.6M12.6 8h1.6M3.6 3.6l1.1 1.1M11.3 11.3l1.1 1.1M3.6 12.4l1.1-1.1M11.3 4.7l1.1-1.1" />
  </svg>
);
export const CardIcon = (p: P) => (
  <svg {...shellBase} {...p}>
    <rect x="1.5" y="3.5" width="13" height="9" rx="1.5" />
    <path d="M1.5 6.5h13M4 10h2.5" />
  </svg>
);
export const SearchIcon = (p: P) => (
  <svg {...shellBase} {...p}>
    <circle cx="7" cy="7" r="4.25" />
    <path d="m10.2 10.2 3.3 3.3" />
  </svg>
);
export const BellIcon = (p: P) => (
  <svg {...shellBase} {...p}>
    <path d="M4 6.5a4 4 0 0 1 8 0c0 3.5 1.5 4.5 1.5 4.5h-11S4 10 4 6.5" />
    <path d="M6.8 13.5a1.4 1.4 0 0 0 2.4 0" />
  </svg>
);
/** A generic app mark for the brand slot. */
export const AppMark = (p: P) => (
  <svg width="28" height="28" viewBox="0 0 28 28" aria-hidden focusable={false} {...p}>
    <rect width="28" height="28" rx="7" fill="currentColor" opacity="0.18" />
    <path
      d="M8 19V9h6.5a3.5 3.5 0 0 1 0 7H11m3.5 0L19 19"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
    />
  </svg>
);
