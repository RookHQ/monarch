import { useState } from 'react';
import { IconButton } from '@rookcom/monarch-react';
import { PostCard } from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { PlusIcon } from '../../../internal/docs-demo.js';

const NOW = new Date(2026, 4, 10, 12);

function SaveDemo() {
  const [saved, setSaved] = useState(false);
  return (
    <div style={{ width: '100%', maxWidth: '40rem' }}>
      <PostCard
        title="Show us your desk setup"
        href="#post-3"
        excerpt="Post a photo of where you work and one thing you would change about it."
        author={{ name: 'Riley Chen' }}
        publishedAt={new Date(2026, 4, 7, 15)}
        now={NOW}
        tags={[{ label: 'Off topic' }]}
        reactionCount={31}
        commentCount={17}
        actions={
          <IconButton
            size="sm"
            variant="ghost"
            icon={<PlusIcon />}
            label="Save post"
            aria-pressed={saved}
            onClick={() => setSaved((s) => !s)}
          />
        }
      />
    </div>
  );
}

export default {
  id: 'post-card',
  title: 'PostCard',
  summary: 'A post in a community feed: author, time, markers, title, excerpt and counts.',
  sections: {
    purpose: {
      summary:
        'Shows one post in a feed, search result or profile list so people can decide whether to open it: who wrote it and when, its category and status, the title, a short excerpt and how many reactions and comments it has. The whole card opens the post. Needed by: Base (community feed, search and "my posts").',
      useWhen: [
        'Listing posts, questions or announcements that open into their own page.',
        'A feed where people scan many items and open a few.',
      ],
      avoidWhen: [
        'A chat message in a conversation: use MessageRow.',
        'A notification about a post: use NotificationCenter.',
        'Showing the full post with comments: build the post page instead.',
      ],
    },
    designPrinciples: {
      items: [
        'Scannable: author and time first, title in bold, excerpt muted and short.',
        'One target: the title link stretches over the card, so the card has one tab stop; extra controls sit above it.',
        'Counts are numbers on screen and full phrases for screen readers ("4 comments").',
        'Data in, no fetching: the product strips formatting and builds the link.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`renderLink(props)` renders the title link with the product’s router; spread the props on its link component.',
        '`truncateExcerpt(text, max)` is the shortening used for `excerpt`. `POST_CARD_MESSAGES` holds the English defaults.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (Base `components/community/PostCard.tsx`): `post` is split into `title`, `excerpt` (pass `stripFormatting(post.body)`), `author` (`{ name, avatarUrl }`), `publishedAt` (`post.publishedAt ?? post.createdAt`), `pinned`, `answered` (`Boolean(post.acceptedCommentId)`), `reactionCount` and `commentCount`. `slug` and `base` become `href` (`${base}/posts/${post.id}`, rendered with `renderLink` for Next.js Link). `post.category` and `post.topic` become `tags`; `showStatus` becomes a tag such as `{ label: "In review", tone: "warning" }`.',
      ],
    },
    variants: {
      items: [],
      notes: ['No visual variants; markers and counts appear when given.'],
    },
    states: {
      items: [
        {
          name: 'Pinned',
          attribute: 'data-pinned',
          description: 'A "Pinned" marker in the accent colour.',
        },
        {
          name: 'Answered',
          attribute: 'data-answered',
          description: 'A success "Answered" badge.',
        },
        { name: 'Hover', description: 'The border darkens and the title is underlined.' },
        {
          name: 'Focus',
          description: 'The whole card shows the focus ring when its link is focused.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking anywhere on the card opens the post (the title link covers it).',
        'Controls passed as `actions` stay clickable above the link.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves to the post link, then to any actions.' },
        { keys: 'Enter', action: 'Opens the post.' },
      ],
    },
    accessibility: {
      role: 'article',
      name: 'Its heading (the title, `headingLevel`, default h2)',
      items: [
        'The title is a link inside a heading, so posts can be skimmed by heading.',
        'Avatar and icons are decorative; the author’s name is written out.',
        'Counts have visually hidden labels; markers are text, not colour only.',
        'The time is a `<time datetime>` without a tooltip tab stop, to keep one stop per card.',
      ],
    },
    responsive: {
      items: [
        'The card fills its container; the meta line and footer wrap at 390px.',
        'Padding grows from 1rem to 1.25rem from 40rem wide.',
        'Long titles and words break instead of overflowing.',
      ],
    },
    theme: {
      items: [
        'Card: `--mn-color-bg-surface`, `--mn-color-border-subtle` (hover `--mn-color-border-default`), `--mn-radius-xl`.',
        'Meta and excerpt `--mn-color-text-muted`; pinned `--mn-color-accent-strong`; badges from Badge tones.',
        'In forced colours the card gets a CanvasText border and the title uses LinkText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Post',
          render: () => (
            <div style={{ width: '100%', maxWidth: '40rem' }}>
              <PostCard
                title="How do you plan your week?"
                href="#post-1"
                excerpt="I keep a short list every Monday and review it on Friday. Curious what works for others, especially with a lot of meetings."
                author={{ name: 'Sam Lee' }}
                publishedAt={new Date(2026, 4, 10, 9)}
                now={NOW}
                tags={[{ label: 'Questions' }, { label: 'Planning' }]}
                answered
                reactionCount={12}
                commentCount={8}
              />
            </div>
          ),
        },
        {
          title: 'Pinned, with a status',
          render: () => (
            <div style={{ width: '100%', maxWidth: '40rem' }}>
              <PostCard
                title="Welcome! Start here"
                href="#post-2"
                excerpt="A few house rules and where to ask for help."
                author={{ name: 'Alex Morgan' }}
                publishedAt={new Date(2026, 3, 2)}
                now={NOW}
                pinned
                tags={[{ label: 'In review', tone: 'warning' }]}
              />
            </div>
          ),
        },
        { title: 'With an action', render: () => <SaveDemo /> },
      ],
    },
    doDont: {
      do: [
        'Strip formatting from the body before passing it as `excerpt`.',
        'Use at most two tags; the category and one status.',
        'Name icon actions after what they do to this post.',
      ],
      dont: [
        'Do not put links inside the excerpt; the card is already a link.',
        'Do not show zero counts as hidden on some cards and visible on others; pass them consistently.',
        'Do not use the accent colour for statuses; use tag tones.',
      ],
    },
    related: {
      items: [
        { id: 'message-row', reason: 'Messages in a conversation.' },
        { id: 'link-card', reason: 'Previews of shared links.' },
        { id: 'badge', reason: 'The markers.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['badge', 'relative-time', 'avatar'] },
    testing: {
      unit: [
        'Excerpt shortening.',
        'Article, heading link, author, time and excerpt.',
        'Pinned, answered and tags; counts with spoken labels; heading level.',
        'Custom link renderer, custom strings and reachable actions.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
