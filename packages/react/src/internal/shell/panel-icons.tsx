/** Internal glyphs for the shell's panels (support, chat, docs, access). Decorative. */
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

export const ChatGlyph = (p: GlyphProps) => (
  <svg {...base} {...p}>
    <path d="M2.5 4a1.5 1.5 0 0 1 1.5-1.5h8A1.5 1.5 0 0 1 13.5 4v5.5A1.5 1.5 0 0 1 12 11H7l-3 2.5V11a1.5 1.5 0 0 1-1.5-1.5Z" />
  </svg>
);
export const SendGlyph = (p: GlyphProps) => (
  <svg {...base} {...p}>
    <path d="M2.5 8 13.5 2.5 10 13.5 7.5 8.5Z" />
    <path d="M7.5 8.5 13.5 2.5" />
  </svg>
);
export const LockGlyph = (p: GlyphProps) => (
  <svg {...base} {...p}>
    <rect x="3" y="7" width="10" height="7" rx="1.5" />
    <path d="M5.5 7V5a2.5 2.5 0 0 1 5 0v2" />
  </svg>
);
export const DocGlyph = (p: GlyphProps) => (
  <svg {...base} {...p}>
    <path d="M4 1.75h5L12.25 5v8.5a.75.75 0 0 1-.75.75h-7.5a.75.75 0 0 1-.75-.75V2.5a.75.75 0 0 1 .75-.75Z" />
    <path d="M9 1.75V5h3.25M5.5 8.25h5M5.5 10.75h3.5" />
  </svg>
);
export const LinkGlyph = (p: GlyphProps) => (
  <svg {...base} {...p}>
    <path d="M6.75 9.25a2.5 2.5 0 0 0 3.5 0l2-2a2.5 2.5 0 0 0-3.5-3.5l-.75.75" />
    <path d="M9.25 6.75a2.5 2.5 0 0 0-3.5 0l-2 2a2.5 2.5 0 0 0 3.5 3.5l.75-.75" />
  </svg>
);
export const CloseGlyph = (p: GlyphProps) => (
  <svg {...base} {...p}>
    <path d="m4 4 8 8M12 4l-8 8" />
  </svg>
);
export const BookGlyph = (p: GlyphProps) => (
  <svg {...base} {...p}>
    <path d="M2.5 3h4A1.5 1.5 0 0 1 8 4.5V13a1.5 1.5 0 0 0-1.5-1.5h-4ZM13.5 3h-4A1.5 1.5 0 0 0 8 4.5V13a1.5 1.5 0 0 1 1.5-1.5h4Z" />
  </svg>
);
export const MailGlyph = (p: GlyphProps) => (
  <svg {...base} {...p}>
    <rect x="2" y="3.5" width="12" height="9" rx="1.5" />
    <path d="m2.5 4.5 5.5 4 5.5-4" />
  </svg>
);
export const PulseGlyph = (p: GlyphProps) => (
  <svg {...base} {...p}>
    <path d="M1.5 8h3l1.5-4 3 8 1.5-4h4" />
  </svg>
);
export const PhoneGlyph = (p: GlyphProps) => (
  <svg {...base} {...p}>
    <path d="M3 2.5h2.5l1 3-1.5 1a7 7 0 0 0 3.5 3.5l1-1.5 3 1V12a1.5 1.5 0 0 1-1.5 1.5A10 10 0 0 1 1.5 4 1.5 1.5 0 0 1 3 2.5Z" />
  </svg>
);
