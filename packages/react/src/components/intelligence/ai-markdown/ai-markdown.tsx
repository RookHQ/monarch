import {
  forwardRef,
  memo,
  useId,
  useMemo,
  useRef,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { isSafeImageSrc, isSafeUrl } from '../../../internal/sanitize.js';
import { AiCodeBlock, type AiCodeBlockProps } from '../ai-code-block/index.js';
import {
  parseMarkdownBlocks,
  type MarkdownBlock,
  type MarkdownInline,
} from './ai-markdown.parser.js';

export {
  parseMarkdownBlocks,
  parseMarkdownIncremental,
  parseInline,
  inlineText,
  blockText,
  type MarkdownBlock,
  type MarkdownInline,
  type MarkdownListItem,
  type MarkdownAlign,
  type MarkdownParseState,
  type BlockTextOptions,
} from './ai-markdown.parser.js';

/** Every user-visible string AiMarkdown adds around the content. */
export interface AiMarkdownMessages {
  /** Label of each table's scroll region. Default "Table". */
  table: string;
  /** Label of the footnotes list. Default "Footnotes". */
  footnotes: string;
  /** Accessible name of a footnote reference. Default "Footnote 1". */
  footnoteReference: (n: number) => string;
  /** Accessible name of the link back from a footnote. Default "Back to reference 1". */
  footnoteBack: (n: number) => string;
  /** Accessible name of a task-list checkbox. Default "Task". */
  task: string;
  /** Added to links that open in a new tab. Default "(opens in a new tab)". */
  newTab: string;
}

export const AI_MARKDOWN_MESSAGES: AiMarkdownMessages = {
  table: 'Table',
  footnotes: 'Footnotes',
  footnoteReference: (n) => `Footnote ${n}`,
  footnoteBack: (n) => `Back to reference ${n}`,
  task: 'Task',
  newTab: '(opens in a new tab)',
};

export interface AiMarkdownProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  /** The Markdown source. Treated as untrusted: raw HTML is shown as text. */
  text?: string;
  /**
   * Blocks already parsed with `parseMarkdownBlocks` (or kept between updates
   * by a streaming renderer). When given, `text` is ignored. Blocks that are
   * the same object as on the previous render are not re-rendered.
   */
  blocks?: readonly MarkdownBlock[];
  /**
   * Added to every heading level so answers never break the page outline.
   * Default 2: `#` renders as `<h3>`, `##` as `<h4>`; levels stop at `<h6>`.
   */
  headingLevelOffset?: number;
  /**
   * Render images (safe `http(s)` sources with alt text only). Off by default:
   * an image becomes its alt text, without a request to the image URL.
   */
  allowImages?: boolean;
  /** Open links to other sites in a new tab (announced with `messages.newTab`). Default false. */
  externalLinksInNewTab?: boolean;
  /**
   * Custom renderer for fenced code, for example a lazily loaded diagram or
   * maths renderer for `mermaid` or `math`. Return `undefined` to fall back
   * to AiCodeBlock. Pass a stable function (module level or `useCallback`)
   * so completed blocks are not re-rendered while an answer streams.
   */
  renderCode?: (lang: string, code: string) => ReactNode | undefined;
  /** Props for every built-in AiCodeBlock (for example `onApply`, `lineNumbers`). Keep the object stable. */
  codeBlockProps?: Omit<AiCodeBlockProps, 'code' | 'language' | 'filename' | 'incomplete'>;
  /**
   * The text is still arriving. The last block is treated as unfinished: an
   * unclosed code block keeps its Apply and Insert actions disabled.
   */
  streaming?: boolean;
  /**
   * Inline content placed at the very end of the last paragraph, heading or
   * list item (AiStreamingText uses it for the caret). Not placed after code,
   * tables or rules.
   */
  trailing?: ReactNode;
  /** Replaces any of the English strings. Keep the object stable while streaming. */
  messages?: Partial<AiMarkdownMessages> | undefined;
}

interface Ctx {
  offset: number;
  allowImages: boolean;
  newTab: boolean;
  renderCode: AiMarkdownProps['renderCode'];
  codeBlockProps: AiMarkdownProps['codeBlockProps'];
  m: AiMarkdownMessages;
  id: string;
}

/** Footnote numbers by label, for the labels a block references. */
interface Notes {
  numbers: ReadonlyMap<string, number>;
  /** Labels whose first reference in the whole answer is in this block. */
  first: ReadonlySet<string>;
}

const EXTERNAL = /^(?:https?:)?\/\//i;

function renderInline(
  nodes: readonly MarkdownInline[],
  ctx: Ctx,
  notes: Notes,
  used: Set<string>,
  inLink = false,
): ReactNode[] {
  return nodes.map((node, key) => {
    switch (node.type) {
      case 'text':
        return node.value;
      case 'code':
        return (
          <code key={key} className="mn-ai-markdown__code">
            {node.value}
          </code>
        );
      case 'em':
        return <em key={key}>{renderInline(node.children, ctx, notes, used, inLink)}</em>;
      case 'strong':
        return <strong key={key}>{renderInline(node.children, ctx, notes, used, inLink)}</strong>;
      case 'del':
        return <del key={key}>{renderInline(node.children, ctx, notes, used, inLink)}</del>;
      case 'break':
        return <br key={key} />;
      case 'image': {
        if (ctx.allowImages && node.alt.trim() && isSafeImageSrc(node.src)) {
          return (
            <img
              key={key}
              className="mn-ai-markdown__image"
              src={node.src}
              alt={node.alt}
              title={node.title}
              loading="lazy"
              decoding="async"
              referrerPolicy="no-referrer"
            />
          );
        }
        return node.alt ? (
          <span key={key} className="mn-ai-markdown__image-alt">
            {node.alt}
          </span>
        ) : null;
      }
      case 'link': {
        const children = renderInline(node.children, ctx, notes, used, true);
        // Unsafe targets (javascript:, data:…) lose the link but keep the text.
        if (inLink || !isSafeUrl(node.href)) {
          return (
            <span key={key} className="mn-ai-markdown__unsafe-link">
              {children}
            </span>
          );
        }
        const newTab = ctx.newTab && EXTERNAL.test(node.href);
        return (
          <a
            key={key}
            className="mn-ai-markdown__link"
            href={node.href}
            title={node.title}
            rel="noopener noreferrer nofollow"
            target={newTab ? '_blank' : undefined}
          >
            {children}
            {newTab ? <span className="mn-visually-hidden"> {ctx.m.newTab}</span> : null}
          </a>
        );
      }
      case 'footnoteRef': {
        const n = notes.numbers.get(node.label);
        if (n === undefined) return `[^${node.label}]`;
        const first = notes.first.has(node.label) && !used.has(node.label);
        used.add(node.label);
        return (
          <sup key={key} className="mn-ai-markdown__footnote-ref">
            <a
              href={`#${ctx.id}-fn-${n}`}
              id={first ? `${ctx.id}-fnref-${n}` : undefined}
              aria-label={ctx.m.footnoteReference(n)}
            >
              {n}
            </a>
          </sup>
        );
      }
    }
  });
}

function headingTag(level: number, offset: number) {
  return `h${Math.min(6, Math.max(1, level + offset))}` as 'h1' | 'h2' | 'h3' | 'h4' | 'h5' | 'h6';
}

interface BlockViewProps {
  block: MarkdownBlock;
  ctx: Ctx;
  notes: Notes;
  /** Footnote signature: the only part of `notes` that affects this block. */
  notesKey: string;
  trailing: ReactNode;
  tail: boolean;
  tight?: boolean;
}

function renderBlock(
  { block, ctx, notes, trailing, tail, tight }: Omit<BlockViewProps, 'notesKey'>,
  used: Set<string>,
): ReactNode {
  const sub = (children: readonly MarkdownBlock[], childTight = false) =>
    children.map((child, i) => (
      <BlockContent
        key={i}
        block={child}
        ctx={ctx}
        notes={notes}
        used={used}
        trailing={i === children.length - 1 ? trailing : undefined}
        tail={tail && i === children.length - 1}
        tight={childTight}
      />
    ));

  switch (block.type) {
    case 'heading': {
      const Tag = headingTag(block.level, ctx.offset);
      return (
        <Tag className="mn-ai-markdown__heading" data-level={block.level}>
          {renderInline(block.content, ctx, notes, used)}
          {trailing}
        </Tag>
      );
    }
    case 'paragraph': {
      const content = (
        <>
          {renderInline(block.content, ctx, notes, used)}
          {trailing}
        </>
      );
      return tight ? content : <p className="mn-ai-markdown__p">{content}</p>;
    }
    case 'code': {
      const custom = ctx.renderCode?.(block.lang, block.code);
      if (custom !== undefined) return custom;
      return (
        <AiCodeBlock
          {...ctx.codeBlockProps}
          className={cx('mn-ai-markdown__code-block', ctx.codeBlockProps?.className)}
          code={block.code}
          language={block.lang || undefined}
          filename={block.filename}
          incomplete={tail && !block.closed}
        />
      );
    }
    case 'blockquote':
      return <blockquote className="mn-ai-markdown__quote">{sub(block.children)}</blockquote>;
    case 'list': {
      const Tag = block.ordered ? 'ol' : 'ul';
      const hasTasks = block.items.some((it) => it.checked !== null);
      return (
        <Tag
          className="mn-ai-markdown__list"
          start={block.ordered && block.start !== 1 ? block.start : undefined}
          data-tasks={hasTasks || undefined}
        >
          {block.items.map((item, i) => {
            const last = i === block.items.length - 1;
            return (
              <li
                key={i}
                className="mn-ai-markdown__item"
                data-checked={item.checked === null ? undefined : String(item.checked)}
              >
                {item.checked !== null ? (
                  <input
                    type="checkbox"
                    className="mn-ai-markdown__task"
                    checked={item.checked}
                    readOnly
                    disabled
                    aria-label={ctx.m.task}
                  />
                ) : null}
                {item.children.map((child, c) => (
                  <BlockContent
                    key={c}
                    block={child}
                    ctx={ctx}
                    notes={notes}
                    used={used}
                    trailing={last && c === item.children.length - 1 ? trailing : undefined}
                    tail={tail && last && c === item.children.length - 1}
                    tight={!block.loose}
                  />
                ))}
                {last && item.children.length === 0 ? trailing : null}
              </li>
            );
          })}
        </Tag>
      );
    }
    case 'table':
      return (
        // Focusable so keyboard users can scroll a wide table.
        // eslint-disable-next-line jsx-a11y/no-noninteractive-tabindex
        <div className="mn-ai-markdown__table" role="region" aria-label={ctx.m.table} tabIndex={0}>
          <table>
            <thead>
              <tr>
                {block.header.map((cell, c) => (
                  <th
                    key={c}
                    scope="col"
                    style={block.align[c] ? { textAlign: block.align[c] as 'left' } : undefined}
                  >
                    {renderInline(cell, ctx, notes, used)}
                  </th>
                ))}
              </tr>
            </thead>
            {block.rows.length ? (
              <tbody>
                {block.rows.map((row, r) => (
                  <tr key={r}>
                    {row.map((cell, c) => (
                      <td
                        key={c}
                        style={block.align[c] ? { textAlign: block.align[c] as 'left' } : undefined}
                      >
                        {renderInline(cell, ctx, notes, used)}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            ) : null}
          </table>
        </div>
      );
    case 'thematicBreak':
      return <hr className="mn-ai-markdown__rule" />;
    case 'footnoteDefinition':
      // Rendered in the footnotes list at the end.
      return null;
  }
}

function BlockContent(props: Omit<BlockViewProps, 'notesKey'> & { used: Set<string> }) {
  return <>{renderBlock(props, props.used)}</>;
}

/** One top-level block, re-rendered only when its token, options or footnote numbers change. */
const BlockView = memo(
  function BlockView(props: BlockViewProps) {
    return <>{renderBlock(props, new Set())}</>;
  },
  (a, b) =>
    a.block === b.block &&
    a.ctx === b.ctx &&
    a.notesKey === b.notesKey &&
    a.trailing === b.trailing &&
    a.tail === b.tail,
);

const refsCache = new WeakMap<MarkdownBlock, string[]>();

function inlineRefs(nodes: readonly MarkdownInline[], out: string[]) {
  for (const n of nodes) {
    if (n.type === 'footnoteRef') out.push(n.label);
    else if (n.type === 'em' || n.type === 'strong' || n.type === 'del' || n.type === 'link')
      inlineRefs(n.children, out);
  }
}

/** Footnote labels referenced in a block, in order (cached per block object). */
function blockRefs(block: MarkdownBlock): string[] {
  const cached = refsCache.get(block);
  if (cached) return cached;
  const out: string[] = [];
  const walk = (b: MarkdownBlock) => {
    if (b.type === 'heading' || b.type === 'paragraph') inlineRefs(b.content, out);
    else if (b.type === 'table')
      [b.header, ...b.rows].forEach((r) => r.forEach((cell) => inlineRefs(cell, out)));
    else if (b.type === 'blockquote') b.children.forEach(walk);
    else if (b.type === 'list') b.items.forEach((it) => it.children.forEach(walk));
  };
  walk(block);
  refsCache.set(block, out);
  return out;
}

/** Keeps the previous object while the new one has the same keys and values. */
function useShallowStable<T extends object | undefined>(value: T): T {
  const ref = useRef(value);
  const prev = ref.current;
  const same =
    prev === value ||
    (prev !== undefined &&
      value !== undefined &&
      Object.keys(prev).length === Object.keys(value).length &&
      Object.keys(value).every(
        (k) => (prev as Record<string, unknown>)[k] === (value as Record<string, unknown>)[k],
      ));
  if (!same) ref.current = value;
  return same ? prev : value;
}

/**
 * Renders model-written Markdown (GitHub flavour) as React elements, never as
 * an HTML string. Links must pass the safe-URL check and get
 * `rel="noopener noreferrer nofollow"`, raw HTML is shown as text, images
 * are off unless `allowImages`, headings start at `<h3>`, and fenced code
 * renders through AiCodeBlock (or your `renderCode`).
 */
export const AiMarkdown = forwardRef<HTMLDivElement, AiMarkdownProps>(function AiMarkdown(
  {
    text = '',
    blocks: blocksProp,
    headingLevelOffset = 2,
    allowImages = false,
    externalLinksInNewTab = false,
    renderCode,
    codeBlockProps,
    streaming = false,
    trailing,
    messages,
    className,
    ...rest
  },
  ref,
) {
  const parsed = useMemo(() => (blocksProp ? null : parseMarkdownBlocks(text)), [blocksProp, text]);
  const blocks = blocksProp ?? (parsed as MarkdownBlock[]);
  const id = `mn-md${useId().replace(/[^a-zA-Z0-9_-]/g, '')}`;
  const stableMessages = useShallowStable(messages);
  const stableCodeProps = useShallowStable(codeBlockProps);

  const ctx = useMemo<Ctx>(
    () => ({
      offset: headingLevelOffset,
      allowImages,
      newTab: externalLinksInNewTab,
      renderCode,
      codeBlockProps: stableCodeProps,
      m: { ...AI_MARKDOWN_MESSAGES, ...stableMessages },
      id,
    }),
    [
      headingLevelOffset,
      allowImages,
      externalLinksInNewTab,
      renderCode,
      stableCodeProps,
      stableMessages,
      id,
    ],
  );

  // Footnotes: defined labels, numbered in order of first reference.
  const definitions = new Map<string, Extract<MarkdownBlock, { type: 'footnoteDefinition' }>>();
  for (const b of blocks)
    if (b.type === 'footnoteDefinition' && !definitions.has(b.label)) definitions.set(b.label, b);
  const numbers = new Map<string, number>();
  const firstIn: Set<string>[] = blocks.map(() => new Set());
  blocks.forEach((b, i) => {
    for (const label of blockRefs(b)) {
      if (!definitions.has(label) || numbers.has(label)) continue;
      numbers.set(label, numbers.size + 1);
      (firstIn[i] as Set<string>).add(label);
    }
  });
  const ordered = [...numbers.entries()].map(([label, n]) => ({
    n,
    def: definitions.get(label) as Extract<MarkdownBlock, { type: 'footnoteDefinition' }>,
  }));

  const lastIndex = blocks.length - 1;
  const lastIsNote = blocks[lastIndex]?.type === 'footnoteDefinition';

  return (
    <div ref={ref} {...rest} className={cx('mn-ai-markdown', className)}>
      {blocks.map((block, i) => {
        const refs = blockRefs(block);
        const first = firstIn[i] as Set<string>;
        const notesKey = refs.length
          ? refs.map((l) => `${l}=${numbers.get(l) ?? ''}${first.has(l) ? '*' : ''}`).join(' ')
          : '';
        return (
          <BlockView
            key={i}
            block={block}
            ctx={ctx}
            notes={{ numbers, first }}
            notesKey={notesKey}
            trailing={i === lastIndex ? trailing : undefined}
            tail={streaming && i === lastIndex}
          />
        );
      })}
      {lastIsNote ? trailing : null}
      {ordered.length ? (
        <div className="mn-ai-markdown__footnotes">
          <ol aria-label={ctx.m.footnotes}>
            {ordered.map(({ n, def }) => {
              const back = (
                <>
                  {' '}
                  <a
                    href={`#${id}-fnref-${n}`}
                    className="mn-ai-markdown__footnote-back"
                    aria-label={ctx.m.footnoteBack(n)}
                  >
                    <span aria-hidden="true">↩</span>
                  </a>
                </>
              );
              const used = new Set<string>();
              const notes = { numbers, first: new Set<string>() };
              return (
                <li key={def.label} id={`${id}-fn-${n}`}>
                  {def.children.map((child, c) => (
                    <BlockContent
                      key={c}
                      block={child}
                      ctx={ctx}
                      notes={notes}
                      used={used}
                      trailing={c === def.children.length - 1 ? back : undefined}
                      tail={false}
                    />
                  ))}
                  {def.children.length === 0 ? back : null}
                </li>
              );
            })}
          </ol>
        </div>
      ) : null}
    </div>
  );
});
