import { Avatar, Button, DeviceFrame } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function SamplePage() {
  return (
    <div
      style={{
        display: 'grid',
        gap: 'var(--mn-space-3)',
        justifyItems: 'center',
        padding: 'var(--mn-space-6) var(--mn-space-4)',
        textAlign: 'center',
      }}
    >
      <Avatar name="Northwind Studio" size="lg" />
      <strong>Northwind Studio</strong>
      <span style={{ color: 'var(--mn-color-text-muted)', fontSize: 'var(--mn-text-sm)' }}>
        Ceramics and workshops
      </span>
      {['Shop', 'Book a workshop', 'Newsletter'].map((label) => (
        <Button key={label} variant="secondary" style={{ width: '100%' }}>
          {label}
        </Button>
      ))}
    </div>
  );
}

export default {
  id: 'device-frame',
  title: 'DeviceFrame',
  summary: 'A phone or tablet outline around a preview, named as one image or a live group.',
  sections: {
    purpose: {
      summary:
        'Shows how something looks on a phone or tablet: a link page being edited, an email, a mobile layout. Needed by: Links (mobile preview beside the editor); GTM marketing pages use a similar phone mockup.',
      useWhen: [
        'An editor shows a live mobile preview next to the form that changes it.',
        'A marketing or onboarding page illustrates a mobile experience.',
      ],
      avoidWhen: [
        'Desktop product shots: use ProductFrame from the marketing entry.',
        'Actually testing responsive layouts: use the browser’s device tools.',
      ],
    },
    designPrinciples: {
      items: [
        'Chrome is decoration; the label says what the preview shows.',
        'Presentational by default: sample buttons and headings inside are neither read out nor focusable.',
        'Scales down to its container and keeps the device’s aspect ratio; never wider than its screen width.',
        'No images or device brand: the outline is drawn with tokens, so it follows the theme.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: ['Forwards its ref to the root `<div>`. Server-component safe (no state).'],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'Migrating from the app copy (Links `Iphone` in `components/ui/iphone.tsx`): `<Iphone width={280} height={607} className="w-full h-full">` → `<DeviceFrame label="…" width={280} height={607} interactive>`. Add a `label`; pass `interactive` because the Links preview scrolls. The white screen background becomes `--mn-color-bg-surface`; set your own background on the child for brand gradients. GTM’s `PhoneFrame` maps to `<DeviceFrame width={280} height={560} statusBarTime="9:41">`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'device',
          values: ['phone', 'tablet'],
          description: 'Outline shape and default size.',
        },
        {
          prop: 'interactive',
          values: ['false', 'true'],
          description: 'Presentational image, or a scrollable, focusable live preview.',
        },
        { prop: 'flat', values: ['false', 'true'], description: 'No side buttons or shadow.' },
      ],
    },
    states: {
      items: [
        { name: 'Device', attribute: 'data-device', description: '`phone` or `tablet`.' },
        {
          name: 'Interactive',
          attribute: 'data-interactive',
          description: 'Contents scroll and can be focused.',
        },
        { name: 'Flat', attribute: 'data-flat', description: 'No side buttons or shadow.' },
      ],
    },
    interaction: {
      items: [
        'Presentational: nothing inside responds (the contents are `inert`).',
        'Interactive: the screen scrolls with wheel, touch or arrow keys once focused; links and buttons inside work.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Interactive only: focus the screen, then the controls inside.' },
        {
          keys: '↑ / ↓ / Page Up / Page Down',
          action: 'Interactive only: scroll the focused screen.',
        },
      ],
      notes: ['Presentational frames are skipped by Tab.'],
    },
    accessibility: {
      role: '`img` (default) or `group` (`interactive`)',
      name: '`label`, e.g. "Preview of your link page on a phone".',
      items: [
        'The island, status bar and buttons are hidden from assistive technology.',
        'Interactive frames make the scrolling screen focusable so keyboard users can scroll it.',
      ],
    },
    responsive: {
      items: [
        'Width 100% of its container up to `width` pixels; height follows the aspect ratio.',
        'At 390px a phone frame fills the column without horizontal scroll.',
      ],
    },
    theme: {
      items: [
        'Outline in `--mn-color-bg-inverse`, screen in `--mn-color-bg-surface`, shadow `--mn-shadow-xl`, so it inverts with dark themes.',
        'Forced colours draw the outline and screen edges in CanvasText.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Phone preview with a status bar',
          render: () => (
            <div style={{ width: '17.5rem' }}>
              <DeviceFrame
                label="Preview of a link page on a phone"
                width={280}
                height={560}
                statusBarTime="9:41"
              >
                <SamplePage />
              </DeviceFrame>
            </div>
          ),
        },
        {
          title: 'Interactive, scrolling preview',
          render: () => (
            <div style={{ width: '15rem' }}>
              <DeviceFrame label="Live preview" width={240} height={420} interactive flat>
                <SamplePage />
                <SamplePage />
              </DeviceFrame>
            </div>
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Write a label that says what is previewed, not "iPhone".',
        'Use `interactive` for live editor previews so people can scroll them.',
      ],
      dont: [
        'Do not put real forms people must fill in inside a frame.',
        'Do not nest a frame inside ProductFrame.',
      ],
    },
    related: {
      items: [
        { id: 'marketing-section-blocks', reason: 'ProductFrame is the desktop counterpart.' },
        { id: 'section-card', reason: 'Holds the editor beside a preview.' },
      ],
    },
    dependencies: { packages: ['react'], components: [] },
    testing: {
      unit: [
        'Presentational: one named image with inert contents.',
        'Interactive: named group; Tab reaches the screen, then its links.',
        'Size, tablet device and flat.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
