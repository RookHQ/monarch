import { Button, IconButton } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';
import { MoreIcon, PlusIcon } from '../../../internal/docs-demo.js';

export default {
  id: 'button',
  title: 'Button',
  summary: 'Primary, secondary, ghost and danger actions; loading and icon-only forms.',
  sections: {
    purpose: {
      summary:
        'Triggers an action in place: saving, submitting, opening a dialog or menu. IconButton is the square, icon-only form.',
      useWhen: [
        'The user performs an action on the current view (save, delete, open a menu).',
        'A link must look like a button, for example a primary "New ticket" call to action (use `asChild`).',
      ],
      avoidWhen: [
        'The control navigates as ordinary text: use Link.',
        'Status is only being shown, not acted on: use Badge. `success` and `warning` buttons are for actions whose outcome is that status ("Approve", "Mark at risk").',
        'The choice is on/off: use Switch or Checkbox.',
      ],
    },
    designPrinciples: {
      items: [
        'One `primary` button per view region; everything else is `secondary` or `ghost`.',
        '`danger` is for the final, destructive confirmation; `danger-subtle` for destructive actions that still need confirming.',
        'Labels are verbs that say what happens ("Save changes", not "OK").',
        'Never submits by accident: the default `type` is `button`.',
        '`success` and `warning` are status actions, used sparingly next to a neutral alternative.',
        '`brand` is for a tenant or customer colour (a hosted booking page, a public form). Contrast is guaranteed by the tokens engine, never by the caller.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Both components forward their ref to the underlying `<button>` (or the child element with `asChild`).',
        'All native button attributes pass through.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [
        {
          prop: 'variant',
          values: [
            'primary',
            'secondary',
            'ghost',
            'danger',
            'danger-subtle',
            'success',
            'warning',
            'brand',
          ],
          description:
            'Visual weight. Default `secondary`; IconButton defaults to `ghost`. Exposed as `data-variant`. `success` and `warning` use the status tokens; `brand` uses `brandColor`.',
        },
        {
          prop: 'brandColor',
          values: ['#rrggbb', '#rgb'],
          description:
            'The tenant colour for `variant="brand"`. Fill, hover, active and text are derived for light, dark and both more-contrast themes with `deriveIntent` from @rookcom/monarch-tokens, so text meets 4.5:1 (7:1 in more contrast). Sets `data-brand` and inline `--_brand-*` properties. Missing or invalid: the product accent is used.',
        },
        {
          prop: 'size',
          values: ['xs', 'sm', 'md', 'lg'],
          description:
            'Control height from the `--mn-control-*` tokens. Default `md`. Exposed as `data-size`.',
        },
        {
          prop: 'block',
          values: ['true', 'false'],
          description: 'Stretches to the container width (`data-block`).',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Hover and active',
          description: 'Background steps through the variant’s hover and active tokens.',
        },
        {
          name: 'Focus',
          description: 'Visible focus ring from `--mn-color-focus-ring` on `:focus-visible` only.',
        },
        {
          name: 'Disabled',
          attribute: 'disabled, aria-disabled="true"',
          description:
            'Native `disabled` plus `aria-disabled`; 50% opacity and a not-allowed cursor.',
        },
        {
          name: 'Loading',
          attribute: 'data-loading, aria-busy="true", aria-disabled="true"',
          description:
            'Replaces the leading icon with a small Spinner, blocks clicks, and stays focusable so keyboard users keep their place.',
        },
      ],
    },
    interaction: {
      items: [
        'Click, Enter and Space call `onClick` (native button behaviour).',
        'While `disabled` or `loading`, clicks are prevented and `onClick` is not called.',
        'With `asChild`, the child element (for example a router link) receives the classes, data attributes and handlers; no `type` attribute is added.',
        '`leadingIcon` and `trailingIcon` render in decorative spans.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves focus to the button, including while it is loading.' },
        { keys: 'Enter / Space', action: 'Activates the button.' },
      ],
    },
    accessibility: {
      role: 'button (or the child element’s role with asChild)',
      name: 'The visible label; IconButton requires `label`, which becomes `aria-label`.',
      items: [
        'Icons are wrapped in `aria-hidden` spans, so they never add to the accessible name.',
        'Loading sets `aria-busy="true"` and `aria-disabled="true"` instead of `disabled`, so focus is not lost.',
        'IconButton cannot be rendered without a `label`: the type requires it.',
        '`brand` text contrast is computed, not assumed: a light brand colour gets dark text, a mid colour is darkened until white text passes.',
      ],
    },
    responsive: {
      items: [
        'Heights come from the `--mn-control-*` tokens and do not change with viewport width.',
        '`block` makes the button fill narrow containers such as mobile dialog footers.',
        'Labels do not wrap (`white-space: nowrap`).',
      ],
    },
    theme: {
      items: [
        'Colours are Monarch semantic tokens only, so light, dark and more-contrast themes apply automatically.',
        '`primary` uses the product accent (`--mn-color-accent-solid`), so each Rook product gets its own accent with no code change.',
        '`danger` variants use the danger tokens, which do not change with the product accent; `success` and `warning` likewise use their status tokens.',
        '`brand` carries one derived colour set per theme and picks the one matching `.dark` / `data-mn-theme` / `data-mn-contrast` on an ancestor, so a tenant colour also adapts to dark and more-contrast themes.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Variants',
          render: () => (
            <>
              <Button variant="primary">Save changes</Button>
              <Button>Cancel</Button>
              <Button variant="ghost">Learn more</Button>
              <Button variant="danger">Delete</Button>
              <Button variant="danger-subtle">Remove</Button>
            </>
          ),
          code: `<Button variant="primary">Save changes</Button>
<Button>Cancel</Button>
<Button variant="ghost">Learn more</Button>
<Button variant="danger">Delete</Button>
<Button variant="danger-subtle">Remove</Button>`,
        },
        {
          title: 'Sizes, icons, loading',
          render: () => (
            <>
              <Button size="xs">Extra small</Button>
              <Button size="sm">Small</Button>
              <Button size="md" leadingIcon={<PlusIcon />}>
                New ticket
              </Button>
              <Button size="lg" variant="primary" loading>
                Saving
              </Button>
              <Button disabled>Disabled</Button>
              <IconButton label="More actions" icon={<MoreIcon />} />
            </>
          ),
          code: `<Button size="xs">Extra small</Button>
<Button size="sm">Small</Button>
<Button leadingIcon={<PlusIcon />}>New ticket</Button>
<Button size="lg" variant="primary" loading>Saving</Button>
<Button disabled>Disabled</Button>
<IconButton label="More actions" icon={<MoreIcon />} />`,
        },
        {
          title: 'As a link',
          render: () => (
            <Button asChild variant="primary">
              <a href="#/components/button">Open ticket</a>
            </Button>
          ),
          code: `<Button asChild variant="primary">
  <Link href="/tickets/42">Open ticket</Link>
</Button>`,
        },
        {
          title: 'Status and brand',
          render: () => (
            <>
              <Button variant="success">Approve</Button>
              <Button variant="warning">Mark at risk</Button>
              <Button variant="brand" brandColor="#0f766e">
                Book a call
              </Button>
              <Button variant="brand" brandColor="#facc15">
                Join waitlist
              </Button>
              <Button variant="success" size="xs">
                Approve
              </Button>
            </>
          ),
          code: `<Button variant="success">Approve</Button>
<Button variant="warning">Mark at risk</Button>
{/* tenant colour from your data; text contrast is derived */}
<Button variant="brand" brandColor={tenant.color}>Book a call</Button>
<Button variant="brand" brandColor="#facc15">Join waitlist</Button>
<Button variant="success" size="xs">Approve</Button>`,
        },
      ],
    },
    doDont: {
      do: [
        'Use one primary button per region, placed where the eye ends (the right of a dialog footer).',
        'Keep the button focusable while work is in progress: use `loading`, not `disabled`.',
        'Use `asChild` to style a router link as a button instead of nesting a button in a link.',
      ],
      dont: [
        'Do not put several primary buttons side by side.',
        'Do not use an IconButton for an action whose icon is ambiguous; give it a visible label instead.',
        'Do not use `danger` for actions that are reversible.',
        'Do not pass a brand colour through `style` or `className`: use `variant="brand"` + `brandColor` so contrast is checked.',
      ],
    },
    related: {
      items: [
        { id: 'link', reason: 'Navigation that reads as text.' },
        { id: 'dropdown-menu', reason: 'Several related actions behind one button.' },
        { id: 'spinner', reason: 'Loading indicator the button uses internally.' },
        { id: 'toolbar', reason: 'Groups buttons with arrow-key navigation.' },
      ],
    },
    dependencies: {
      packages: ['@radix-ui/react-slot', '@rookcom/monarch-tokens', 'react'],
      components: ['spinner'],
    },
    testing: {
      unit: [
        'Defaults to `type="button"`.',
        'Activates with click, Enter and Space.',
        'Stays focusable but inert while loading (aria-busy, aria-disabled, no click).',
        'Disables natively when `disabled`.',
        'Renders the child element with `asChild`, without a `type` attribute.',
        'Keeps the child element and spinner with `asChild` + `loading`, and both icons with `asChild`.',
        'Hides decorative icons from assistive technology.',
        'IconButton uses `label` as its accessible name and shows only the spinner while loading.',
        '`success` and `warning` expose their variant; `xs` its size.',
        '`brand` derives a fill and text for all four themes that meet 4.5:1 (7:1 in more contrast) for light, mid and dark brand colours, accepts `#rgb`, keeps caller styles, and falls back to the accent for an invalid colour.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery', 'Cross-browser axe: /components/button'],
    },
  },
} satisfies ComponentDocs;
