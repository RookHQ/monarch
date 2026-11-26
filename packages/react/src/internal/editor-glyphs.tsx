/**
 * Internal glyphs for the editor toolbar and composer. Monarch ships no icon
 * set; these are drawn on the same 16px grid and stroke as the glyphs in
 * @rookcom/monarch-react.
 */
import type { ReactNode, SVGProps } from 'react';

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

type GlyphProps = SVGProps<SVGSVGElement>;
const glyph = (children: ReactNode) =>
  function Glyph(props: GlyphProps) {
    return (
      <svg {...base} {...props}>
        {children}
      </svg>
    );
  };

export const BoldGlyph = glyph(
  <path d="M4.5 3h4.25a2.5 2.5 0 0 1 0 5H4.5zM4.5 8h5a2.5 2.5 0 0 1 0 5h-5z" />,
);
export const ItalicGlyph = glyph(<path d="M10 3H6.5M9.5 13H6M9 3l-2 10" />);
export const StrikeGlyph = glyph(
  <path d="M2.5 8h11M10.75 4.5A2.75 2.25 0 0 0 8 3c-1.66 0-3 .9-3 2.25 0 .8.47 1.4 1.2 1.85M5.25 11.5A2.75 2.25 0 0 0 8 13c1.66 0 3-.9 3-2.25 0-.27-.05-.52-.15-.75" />,
);
export const CodeGlyph = glyph(<path d="M5.5 4.5 2 8l3.5 3.5M10.5 4.5 14 8l-3.5 3.5" />);
const HEADING_DIGITS: Record<number, string> = {
  1: 'M12.75 13V8.5l-1.5 1',
  2: 'M11 9.75a1.4 1.4 0 0 1 2.75.25c0 1.1-2.75 1.9-2.75 3h2.9',
  3: 'M11.1 8.75h2.6l-1.5 1.6a1.35 1.35 0 1 1-1.15 2.3',
  4: 'M13.25 13V8.5l-2.5 3h3.25',
};
/** "H" with the level drawn as a path, so the button's name is its only text. */
export function HeadingGlyph({ level, ...props }: GlyphProps & { level: number }) {
  return (
    <svg {...base} {...props}>
      <path
        d={`M2.5 3.5v9M8.5 3.5v9M2.5 8h6${HEADING_DIGITS[level] ? ` ${HEADING_DIGITS[level]}` : ''}`}
      />
    </svg>
  );
}
export const BulletListGlyph = glyph(
  <path d="M6.5 4h7M6.5 8h7M6.5 12h7M3 4h.01M3 8h.01M3 12h.01" strokeWidth={2} />,
);
export const OrderedListGlyph = glyph(
  <path d="M6.5 4h7M6.5 8h7M6.5 12h7M2.5 3.25 3.25 3v3M2.5 9.25c.2-.3.5-.5.85-.5.45 0 .8.33.8.75 0 .6-1.65 1.75-1.65 1.75h1.75" />,
);
export const TaskListGlyph = glyph(
  <path d="M2.5 3.5h3v3h-3zM3 11.25l1 1 1.75-2M8 5h5.5M8 11h5.5" />,
);
export const LinkGlyph = glyph(
  <path d="M6.75 9.25a2.75 2.75 0 0 0 3.9 0l2-2a2.76 2.76 0 0 0-3.9-3.9l-.6.6M9.25 6.75a2.75 2.75 0 0 0-3.9 0l-2 2a2.76 2.76 0 0 0 3.9 3.9l.6-.6" />,
);
export const QuoteGlyph = glyph(
  <path d="M3 4.5h3.5v3.25c0 2-1 3.25-3 3.75M9.5 4.5H13v3.25c0 2-1 3.25-3 3.75" />,
);
export const CodeBlockGlyph = glyph(
  <path d="M2.5 3h11v10h-11zM6 6.5 4.75 8 6 9.5M10 6.5 11.25 8 10 9.5" />,
);
export const PaperclipGlyph = glyph(
  <path d="m13 7.5-5.3 5.3a3.2 3.2 0 0 1-4.5-4.5l5.6-5.6a2.1 2.1 0 0 1 3 3L6.2 11.3a1.06 1.06 0 0 1-1.5-1.5l5.1-5.1" />,
);
export const SendGlyph = glyph(<path d="M2.5 8 13.5 2.5 10.5 13.5 8 9zM8 9l5.5-6.5" />);
export const CloseGlyph = glyph(<path d="M4 4l8 8M12 4l-8 8" />);
export const ReplyGlyph = glyph(<path d="M6.5 4 3 7.5 6.5 11M3 7.5h6.5A3.5 3.5 0 0 1 13 11v1" />);
export const PencilGlyph = glyph(<path d="M10.5 2.5 13.5 5.5 5.5 13.5H2.5v-3zM9 4l3 3" />);
