'use client';

import { forwardRef, useState, type FormEvent, type HTMLAttributes, type ReactNode } from 'react';
import { Node, mergeAttributes } from '@tiptap/core';
import { NodeViewWrapper, ReactNodeViewRenderer, type NodeViewProps } from '@tiptap/react';
import { Button } from '../../actions/button/index.js';
import { Field, FieldError, FieldLabel } from '../../forms/form-field/index.js';
import { Input } from '../../forms/text-field/index.js';
import { cx } from '../../../internal/cx.js';
import { isSafeUrl } from '../../../internal/sanitize.js';

/* -------------------------------------------------------------- providers */

/**
 * A site whose pages may be shown in a frame. The URL someone pastes is
 * checked against `hosts` (exact hostnames, https only) and turned into the
 * provider's own embed address by `toEmbedSrc`; anything else is never framed.
 */
export interface EmbedProvider {
  /** Stable id, e.g. `youtube`. */
  id: string;
  /** Shown on the frame and link card, e.g. `YouTube`. */
  name: string;
  /** Exact hostnames of share URLs, e.g. `['youtube.com', 'www.youtube.com', 'youtu.be']`. */
  hosts: readonly string[];
  /** The embed address for a share URL, or null when the URL has no embeddable form. Must return an https URL. */
  toEmbedSrc: (url: URL) => string | null;
  /**
   * `sandbox` tokens for the frame. Default
   * `allow-scripts allow-same-origin allow-presentation allow-popups`.
   */
  sandbox?: string | undefined;
  /** Permissions Policy for the frame (`allow`). Default none. */
  allow?: string | undefined;
  /** CSS aspect ratio of the frame. Default `16 / 9`. */
  aspectRatio?: string | undefined;
}

/** The default sandbox. The framed page is on the provider's origin, never the app's, so `allow-same-origin` only lets it keep its own cookies; it can never reach the app. */
export const EMBED_DEFAULT_SANDBOX =
  'allow-scripts allow-same-origin allow-presentation allow-popups';

const segments = (url: URL) => url.pathname.split('/').filter(Boolean);
const YOUTUBE_ID = /^[\w-]{11}$/;

/** The providers framed out of the box. Pass your own list to `providers` to add or remove sites. */
export const DEFAULT_EMBED_PROVIDERS: readonly EmbedProvider[] = [
  {
    id: 'youtube',
    name: 'YouTube',
    hosts: ['youtube.com', 'www.youtube.com', 'm.youtube.com', 'youtu.be'],
    allow: 'encrypted-media; picture-in-picture; fullscreen',
    toEmbedSrc: (url) => {
      const path = segments(url);
      const id =
        url.hostname === 'youtu.be'
          ? path[0]
          : path[0] === 'watch'
            ? url.searchParams.get('v')
            : path[0] === 'embed' || path[0] === 'shorts' || path[0] === 'live'
              ? path[1]
              : null;
      return id && YOUTUBE_ID.test(id) ? `https://www.youtube-nocookie.com/embed/${id}` : null;
    },
  },
  {
    id: 'vimeo',
    name: 'Vimeo',
    hosts: ['vimeo.com', 'www.vimeo.com', 'player.vimeo.com'],
    allow: 'fullscreen; picture-in-picture',
    toEmbedSrc: (url) => {
      const id = segments(url).find((s) => /^\d+$/.test(s));
      return id ? `https://player.vimeo.com/video/${id}` : null;
    },
  },
  {
    id: 'loom',
    name: 'Loom',
    hosts: ['loom.com', 'www.loom.com'],
    allow: 'fullscreen',
    toEmbedSrc: (url) => {
      const [kind, id] = segments(url);
      return (kind === 'share' || kind === 'embed') && id && /^[a-f0-9]{16,64}$/i.test(id)
        ? `https://www.loom.com/embed/${id}`
        : null;
    },
  },
  {
    id: 'figma',
    name: 'Figma',
    hosts: ['figma.com', 'www.figma.com'],
    aspectRatio: '4 / 3',
    toEmbedSrc: (url) =>
      ['file', 'design', 'proto', 'board', 'slides'].includes(segments(url)[0] ?? '')
        ? `https://www.figma.com/embed?embed_host=monarch&url=${encodeURIComponent(url.href)}`
        : null,
  },
  {
    id: 'codepen',
    name: 'CodePen',
    hosts: ['codepen.io'],
    aspectRatio: '4 / 3',
    sandbox: 'allow-scripts allow-same-origin allow-popups allow-forms',
    toEmbedSrc: (url) => {
      const [user, kind, id] = segments(url);
      return user &&
        (kind === 'pen' || kind === 'embed') &&
        id &&
        /^\w+$/.test(id) &&
        /^[\w-]+$/.test(user)
        ? `https://codepen.io/${user}/embed/${id}?default-tab=result`
        : null;
    },
  },
];

/** What `resolveEmbed` decided for a URL. */
export type EmbedResolution =
  | { kind: 'frame'; url: string; src: string; provider: EmbedProvider }
  | { kind: 'link'; url: string; host: string }
  | { kind: 'invalid'; url: string };

/**
 * Decides how a pasted URL is shown: a sandboxed frame when it is an https
 * URL on an allow-listed host with an embeddable form, a link card for any
 * other http(s) URL, and nothing for anything else (`javascript:`, `data:`,
 * relative or malformed URLs).
 */
export function resolveEmbed(
  raw: string,
  providers: readonly EmbedProvider[] = DEFAULT_EMBED_PROVIDERS,
): EmbedResolution {
  const value = raw.trim();
  let url: URL;
  try {
    url = new URL(value);
  } catch {
    return { kind: 'invalid', url: value };
  }
  if (
    (url.protocol !== 'https:' && url.protocol !== 'http:') ||
    !isSafeUrl(url.href, ['http', 'https'])
  )
    return { kind: 'invalid', url: value };
  if (url.username || url.password) return { kind: 'invalid', url: value };
  const host = url.hostname.toLowerCase();
  if (url.protocol === 'https:' && !url.port) {
    for (const provider of providers) {
      if (!provider.hosts.includes(host)) continue;
      const src = provider.toEmbedSrc(url);
      if (src && src.startsWith('https://')) return { kind: 'frame', url: url.href, src, provider };
    }
  }
  return { kind: 'link', url: url.href, host: host.replace(/^www\./, '') };
}

/* ------------------------------------------------------------- component */

/** Strings the embed shows or announces. */
export interface EmbedBlockMessages {
  /** Frame title when there is no caption. Default "YouTube content". */
  frameTitle: (provider: string) => string;
  /** Shown for a URL that cannot be shown. */
  invalid: string;
  /** Link card hint. Default "Opens in a new tab". */
  opensInNewTab: string;
  /** Editor: URL field label. */
  urlLabel: string;
  /** Editor: URL field placeholder. */
  urlPlaceholder: string;
  /** Editor: submit button. */
  submit: string;
  /** Editor: error for an unusable URL. */
  urlInvalid: string;
  /** Editor: caption field label. */
  captionLabel: string;
  /** Editor: caption placeholder. */
  captionPlaceholder: string;
}

export const DEFAULT_EMBED_BLOCK_MESSAGES: EmbedBlockMessages = {
  frameTitle: (provider) => `${provider} content`,
  invalid: 'This link can’t be shown.',
  opensInNewTab: 'Opens in a new tab',
  urlLabel: 'Link to embed',
  urlPlaceholder: 'https://',
  submit: 'Embed',
  urlInvalid: 'Enter a web address that starts with https://.',
  captionLabel: 'Caption',
  captionPlaceholder: 'Add a caption',
};

export interface EmbedBlockProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** The URL someone pasted (a share link, not an embed code). */
  url: string;
  /** Shown under the frame, and used as the frame's title. */
  caption?: ReactNode;
  /** Accessible title of the frame when `caption` is not plain text. */
  title?: string | undefined;
  /** Sites that may be framed. Default `DEFAULT_EMBED_PROVIDERS`. */
  providers?: readonly EmbedProvider[] | undefined;
  /** Replaces the caption row (the editor uses it for the caption field). */
  footer?: ReactNode;
  messages?: Partial<EmbedBlockMessages> | undefined;
}

/**
 * Shows a video, design or demo from an allow-listed site in a sandboxed
 * frame, and any other web link as a link card. Only providers' own embed
 * addresses are ever framed; the pasted URL itself never is.
 */
export const EmbedBlock = forwardRef<HTMLElement, EmbedBlockProps>(function EmbedBlock(
  { url, caption, title, providers, footer, messages, className, ...rest },
  ref,
) {
  const m = { ...DEFAULT_EMBED_BLOCK_MESSAGES, ...messages };
  const r = resolveEmbed(url, providers);
  const captionText = typeof caption === 'string' ? caption : undefined;
  return (
    <figure ref={ref} {...rest} className={cx('mn-embed-block', className)} data-kind={r.kind}>
      {r.kind === 'frame' ? (
        <div
          className="mn-embed-block__frame"
          data-provider={r.provider.id}
          style={{ aspectRatio: r.provider.aspectRatio ?? '16 / 9' }}
        >
          <iframe
            src={r.src}
            title={title ?? captionText ?? m.frameTitle(r.provider.name)}
            sandbox={r.provider.sandbox ?? EMBED_DEFAULT_SANDBOX}
            // Fullscreen goes in `allow` when the provider sets one: with both,
            // browsers ignore `allowfullscreen` and warn that `allow` wins.
            {...(r.provider.allow
              ? {
                  allow: /\bfullscreen\b/.test(r.provider.allow)
                    ? r.provider.allow
                    : `${r.provider.allow}; fullscreen`,
                }
              : { allowFullScreen: true })}
            referrerPolicy="strict-origin-when-cross-origin"
            loading="lazy"
          />
        </div>
      ) : r.kind === 'link' ? (
        <a
          className="mn-embed-block__card"
          href={r.url}
          target="_blank"
          rel="noopener noreferrer nofollow"
        >
          <span className="mn-embed-block__host">{r.host}</span>
          <span className="mn-embed-block__url">{r.url}</span>
          <span className="mn-visually-hidden"> ({m.opensInNewTab})</span>
        </a>
      ) : (
        <p className="mn-embed-block__invalid">{m.invalid}</p>
      )}
      {footer ??
        (caption ? <figcaption className="mn-embed-block__caption">{caption}</figcaption> : null)}
    </figure>
  );
});

/* ------------------------------------------------------------- extension */

/** Options for `EmbedBlockExtension.configure()`. */
export interface EmbedBlockExtensionOptions {
  /** Sites that may be framed. Default `DEFAULT_EMBED_PROVIDERS`. */
  providers: readonly EmbedProvider[];
  messages: Partial<EmbedBlockMessages> | undefined;
  HTMLAttributes: Record<string, unknown>;
}

declare module '@tiptap/core' {
  interface Commands<ReturnType> {
    embed: {
      /** Inserts an embed; without a URL it shows a field to paste one. */
      insertEmbed: (attrs?: { url?: string; caption?: string }) => ReturnType;
    };
  }
}

const safeAttr = (v: unknown) =>
  typeof v === 'string' && v && resolveEmbed(v).kind !== 'invalid' ? v.trim() : '';

function UrlForm({
  m,
  providers,
  onSubmit,
}: {
  m: EmbedBlockMessages;
  providers: readonly EmbedProvider[];
  onSubmit: (url: string) => void;
}) {
  const [value, setValue] = useState('');
  const [invalid, setInvalid] = useState(false);
  const submit = (e: FormEvent) => {
    e.preventDefault();
    e.stopPropagation();
    const r = resolveEmbed(value, providers);
    if (r.kind === 'invalid') {
      setInvalid(true);
      return;
    }
    onSubmit(r.url);
  };
  return (
    <form className="mn-embed-block__form" onSubmit={submit} noValidate>
      <Field invalid={invalid} className="mn-embed-block__field">
        <FieldLabel>{m.urlLabel}</FieldLabel>
        <Input
          type="url"
          inputMode="url"
          autoComplete="off"
          value={value}
          placeholder={m.urlPlaceholder}
          onChange={(e) => {
            setValue(e.target.value);
            setInvalid(false);
          }}
        />
        <FieldError>{invalid ? m.urlInvalid : null}</FieldError>
      </Field>
      <Button type="submit" variant="secondary">
        {m.submit}
      </Button>
    </form>
  );
}

function EmbedNodeView({ node, editor, extension, updateAttributes, selected }: NodeViewProps) {
  const options = extension.options as EmbedBlockExtensionOptions;
  const m = { ...DEFAULT_EMBED_BLOCK_MESSAGES, ...options.messages };
  const url = typeof node.attrs.url === 'string' ? node.attrs.url : '';
  const caption = typeof node.attrs.caption === 'string' ? node.attrs.caption : '';
  const editable = editor.isEditable;
  return (
    <NodeViewWrapper
      className="mn-embed-block__node"
      data-type="embed"
      data-selected={selected || undefined}
      contentEditable={false}
    >
      {!url ? (
        editable ? (
          <div className="mn-embed-block mn-embed-block--empty">
            <UrlForm
              m={m}
              providers={options.providers}
              onSubmit={(u) => updateAttributes({ url: u })}
            />
          </div>
        ) : null
      ) : (
        <EmbedBlock
          url={url}
          caption={caption || undefined}
          providers={options.providers}
          messages={options.messages}
          footer={
            editable ? (
              <Input
                className="mn-embed-block__caption-input"
                aria-label={m.captionLabel}
                placeholder={m.captionPlaceholder}
                value={caption}
                onChange={(e) => updateAttributes({ caption: e.target.value })}
              />
            ) : undefined
          }
        />
      )}
    </NodeViewWrapper>
  );
}

/**
 * The Tiptap node for EmbedBlock: an atom `embed` with `url` and `caption`.
 * A new embed shows a labelled field to paste a link. HTML output is a plain
 * link (`<div data-type="embed" data-url="…"><a href="…">…</a></div>`), never
 * a frame, so stored HTML cannot carry an iframe; the frame is drawn from the
 * allow-list when the document is shown. Adds `insertEmbed`.
 */
export const EmbedBlockExtension = Node.create<EmbedBlockExtensionOptions>({
  name: 'embed',
  group: 'block',
  atom: true,
  draggable: true,
  selectable: true,
  addOptions() {
    return { providers: DEFAULT_EMBED_PROVIDERS, messages: undefined, HTMLAttributes: {} };
  },
  addAttributes() {
    return {
      url: {
        default: '',
        parseHTML: (el: HTMLElement) => safeAttr(el.getAttribute('data-url')),
        renderHTML: (attrs: { url?: unknown }) => ({ 'data-url': safeAttr(attrs.url) }),
      },
      caption: {
        default: '',
        parseHTML: (el: HTMLElement) => el.getAttribute('data-caption') ?? '',
        renderHTML: (attrs: { caption?: unknown }) =>
          typeof attrs.caption === 'string' && attrs.caption
            ? { 'data-caption': attrs.caption }
            : {},
      },
    };
  },
  parseHTML() {
    return [{ tag: 'div[data-type="embed"]' }];
  },
  renderHTML({ HTMLAttributes, node }) {
    const url = safeAttr(node.attrs.url);
    const caption = typeof node.attrs.caption === 'string' ? node.attrs.caption : '';
    const attrs = mergeAttributes(this.options.HTMLAttributes, HTMLAttributes, {
      'data-type': 'embed',
    });
    if (!url) return ['div', attrs];
    return [
      'div',
      attrs,
      ['a', { href: url, rel: 'noopener noreferrer nofollow', target: '_blank' }, caption || url],
    ];
  },
  addCommands() {
    return {
      insertEmbed:
        (attrs) =>
        ({ commands }) =>
          commands.insertContent({
            type: this.name,
            attrs: { url: safeAttr(attrs?.url), caption: attrs?.caption ?? '' },
          }),
    };
  },
  addNodeView() {
    return ReactNodeViewRenderer(EmbedNodeView);
  },
});
