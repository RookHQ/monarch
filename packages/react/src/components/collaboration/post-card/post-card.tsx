import {
  forwardRef,
  type AnchorHTMLAttributes,
  type HTMLAttributes,
  type ReactNode,
  type SVGProps,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { Badge, type Tone } from '../../data-display/badge/index.js';
import { RelativeTime, type DateInput } from '../../data-display/relative-time/index.js';
import { Avatar } from '../../identity/avatar/index.js';

/** The author of a post. */
export interface PostCardAuthor {
  name: string;
  avatarUrl?: string | null | undefined;
}

/** A marker after the author line, such as the category or a review status. */
export interface PostCardTag {
  label: string;
  /** Badge colour. Default `neutral`. */
  tone?: Tone | undefined;
}

/** Every string the post card shows or announces. */
export interface PostCardMessages {
  pinned: string;
  answered: string;
  /** Read after the reaction count. */
  reactions: (count: number) => string;
  /** Read after the comment count. */
  comments: (count: number) => string;
}

export const POST_CARD_MESSAGES: PostCardMessages = {
  pinned: 'Pinned',
  answered: 'Answered',
  reactions: (n) => (n === 1 ? '1 reaction' : `${n} reactions`),
  comments: (n) => (n === 1 ? '1 comment' : `${n} comments`),
};

/** Props the title link receives, for `renderLink`. */
export interface PostCardLinkProps extends AnchorHTMLAttributes<HTMLAnchorElement> {
  href: string;
  className: string;
  children: ReactNode;
}

/**
 * Shortens text to at most `max` characters at a word boundary and adds an
 * ellipsis. Text that already fits is returned unchanged.
 */
export function truncateExcerpt(text: string, max = 220): string {
  const plain = text.trim();
  if (plain.length <= max) return plain;
  const cut = plain.slice(0, max);
  const space = cut.lastIndexOf(' ');
  return `${(space > max * 0.6 ? cut.slice(0, space) : cut).trimEnd()}…`;
}

const Glyph = ({ children, ...props }: SVGProps<SVGSVGElement>) => (
  <svg
    viewBox="0 0 24 24"
    width="14"
    height="14"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    strokeLinecap="round"
    strokeLinejoin="round"
    aria-hidden
    focusable="false"
    {...props}
  >
    {children}
  </svg>
);

export interface PostCardProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** Post title; it is the link to the post. */
  title: string;
  /** Where the post opens. */
  href: string;
  /** Plain-text body; shortened to `excerptLength` characters. Strip formatting before passing it. */
  excerpt?: string | undefined;
  /** Longest excerpt shown. Default 220. */
  excerptLength?: number | undefined;
  /** Who wrote it. */
  author: PostCardAuthor;
  /** When it was published (or created, for drafts). */
  publishedAt?: DateInput | undefined;
  /** Markers after the time: category, topic, a status such as "In review". */
  tags?: readonly PostCardTag[] | undefined;
  /** Pinned to the top of its list. */
  pinned?: boolean | undefined;
  /** A question with an accepted answer. */
  answered?: boolean | undefined;
  /** Reaction count. Hidden when undefined. */
  reactionCount?: number | undefined;
  /** Comment count. Hidden when undefined. */
  commentCount?: number | undefined;
  /** Controls on the card, such as a bookmark button. They sit above the card's link. */
  actions?: ReactNode | undefined;
  /** Heading level of the title. Default 2, for a list under the page's h1. */
  headingLevel?: 2 | 3 | 4 | undefined;
  /** Renders the title link, e.g. with the router's Link. Default: a plain `<a>`. */
  renderLink?: ((props: PostCardLinkProps) => ReactNode) | undefined;
  /** "Now" for the relative time, for tests and server rendering. */
  now?: DateInput | undefined;
  /** BCP 47 locale for the time. */
  locale?: string | undefined;
  /** Override any string. */
  messages?: Partial<PostCardMessages> | undefined;
}

/**
 * A post in a community feed or search result: author, time, category and
 * status markers, title, a short excerpt, and reaction and comment counts.
 * The whole card opens the post through its title link.
 */
export const PostCard = forwardRef<HTMLElement, PostCardProps>(function PostCard(
  {
    title,
    href,
    excerpt,
    excerptLength = 220,
    author,
    publishedAt,
    tags,
    pinned = false,
    answered = false,
    reactionCount,
    commentCount,
    actions,
    headingLevel = 2,
    renderLink,
    now,
    locale,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...POST_CARD_MESSAGES, ...messagesProp };
  const Heading = `h${headingLevel}` as 'h2';
  const linkProps: PostCardLinkProps = { href, className: 'mn-post-card__link', children: title };
  const text = excerpt ? truncateExcerpt(excerpt, excerptLength) : '';

  return (
    <article
      ref={ref}
      {...rest}
      className={cx('mn-post-card', className)}
      data-pinned={pinned || undefined}
      data-answered={answered || undefined}
    >
      <div className="mn-post-card__meta">
        <Avatar name={author.name} src={author.avatarUrl ?? null} size="xs" decorative />
        <span className="mn-post-card__author">{author.name}</span>
        {publishedAt !== undefined ? (
          <>
            <span aria-hidden className="mn-post-card__dot">
              ·
            </span>
            <RelativeTime
              date={publishedAt}
              tooltip={false}
              {...(now !== undefined ? { now } : {})}
              {...(locale !== undefined ? { locale } : {})}
            />
          </>
        ) : null}
        {pinned ? (
          <span className="mn-post-card__flag" data-flag="pinned">
            <Glyph>
              <path d="M12 17v5M9 10.8V4h6v6.8l2.5 3.2h-11z" />
            </Glyph>
            {m.pinned}
          </span>
        ) : null}
        {answered ? (
          <Badge size="sm" tone="success">
            {m.answered}
          </Badge>
        ) : null}
        {tags?.map((tag) => (
          <Badge key={tag.label} size="sm" tone={tag.tone ?? 'neutral'}>
            {tag.label}
          </Badge>
        ))}
      </div>
      <Heading className="mn-post-card__title">
        {renderLink ? (
          renderLink(linkProps)
        ) : (
          <a href={href} className={linkProps.className}>
            {title}
          </a>
        )}
      </Heading>
      {text ? <p className="mn-post-card__excerpt">{text}</p> : null}
      {reactionCount !== undefined || commentCount !== undefined || actions ? (
        <div className="mn-post-card__footer">
          {reactionCount !== undefined ? (
            <span className="mn-post-card__count">
              <Glyph>
                <path d="M7 10v11M15 5.9 14 10h5.8a2 2 0 0 1 2 2.3l-1.4 7A2 2 0 0 1 18.4 21H7V10l4-8a3 3 0 0 1 4 3.9zM3 10h4v11H3z" />
              </Glyph>
              <span aria-hidden>{reactionCount}</span>
              <span className="mn-visually-hidden">{m.reactions(reactionCount)}</span>
            </span>
          ) : null}
          {commentCount !== undefined ? (
            <span className="mn-post-card__count">
              <Glyph>
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
              </Glyph>
              <span aria-hidden>{commentCount}</span>
              <span className="mn-visually-hidden">{m.comments(commentCount)}</span>
            </span>
          ) : null}
          {actions ? <div className="mn-post-card__actions">{actions}</div> : null}
        </div>
      ) : null}
    </article>
  );
});
