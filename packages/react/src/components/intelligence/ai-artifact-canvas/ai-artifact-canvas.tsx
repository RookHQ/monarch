'use client';

import {
  forwardRef,
  useEffect,
  useId,
  useRef,
  useState,
  type ForwardedRef,
  type HTMLAttributes,
  type KeyboardEvent as ReactKeyboardEvent,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { ownsOpenPopup } from '../../../internal/dismiss.js';
import { CloseGlyph } from '../../../internal/icons.js';
import { isSafeImageSrc } from '../../../internal/sanitize.js';
import { Button, IconButton } from '../../actions/button/index.js';
import { CopyButton } from '../../actions/copy-button/index.js';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../../navigation/tabs/index.js';
import { Sheet, SheetContent, SheetTitle } from '../../overlays/sheet/index.js';
import { AiArtifactKindGlyph, type AiArtifactKind } from '../ai-artifact-card/index.js';
import { AiArtifactVersions, type AiArtifactVersionInfo } from '../ai-artifact-versions/index.js';
import { AiCodeBlock } from '../ai-code-block/index.js';
import { AiMarkdown } from '../ai-markdown/index.js';

/** The artifact the canvas shows: one version of it, with its full content. */
export interface AiCanvasArtifact {
  id: string;
  kind: AiArtifactKind;
  title: string;
  /** The version shown, counting from 1. */
  version: number;
  /**
   * The content as text: Markdown for documents and tables, source for code,
   * an HTML document for `html`, an image URL for `image`. Untrusted model
   * output: it is never injected into the page.
   */
  content: string;
  /** Language id for the Code tab ("ts", "python"…). Defaults by kind. */
  language?: string | undefined;
}

/** The two views of the canvas. */
export type AiArtifactCanvasTab = 'preview' | 'code';

/** Every string AiArtifactCanvas shows or reads. English defaults. */
export interface AiArtifactCanvasMessages {
  /** Accessible name of the canvas region, e.g. "Artifact: Q3 summary". */
  label: (title: string) => string;
  /** Kind names under the title. */
  kinds: Record<AiArtifactKind, string>;
  /** "v3" under the title. */
  version: (version: number) => string;
  preview: string;
  code: string;
  /** Accessible name of the tab list. */
  views: string;
  close: string;
  copy: string;
  copied: string;
  download: string;
  /** Label of the save action, e.g. "Save to library". */
  save: string;
  /** Title of the sandboxed HTML preview frame. */
  frameTitle: (title: string) => string;
  /** Shown under the HTML preview: scripts, forms and network are off. */
  sandboxNote: string;
  /** Shown when an image artifact's URL is not allowed. */
  unsafeImage: string;
  /** Shown in the Preview tab when the kind has no built-in preview and `renderPreview` returns nothing. */
  noPreview: string;
}

export const AI_ARTIFACT_CANVAS_MESSAGES: AiArtifactCanvasMessages = {
  label: (title) => `Artifact: ${title}`,
  kinds: {
    document: 'Document',
    code: 'Code',
    table: 'Table',
    chart: 'Chart',
    image: 'Image',
    html: 'Web page',
  },
  version: (version) => `v${version}`,
  preview: 'Preview',
  code: 'Code',
  views: 'View',
  close: 'Close artifact',
  copy: 'Copy',
  copied: 'Copied',
  download: 'Download',
  save: 'Save',
  frameTitle: (title) => `Preview of ${title}`,
  sandboxNote: 'Sandboxed preview: scripts, forms and network requests are blocked.',
  unsafeImage: 'This image address is not allowed, so it is not shown.',
  noPreview: 'No preview for this kind of artifact. See the Code tab.',
};

const DEFAULT_LANGUAGE: Record<AiArtifactKind, string | undefined> = {
  document: 'markdown',
  table: 'markdown',
  code: undefined,
  chart: 'json',
  image: undefined,
  html: 'html',
};

/**
 * Wraps untrusted HTML for a `srcdoc` frame. The frame's empty `sandbox`
 * already blocks scripts, forms, popups, plugins and same-origin access; the
 * Content Security Policy also blocks every network request (images, fonts,
 * styles, frames) so a preview cannot load or beacon to remote addresses.
 * Only inline styles and `data:` images remain. `<meta http-equiv="refresh">`
 * tags are removed so the preview cannot navigate itself to a remote page.
 */
export function aiArtifactSrcDoc(html: string): string {
  const safe = html.replace(/<meta\b[^>]*http-equiv\s*=\s*["']?\s*refresh[^>]*>/gi, '');
  return (
    '<!doctype html><html><head><meta charset="utf-8">' +
    `<meta http-equiv="Content-Security-Policy" content="default-src 'none'; img-src data:; style-src 'unsafe-inline'; font-src data:; form-action 'none'; base-uri 'none'">` +
    '</head><body>' +
    safe +
    '</body></html>'
  );
}

/** True when the kind has a built-in Preview tab. */
function hasBuiltInPreview(kind: AiArtifactKind) {
  return kind === 'document' || kind === 'table' || kind === 'html' || kind === 'image';
}

export interface AiArtifactCanvasProps extends Omit<
  HTMLAttributes<HTMLElement>,
  'title' | 'onCopy'
> {
  /** The artifact version to show. */
  artifact: AiCanvasArtifact;
  /** Every version, for the version stepper. Omit or pass one to hide it. */
  versions?: readonly AiArtifactVersionInfo[] | undefined;
  /** Called with the version chosen in the stepper; load it and pass it as `artifact`. */
  onVersionChange?: ((version: number) => void) | undefined;
  /** Shows "Compare with previous" in the stepper (see AiArtifactVersions). */
  onCompare?: ((from: number, to: number) => void) | undefined;
  /** Whether the canvas is shown. */
  open: boolean;
  /** Called with `false` by the close button and by Escape. */
  onOpenChange: (open: boolean) => void;
  /** Called with the artifact after the Copy button copies its content. */
  onCopy?: ((artifact: AiCanvasArtifact) => void) | undefined;
  /** Shows a Download button. The app produces the file. */
  onDownload?: ((artifact: AiCanvasArtifact) => void) | undefined;
  /**
   * Shows a primary save action, labelled by `saveLabel` (e.g. "Save to
   * library"). It may return a promise; the button shows progress until it settles.
   */
  onSave?: ((artifact: AiCanvasArtifact) => unknown) | undefined;
  /** Label of the save action. Default "Save". */
  saveLabel?: string | undefined;
  /**
   * Replaces the built-in preview (for charts, tables as grids, or a richer
   * document view). Return `undefined` to fall back to the built-in preview.
   * Whatever it renders is the app's responsibility: never inject the content as HTML.
   */
  renderPreview?: ((artifact: AiCanvasArtifact) => ReactNode | undefined) | undefined;
  /** The open tab (controlled). Default: Preview when there is one, otherwise Code. */
  tab?: AiArtifactCanvasTab | undefined;
  /** Called when the tab changes. */
  onTabChange?: ((tab: AiArtifactCanvasTab) => void) | undefined;
  /**
   * `auto` (default): a pane in the layout, and a full-screen sheet below
   * 640px. `inline`: always a pane. `sheet`: always a sheet.
   */
  placement?: 'auto' | 'inline' | 'sheet' | undefined;
  /** Allows images in Markdown previews. Default false: model output stays text and links. */
  allowImages?: boolean | undefined;
  /** Strings, for localisation. `kinds` merges per kind. */
  messages?:
    | (Partial<Omit<AiArtifactCanvasMessages, 'kinds'>> & {
        kinds?: Partial<Record<AiArtifactKind, string>>;
      })
    | undefined;
}

const COMPACT_QUERY = '(max-width: 639.98px)';

/** True below 640px, read after mount (false on the server). */
function useCompact(enabled: boolean): boolean {
  const [compact, setCompact] = useState(false);
  useEffect(() => {
    if (!enabled || typeof window === 'undefined' || !window.matchMedia) {
      setCompact(false);
      return;
    }
    const mq = window.matchMedia(COMPACT_QUERY);
    const update = () => setCompact(mq.matches);
    update();
    mq.addEventListener?.('change', update);
    return () => mq.removeEventListener?.('change', update);
  }, [enabled]);
  return compact;
}

function setRef<T>(ref: ForwardedRef<T>, value: T | null) {
  if (typeof ref === 'function') ref(value);
  else if (ref) ref.current = value;
}

/**
 * The working surface for an artifact a reply produced: a pane beside the
 * thread (a full-screen sheet on phones) with the title, version stepper,
 * Copy / Download / Save actions and Preview / Code tabs. Documents preview
 * through AiMarkdown, code through AiCodeBlock, and HTML only inside a
 * sandboxed `srcdoc` frame (`sandbox=""`, no network) so model output is
 * never injected into the page. Escape and the close button close it, and
 * focus returns to where it was.
 */
export const AiArtifactCanvas = forwardRef<HTMLElement, AiArtifactCanvasProps>(
  function AiArtifactCanvas(
    {
      artifact,
      versions,
      onVersionChange,
      onCompare,
      open,
      onOpenChange,
      onCopy,
      onDownload,
      onSave,
      saveLabel,
      renderPreview,
      tab: tabProp,
      onTabChange,
      placement = 'auto',
      allowImages = false,
      messages: messagesProp,
      className,
      onKeyDown,
      id: idProp,
      ...rest
    },
    ref,
  ) {
    const m = {
      ...AI_ARTIFACT_CANVAS_MESSAGES,
      ...messagesProp,
      kinds: { ...AI_ARTIFACT_CANVAS_MESSAGES.kinds, ...messagesProp?.kinds },
    };
    const autoId = useId();
    const id = idProp ?? `mn-ai-artifact-canvas-${autoId.replace(/:/g, '')}`;
    const titleId = `${id}-title`;
    const rootRef = useRef<HTMLElement | null>(null);
    const returnTo = useRef<HTMLElement | null>(null);

    const compact = useCompact(placement === 'auto');
    const asSheet = placement === 'sheet' || (placement === 'auto' && compact);

    // ------------------------------------------------------------ preview
    const custom = renderPreview?.(artifact);
    const previewAvailable = custom !== undefined || hasBuiltInPreview(artifact.kind);
    const [tabInner, setTabInner] = useState<AiArtifactCanvasTab>('preview');
    const requested = tabProp ?? tabInner;
    const tab: AiArtifactCanvasTab = previewAvailable ? requested : 'code';
    const setTab = (next: AiArtifactCanvasTab) => {
      if (tabProp === undefined) setTabInner(next);
      onTabChange?.(next);
    };

    // ------------------------------------------------------------ save
    const [saving, setSaving] = useState(false);
    const mounted = useRef(true);
    useEffect(() => {
      mounted.current = true;
      return () => {
        mounted.current = false;
      };
    }, []);
    const save = async () => {
      if (!onSave) return;
      setSaving(true);
      try {
        await onSave(artifact);
      } finally {
        if (mounted.current) setSaving(false);
      }
    };

    // ------------------------------------------------------------ focus (inline pane)
    const prevOpen = useRef(open);
    useEffect(() => {
      if (asSheet) return;
      const was = prevOpen.current;
      prevOpen.current = open;
      if (open && !was) {
        const active = document.activeElement;
        returnTo.current =
          active instanceof HTMLElement && active !== document.body ? active : null;
        rootRef.current?.focus({ preventScroll: true });
      } else if (!open && was) {
        const target = returnTo.current;
        returnTo.current = null;
        const active = document.activeElement;
        const lost = !active || active === document.body;
        if (lost && target && target.isConnected) target.focus();
      }
    }, [open, asSheet]);

    const close = () => {
      // Move focus before the pane unmounts so it is never lost to <body>.
      const root = rootRef.current;
      const target = returnTo.current;
      if (root && root.contains(document.activeElement) && target && target.isConnected) {
        returnTo.current = null;
        target.focus();
      }
      onOpenChange(false);
    };

    const handleKeyDown = (e: ReactKeyboardEvent<HTMLElement>) => {
      onKeyDown?.(e);
      if (e.defaultPrevented || e.key !== 'Escape') return;
      const root = rootRef.current;
      if (!root || !(e.target instanceof Node) || !root.contains(e.target)) return;
      if (ownsOpenPopup(e.target)) return;
      e.preventDefault();
      close();
    };

    // ------------------------------------------------------------ parts
    const language = artifact.language ?? DEFAULT_LANGUAGE[artifact.kind];
    const kindLabel = m.kinds[artifact.kind] ?? artifact.kind;

    let preview: ReactNode;
    if (custom !== undefined) {
      preview = custom;
    } else if (artifact.kind === 'html') {
      preview = (
        <>
          <iframe
            className="mn-ai-artifact-canvas__frame"
            title={m.frameTitle(artifact.title)}
            // Empty sandbox: no scripts, forms, popups, top navigation or same-origin access.
            sandbox=""
            srcDoc={aiArtifactSrcDoc(artifact.content)}
            referrerPolicy="no-referrer"
            loading="lazy"
          />
          <p className="mn-ai-artifact-canvas__note">{m.sandboxNote}</p>
        </>
      );
    } else if (artifact.kind === 'image') {
      const src = artifact.content.trim();
      preview = isSafeImageSrc(src) ? (
        <img
          className="mn-ai-artifact-canvas__image"
          src={src}
          alt={artifact.title}
          referrerPolicy="no-referrer"
        />
      ) : (
        <p className="mn-ai-artifact-canvas__note" data-tone="warning">
          {m.unsafeImage}
        </p>
      );
    } else if (artifact.kind === 'document' || artifact.kind === 'table') {
      preview = <AiMarkdown text={artifact.content} allowImages={allowImages} />;
    } else {
      preview = <p className="mn-ai-artifact-canvas__note">{m.noPreview}</p>;
    }

    const header = (
      <div className="mn-ai-artifact-canvas__header">
        <span className="mn-ai-artifact-canvas__icon" aria-hidden>
          <AiArtifactKindGlyph kind={artifact.kind} />
        </span>
        <div className="mn-ai-artifact-canvas__heading">
          {asSheet ? (
            <SheetTitle id={titleId} className="mn-ai-artifact-canvas__title">
              {artifact.title}
            </SheetTitle>
          ) : (
            <h2 id={titleId} className="mn-ai-artifact-canvas__title">
              {artifact.title}
            </h2>
          )}
          <p className="mn-ai-artifact-canvas__meta">
            {kindLabel} · {m.version(artifact.version)}
          </p>
        </div>
        <IconButton
          size="sm"
          className="mn-ai-artifact-canvas__close"
          label={m.close}
          icon={<CloseGlyph />}
          onClick={close}
        />
      </div>
    );

    const toolbar = (
      <div className="mn-ai-artifact-canvas__toolbar">
        {versions && versions.length > 1 && onVersionChange ? (
          <AiArtifactVersions
            className="mn-ai-artifact-canvas__versions"
            versions={versions}
            value={artifact.version}
            onChange={onVersionChange}
            onCompare={onCompare}
          />
        ) : null}
        <div className="mn-ai-artifact-canvas__actions">
          <CopyButton
            size="sm"
            variant="ghost"
            value={() => artifact.content}
            label={m.copy}
            copiedLabel={m.copied}
            target={artifact.title}
            onCopy={() => onCopy?.(artifact)}
          />
          {onDownload ? (
            <Button size="sm" variant="ghost" onClick={() => onDownload(artifact)}>
              {m.download}
            </Button>
          ) : null}
          {onSave ? (
            <Button size="sm" variant="primary" loading={saving} onClick={() => void save()}>
              {saveLabel ?? m.save}
            </Button>
          ) : null}
        </div>
      </div>
    );

    const body = (
      <Tabs
        className="mn-ai-artifact-canvas__tabs"
        value={tab}
        onValueChange={(v) => setTab(v as AiArtifactCanvasTab)}
      >
        <TabsList aria-label={m.views} variant="pill">
          {previewAvailable ? <TabsTrigger value="preview">{m.preview}</TabsTrigger> : null}
          <TabsTrigger value="code">{m.code}</TabsTrigger>
        </TabsList>
        {previewAvailable ? (
          <TabsContent value="preview" className="mn-ai-artifact-canvas__panel">
            {preview}
          </TabsContent>
        ) : null}
        <TabsContent value="code" className="mn-ai-artifact-canvas__panel">
          <AiCodeBlock code={artifact.content} language={language} lineNumbers />
        </TabsContent>
      </Tabs>
    );

    const content = (
      <>
        {header}
        {toolbar}
        {body}
      </>
    );

    if (asSheet) {
      return (
        <Sheet open={open} onOpenChange={(next) => (next ? onOpenChange(true) : close())}>
          <SheetContent
            ref={(node: HTMLDivElement | null) => {
              rootRef.current = node;
              setRef(ref, node);
            }}
            side="right"
            size="full"
            closeLabel={null}
            aria-labelledby={titleId}
            aria-describedby={undefined}
            {...(rest as HTMLAttributes<HTMLDivElement>)}
            id={id}
            className={cx('mn-ai-artifact-canvas', className)}
            data-placement="sheet"
            data-kind={artifact.kind}
            onKeyDown={onKeyDown}
          >
            {content}
          </SheetContent>
        </Sheet>
      );
    }

    if (!open) return null;
    return (
      // Escape closes the pane from anywhere inside it.
      // eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions
      <section
        ref={(node) => {
          rootRef.current = node;
          setRef(ref, node);
        }}
        aria-label={m.label(artifact.title)}
        tabIndex={-1}
        {...rest}
        id={id}
        className={cx('mn-ai-artifact-canvas', className)}
        data-placement="inline"
        data-kind={artifact.kind}
        onKeyDown={handleKeyDown}
      >
        {content}
      </section>
    );
  },
);
