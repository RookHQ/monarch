import { forwardRef, type ButtonHTMLAttributes, type ReactNode, type SVGProps } from 'react';
import { cx } from '../../../internal/cx.js';
import { ChevronRightGlyph } from '../../../internal/icons.js';
import type { AiArtifactPart } from '../ai-conversation/index.js';

/** What an artifact is: a document, code, a table, a chart, an image or an HTML page. */
export type AiArtifactKind = AiArtifactPart['kind'];

/** Every string AiArtifactCard shows or reads. English defaults. */
export interface AiArtifactCardMessages {
  /** Visible name of each kind, e.g. "Document". */
  kinds: Record<AiArtifactKind, string>;
  /** Visible version, e.g. "v3". */
  version: (version: number) => string;
  /** Version as read by assistive technology, e.g. "version 3". */
  versionLong: (version: number) => string;
  /** Visible call to action. */
  open: string;
  /** Shown instead of `open` while the artifact is already open in the canvas. */
  opened: string;
}

export const AI_ARTIFACT_CARD_MESSAGES: AiArtifactCardMessages = {
  kinds: {
    document: 'Document',
    code: 'Code',
    table: 'Table',
    chart: 'Chart',
    image: 'Image',
    html: 'Web page',
  },
  version: (version) => `v${version}`,
  versionLong: (version) => `version ${version}`,
  open: 'Open',
  opened: 'Showing',
};

type GlyphProps = SVGProps<SVGSVGElement>;
const glyph = (children: ReactNode) =>
  function Glyph(props: GlyphProps) {
    return (
      <svg
        viewBox="0 0 24 24"
        width="1em"
        height="1em"
        fill="none"
        stroke="currentColor"
        strokeWidth={2}
        strokeLinecap="round"
        strokeLinejoin="round"
        aria-hidden
        focusable="false"
        {...props}
      >
        {children}
      </svg>
    );
  };

/** Lucide-style glyphs per kind (file-text, code, table, bar-chart, image, globe). */
const KIND_GLYPHS: Record<AiArtifactKind, (props: GlyphProps) => ReactNode> = {
  document: glyph(
    <>
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
      <path d="M14 2v6h6M16 13H8M16 17H8M10 9H8" />
    </>,
  ),
  code: glyph(<path d="m16 18 6-6-6-6M8 6l-6 6 6 6" />),
  table: glyph(
    <>
      <rect x="3" y="3" width="18" height="18" rx="2" />
      <path d="M3 9h18M3 15h18M12 3v18" />
    </>,
  ),
  chart: glyph(<path d="M3 3v18h18M18 17V9M13 17V5M8 17v-3" />),
  image: glyph(
    <>
      <rect x="3" y="3" width="18" height="18" rx="2" />
      <circle cx="9" cy="9" r="2" />
      <path d="m21 15-3.1-3.1a2 2 0 0 0-2.8 0L6 21" />
    </>,
  ),
  html: glyph(
    <>
      <circle cx="12" cy="12" r="10" />
      <path d="M2 12h20M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z" />
    </>,
  ),
};

/** The decorative glyph for an artifact kind, for apps that show kinds elsewhere. */
export function AiArtifactKindGlyph({ kind, ...props }: GlyphProps & { kind: AiArtifactKind }) {
  const Glyph = KIND_GLYPHS[kind] ?? KIND_GLYPHS.document;
  return <>{Glyph(props)}</>;
}

export interface AiArtifactCardProps extends Omit<
  ButtonHTMLAttributes<HTMLButtonElement>,
  'children' | 'onClick' | 'type' | 'part'
> {
  /** The artifact part from the message: its title, kind and version. */
  part: AiArtifactPart;
  /** Called with the part when the card is activated. Open it in an `AiArtifactCanvas`. */
  onOpen: (part: AiArtifactPart) => void;
  /** The artifact is the one currently shown in the canvas. Sets `aria-current` and `data-active`. */
  active?: boolean | undefined;
  /** Secondary text under the title, e.g. "Updated just now". */
  description?: ReactNode | undefined;
  /** Strings, for localisation. `kinds` merges per kind. */
  messages?:
    | (Partial<Omit<AiArtifactCardMessages, 'kinds'>> & {
        kinds?: Partial<Record<AiArtifactKind, string>>;
      })
    | undefined;
}

/**
 * The compact card a reply shows for an artifact it produced ("Q3 summary ·
 * Document · v3"). One button that opens the artifact in an
 * `AiArtifactCanvas`; its name reads the title, kind, version and action.
 */
export const AiArtifactCard = forwardRef<HTMLButtonElement, AiArtifactCardProps>(
  function AiArtifactCard(
    { part, onOpen, active = false, description, messages: messagesProp, className, ...rest },
    ref,
  ) {
    const m = {
      ...AI_ARTIFACT_CARD_MESSAGES,
      ...messagesProp,
      kinds: { ...AI_ARTIFACT_CARD_MESSAGES.kinds, ...messagesProp?.kinds },
    };
    const kind = m.kinds[part.kind] ?? part.kind;
    const action = active ? m.opened : m.open;
    return (
      <button
        ref={ref}
        type="button"
        aria-current={active ? 'true' : undefined}
        {...rest}
        className={cx('mn-ai-artifact-card', className)}
        data-kind={part.kind}
        data-active={active || undefined}
        onClick={() => onOpen(part)}
      >
        <span className="mn-ai-artifact-card__icon" aria-hidden>
          <AiArtifactKindGlyph kind={part.kind} />
        </span>
        <span className="mn-ai-artifact-card__text">
          <span className="mn-ai-artifact-card__title">{part.title}</span>
          <span className="mn-ai-artifact-card__meta">
            {kind}
            <span aria-hidden> · {m.version(part.version)}</span>
            <span className="mn-visually-hidden">, {m.versionLong(part.version)}</span>
          </span>
          {description ? (
            <span className="mn-ai-artifact-card__description">{description}</span>
          ) : null}
        </span>
        <span className="mn-ai-artifact-card__action">
          <span className="mn-visually-hidden">, </span>
          {action}
          <ChevronRightGlyph className="mn-ai-artifact-card__chevron" aria-hidden />
        </span>
      </button>
    );
  },
);
