/** Internal glyphs for the shell chrome. Monarch ships no icon set; apps pass their own icons. */
import type { SVGProps } from 'react';

const base = {
  width: 16,
  height: 16,
  viewBox: '0 0 16 16',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.6,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  'aria-hidden': true,
  focusable: false,
} as const;

type GlyphProps = SVGProps<SVGSVGElement>;

export const MenuGlyph = (p: GlyphProps) => (
  <svg {...base} {...p}>
    <path d="M2.5 4h11M2.5 8h11M2.5 12h11" />
  </svg>
);
export const PanelOpenGlyph = (p: GlyphProps) => (
  <svg {...base} {...p}>
    <rect x="2" y="2.5" width="12" height="11" rx="2" />
    <path d="M6 2.5v11M8.5 6.5 10 8l-1.5 1.5" />
  </svg>
);
export const PanelCloseGlyph = (p: GlyphProps) => (
  <svg {...base} {...p}>
    <rect x="2" y="2.5" width="12" height="11" rx="2" />
    <path d="M6 2.5v11M10 6.5 8.5 8 10 9.5" />
  </svg>
);
export const HelpGlyph = (p: GlyphProps) => (
  <svg {...base} {...p}>
    <circle cx="8" cy="8" r="6" />
    <circle cx="8" cy="8" r="2.5" />
    <path d="m3.8 3.8 2.4 2.4M9.8 9.8l2.4 2.4M12.2 3.8 9.8 6.2M6.2 9.8l-2.4 2.4" />
  </svg>
);
export const BellGlyph = (p: GlyphProps) => (
  <svg {...base} {...p}>
    <path d="M4 6.5a4 4 0 0 1 8 0c0 3.5 1.5 4.5 1.5 4.5h-11S4 10 4 6.5" />
    <path d="M6.8 13.5a1.4 1.4 0 0 0 2.4 0" />
  </svg>
);
export const ExternalGlyph = (p: GlyphProps) => (
  <svg {...base} {...p}>
    <path d="M5.5 10.5 10.5 5.5M6.5 5.5h4v4" />
  </svg>
);
export const SunGlyph = (p: GlyphProps) => (
  <svg {...base} {...p}>
    <circle cx="8" cy="8" r="2.75" />
    <path d="M8 1.5v1.5M8 13v1.5M1.5 8H3M13 8h1.5M3.4 3.4l1 1M11.6 11.6l1 1M3.4 12.6l1-1M11.6 4.4l1-1" />
  </svg>
);
export const MoonGlyph = (p: GlyphProps) => (
  <svg {...base} {...p}>
    <path d="M13 9.5A5.5 5.5 0 0 1 6.5 3a5.5 5.5 0 1 0 6.5 6.5Z" />
  </svg>
);
export const SystemGlyph = (p: GlyphProps) => (
  <svg {...base} {...p}>
    <rect x="2" y="3" width="12" height="8" rx="1.5" />
    <path d="M6 13.5h4M8 11v2.5" />
  </svg>
);
export const LayersGlyph = (p: GlyphProps) => (
  <svg {...base} {...p}>
    <path d="m8 2 6 3.3L8 8.6 2 5.3 8 2Z" />
    <path d="m2 8.4 6 3.3 6-3.3" />
  </svg>
);
export const ChevronsGlyph = (p: GlyphProps) => (
  <svg {...base} {...p}>
    <path d="m5 6 3-3 3 3M5 10l3 3 3-3" />
  </svg>
);
export const PlusGlyph = (p: GlyphProps) => (
  <svg {...base} {...p}>
    <path d="M8 3v10M3 8h10" />
  </svg>
);
export const SettingsGlyph = (p: GlyphProps) => (
  <svg {...base} {...p}>
    <circle cx="8" cy="8" r="2" />
    <path d="M8 1.8v1.6M8 12.6v1.6M1.8 8h1.6M12.6 8h1.6M3.6 3.6l1.1 1.1M11.3 11.3l1.1 1.1M3.6 12.4l1.1-1.1M11.3 4.7l1.1-1.1" />
  </svg>
);
export const SignOutGlyph = (p: GlyphProps) => (
  <svg {...base} {...p}>
    <path d="M6 13.5H3.5a1 1 0 0 1-1-1v-9a1 1 0 0 1 1-1H6M10.5 11 13.5 8l-3-3M13.5 8H6" />
  </svg>
);
