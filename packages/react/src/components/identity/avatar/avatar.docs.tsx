import { Avatar, AvatarGroup } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'avatar',
  title: 'Avatar',
  summary:
    'Person or entity image with an initials and icon fallback, presence status and stacked groups.',
  sections: {
    purpose: {
      summary:
        'A person or entity image, falling back to initials on a colour stable per seed, then to an icon. AvatarGroup stacks several with a “+N” overflow.',
      useWhen: [
        'Identifying a person, workspace, organisation or brand next to their content.',
        'Showing several assignees or participants in little space (AvatarGroup).',
      ],
      avoidWhen: [
        'A decorative icon: use the icon directly.',
        'A selectable list of people: use a picker (assignee-picker, entity-picker).',
      ],
    },
    designPrinciples: {
      items: [
        'The same name always gets the same fallback colour, everywhere.',
        'Fallback colours keep AA contrast for every hue in light and dark themes (tested).',
        'Set `decorative` when the name is already visible next to the avatar.',
        'Circles are people; squares are workspaces, organisations and brands.',
        'Presence is never colour alone: the status is part of the accessible name.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`initials(name)` is exported: first and last word initials, upper-cased, "?" for an empty name.',
        '`avatarPaletteIndex(seed)` returns the data token (1–5) a seed maps to with `palette="tokens"`, for matching colours elsewhere.',
        'Avatar and AvatarGroup forward their refs to the root `<span>` and `<div>`.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'size',
          values: ['xs', 'sm', 'md', 'lg', 'xl'],
          description:
            '1.25–3.5rem. Default `md` (2rem). Exposed as `data-size`. A number is a size in CSS pixels (`data-size="custom"`, `--mn-avatar-size`).',
        },
        {
          prop: 'shape',
          values: ['circle', 'square'],
          description:
            'Default `circle`; `square` for organisations and brands. Exposed as `data-shape`.',
        },
        {
          prop: 'palette',
          values: ['hue', 'tokens'],
          description:
            '`hue` (default) derives a soft colour from the seed; `tokens` uses one of the five `--mn-color-data-*` tokens (`data-palette`).',
        },
        {
          prop: 'status',
          values: ['online', 'away', 'busy', 'offline'],
          description: 'Presence dot at the bottom end edge (`data-status`).',
        },
        {
          prop: 'fallback',
          values: ['initials', 'icon'],
          description: 'What shows without an image. `icon` suits organisations without a logo.',
        },
      ],
    },
    states: {
      items: [
        { name: 'Image', description: 'Shows `src` when given and it loads.' },
        {
          name: 'Initials fallback',
          description:
            'Shows initials when there is no `src` or the image fails to load. A new `src` is tried again.',
        },
        {
          name: 'Icon fallback',
          description:
            'Shows `icon` (default a person glyph) with `fallback="icon"` or when the name has no letters or digits.',
        },
        {
          name: 'Status',
          attribute: 'data-status',
          description: 'Online, away, busy (filled dot) or offline (ring).',
        },
        {
          name: 'Overflow',
          description:
            'AvatarGroup shows `max` avatars and a “+N” tile; it never shows “+1”, it shows that avatar instead.',
        },
      ],
    },
    interaction: {
      items: [
        'None. If the image errors, the avatar switches to initials (or the icon).',
        'AvatarGroup applies its `size` and `shape` to children that do not set their own.',
      ],
    },
    keyboard: { items: [], notes: ['Not interactive, so there is no keyboard behaviour.'] },
    accessibility: {
      role: 'img (or aria-hidden when decorative); AvatarGroup is a labelled group',
      name: '`name` becomes `aria-label`, plus the status: “Ada Lovelace (Online)”. AvatarGroup requires `label`.',
      items: [
        'The inner image has empty alt and the initials are `aria-hidden`, so the name is announced once.',
        '`decorative` hides the avatar, its status included, when the name is printed beside it.',
        'The “+N” tile is an image named by `overflowLabel` (default “N more”).',
        'Avatars are not focusable; when an avatar alone identifies a person (board cards), keep it labelled (not `decorative`) and, for sighted users, add a Tooltip on the focusable card or link around it.',
      ],
    },
    responsive: {
      items: [
        'Fixed size per `size`; no breakpoints. AvatarGroup overlaps by a quarter of the avatar size and does not wrap.',
      ],
    },
    theme: {
      items: [
        'Fallback colours are derived from a per-seed hue (`--mn-avatar-hue`) with separate light and dark lightness values.',
        '`palette="tokens"` tints the surface with a data token and mixes it into the default text colour (AA in light and dark, tested); in more-contrast mode the text is the default text colour.',
        'Status dots use the success, warning and danger solid tokens with a surface-coloured ring.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Sizes and fallback',
          render: () => (
            <>
              <Avatar name="Ada Lovelace" size="xs" />
              <Avatar name="Grace Hopper" size="sm" />
              <Avatar name="Katherine Johnson" />
              <Avatar name="Margaret Hamilton" size="lg" shape="square" />
              <Avatar name="Rook" size="xl" />
            </>
          ),
          code: `<Avatar name="Ada Lovelace" src={user.avatarUrl} />
<Avatar name="Margaret Hamilton" size="lg" shape="square" />`,
        },
        {
          title: 'Status, numeric size and token palette',
          render: () => (
            <>
              <Avatar name="Ada Lovelace" status="online" />
              <Avatar name="Grace Hopper" status="away" size={28} />
              <Avatar name="Katherine Johnson" status="busy" palette="tokens" />
              <Avatar name="Margaret Hamilton" status="offline" palette="tokens" seed="user_4" />
              <Avatar name="Northwind Labs" shape="square" fallback="icon" size="lg" />
            </>
          ),
          code: `<Avatar name={user.name} src={user.avatarUrl} status="online" />
<Avatar name={user.name} size={28} status="away" />
<Avatar name={user.name} seed={user.id} palette="tokens" status="busy" />
<Avatar name={org.name} src={org.logoUrl} shape="square" fallback="icon" />`,
        },
        {
          title: 'Group with overflow',
          render: () => (
            <AvatarGroup label="Assignees" max={3}>
              <Avatar name="Ada Lovelace" />
              <Avatar name="Grace Hopper" />
              <Avatar name="Katherine Johnson" />
              <Avatar name="Margaret Hamilton" />
              <Avatar name="Hedy Lamarr" />
            </AvatarGroup>
          ),
          code: `<AvatarGroup label="Assignees" max={3}>
  {assignees.map((a) => <Avatar key={a.id} name={a.name} src={a.avatarUrl} seed={a.id} />)}
</AvatarGroup>`,
        },
      ],
    },
    doDont: {
      do: [
        'Always pass the real `name`; it drives the label and initials.',
        'Pass a stable `seed` (a user id) so the colour survives a rename.',
        'Translate `statusLabel` and `overflowLabel` with the rest of the interface.',
      ],
      dont: [
        'Do not leave `decorative` off when the name is printed beside the avatar.',
        'Do not use status colours for anything other than presence.',
      ],
    },
    related: {
      items: [
        { id: 'member-row', reason: 'Planned row with avatar, name and role.' },
        { id: 'assignee-picker', reason: 'Planned picker showing avatars.' },
        { id: 'tooltip', reason: 'Names an avatar shown without its name.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Computes initials.',
        'Falls back to initials when the image fails.',
        'Fallback colours keep AA contrast for every hue.',
        'Decorative avatars are aria-hidden.',
        'Default markup is unchanged from 0.3.',
        'Numeric size, seed colour, token palette mapping and its AA contrast.',
        'Fallback chain image → initials → icon, and retry on a new `src`.',
        'Status is added to the accessible name, with a custom label.',
        'AvatarGroup: labelled group, `max` with “+N”, no “+1”, size inheritance, custom overflow label, no tab stop.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery'],
    },
  },
} satisfies ComponentDocs;
