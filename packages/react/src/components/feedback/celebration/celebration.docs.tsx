import { Button, Celebration, CelebrationPanel } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

export default {
  id: 'celebration',
  title: 'Celebration',
  summary:
    'A short success moment for a real milestone: dialog or inline panel, with one-shot, reduced-motion-safe confetti.',
  sections: {
    purpose: {
      summary:
        'Marks a milestone people worked for (an account or domain verified, a first project published) with a clear message and the next step. The decoration is CSS only, never flashes and stays still under reduced motion. Needed by: Links (verification celebration).',
      useWhen: [
        'A verification, approval or setup the person waited for has just completed.',
        'The end of an onboarding flow, inline on the last step (`CelebrationPanel`).',
      ],
      avoidWhen: [
        'Routine saves and edits: use a Toast.',
        'Every time something succeeds: celebrations lose meaning when repeated.',
        'Errors or partial success: use Alert or InlineNotice.',
      ],
    },
    designPrinciples: {
      items: [
        'Rare and earned: one celebration per milestone, never for ordinary actions.',
        'Decoration is decoration: confetti and the badge are hidden from assistive technology; the title and description carry the message.',
        'Safe motion: the confetti falls once, changes only position and opacity, and nothing flashes. Under reduced motion it rests as a still scatter.',
        'Always a way forward: one primary "Continue" (or a named next step).',
        'No timers: it stays until the person moves on, so nobody misses it (WCAG 2.2.1).',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`Celebration` is a Dialog: controlled (`open` + `onOpenChange`) or uncontrolled (`trigger`, `defaultOpen`). Its ref goes to the dialog element; other DialogContent props pass through.',
        '`CelebrationPanel` is the same content inline, in a `div` with `role="status"` (turn off with `announce={false}`). Its ref goes to that div.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`media` accepts any node: an `<img alt="">` badge, an icon, or nothing for the default check mark. It is decorative.',
        '`continueLabel={null}` removes the continue button; then give `actions` or set `closeLabel` so there is a way out besides Escape.',
        'Migrating from the app copy (Links `components/VerificationCelebration`): render `<Celebration open={…} onOpenChange={…} title="Verified" description={…} media={<img src="/verified-badge.png" alt="" />} />`. `onClose` → `onOpenChange(false)`. The 4-second auto-close is gone on purpose; if a product must close it, it controls `open` itself.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'decoration',
          values: ['confetti', 'none'],
          description:
            'One-shot falling confetti behind the icon, or none. Exposed as `data-decoration`.',
        },
        {
          prop: 'tone',
          values: ['success', 'accent'],
          description: 'Colour of the icon tile. Default `success`. Exposed as `data-tone`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Open',
          attribute: 'data-state="open"',
          description: 'Dialog only: shown, with focus on the continue button.',
        },
        {
          name: 'Closed',
          attribute: 'data-state="closed"',
          description: 'Dialog only: removed; focus returns to the trigger.',
        },
      ],
      notes: ['The panel has no states of its own.'],
    },
    interaction: {
      items: [
        'The continue button runs `onContinue` and closes the dialog.',
        'Escape and an outside click close the dialog without `onContinue`.',
        'The confetti plays once when it appears and does not repeat.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Enter / Space', action: 'Activates the focused button (Continue by default).' },
        {
          keys: 'Tab / Shift+Tab',
          action: 'Moves between the actions; focus stays inside the dialog.',
        },
        { keys: 'Escape', action: 'Closes the dialog.' },
      ],
      notes: ['The panel has no keyboard behaviour beyond the actions you pass.'],
    },
    accessibility: {
      role: 'dialog (Celebration) or status (CelebrationPanel)',
      name: 'The title; the description is the accessible description of the dialog.',
      items: [
        'Confetti and media are `aria-hidden`; nothing is conveyed by them alone.',
        'No flashing content (WCAG 2.3.1): opacity fades once, no luminance flicker, no repetition.',
        'prefers-reduced-motion: the icon does not scale and the pieces do not move (WCAG 2.3.3).',
        'No time limit: the dialog stays until dismissed (WCAG 2.2.1).',
        'Focus moves to the continue button on open and returns to the trigger on close.',
        'Buttons are at least 44px tall on coarse pointers.',
      ],
    },
    responsive: {
      items: [
        'The dialog is 24rem wide, capped at the viewport minus 2rem, so it fits 390px.',
        'Actions wrap and share the width on narrow screens.',
      ],
    },
    theme: {
      items: [
        'Confetti uses the data palette tokens (`--mn-color-data-1` to `-5`), so it follows each theme.',
        'The icon tile uses the success (or accent) subtle and strong tokens.',
        'Forced colours: confetti is hidden and the icon tile gets a CanvasText border.',
        'Reduced motion: static icon and a still scatter of pieces.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Verification dialog',
          render: () => (
            <Celebration
              trigger={<Button>Show celebration</Button>}
              title="Workspace verified"
              description="Your public profile now shows the verified badge."
            />
          ),
        },
        {
          title: 'With a next step',
          render: () => (
            <Celebration
              trigger={<Button>Publish</Button>}
              title="Your page is live"
              description="Share the link so people can find it."
              tone="accent"
              continueLabel="Done"
              actions={<Button>Copy link</Button>}
            />
          ),
        },
        {
          title: 'Inline, at the end of onboarding',
          stack: true,
          render: () => (
            <CelebrationPanel
              announce={false}
              title="Setup complete"
              description="Your workspace is ready. Invite your team next."
              actions={<Button variant="primary">Invite people</Button>}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Celebrate milestones the person waited for or worked toward.',
        'Say what changed and what to do next in plain words.',
        'Use `CelebrationPanel` inline when the flow already has a page for its last step.',
      ],
      dont: [
        'Do not celebrate routine actions; use a Toast.',
        'Do not auto-close it on a timer.',
        'Do not put essential information in the media or decoration.',
      ],
    },
    related: {
      items: [
        { id: 'toast', reason: 'Routine success feedback that does not interrupt.' },
        { id: 'empty-state', reason: 'An "all clear" state with `kind="success"`.' },
        { id: 'dialog', reason: 'General modal content.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'dialog'],
      notes: ['Confetti colours come from `--mn-color-data-1` to `--mn-color-data-5`.'],
    },
    testing: {
      unit: [
        'Opens as a named, described dialog with focus on Continue; Continue runs `onContinue`, closes and returns focus; no axe violations.',
        'Decoration and media are `aria-hidden`; there is no close icon by default.',
        'Escape closes without `onContinue`.',
        'Controlled open; extra actions, custom continue label, no decoration, accent tone.',
        'Server and client render the same confetti (no randomness).',
        'The panel is a polite status with a heading, can be quiet, and forwards its ref.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
