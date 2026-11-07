import { useState } from 'react';
import { Button, MarketingConsentCheckbox, type MarketingConsent } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function ContactForm() {
  const [posted, setPosted] = useState<string | null>(null);
  return (
    <form
      style={{ display: 'flex', flexDirection: 'column', gap: 12, maxWidth: 420 }}
      onSubmit={(event) => {
        event.preventDefault();
        const data = new FormData(event.currentTarget);
        setPosted(`marketing=[${data.getAll('marketing').join(', ')}]`);
      }}
    >
      <MarketingConsentCheckbox
        name="marketing"
        description="Product updates and event invitations, about twice a month."
        policyLink={{ label: 'How we use your data', href: '#privacy' }}
      />
      <div>
        <Button type="submit" variant="primary">
          Send message
        </Button>
      </div>
      {posted ? <p>Submitted: {posted}</p> : null}
    </form>
  );
}

function ChannelsWithRecord() {
  const [consent, setConsent] = useState<MarketingConsent | null>(null);
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12, maxWidth: 420 }}>
      <MarketingConsentCheckbox
        channels={['email', 'sms', 'post']}
        description="Offers and new features, at most once a week."
        onChange={setConsent}
      />
      <pre style={{ margin: 0, whiteSpace: 'pre-wrap', fontSize: 12 }}>
        {consent ? JSON.stringify(consent, null, 2) : 'No choice yet'}
      </pre>
    </div>
  );
}

function SavedChoice() {
  const [channels, setChannels] = useState<MarketingConsent['channels']>({
    email: true,
    phone: false,
  });
  return (
    <MarketingConsentCheckbox
      channels={['email', 'phone']}
      checked={channels}
      description="Product news, about once a month."
      onChange={(consent) => setChannels(consent.channels)}
    />
  );
}

export default {
  id: 'marketing-consent-checkbox',
  title: 'MarketingConsentCheckbox',
  summary:
    'An unticked opt-in to marketing messages for sign-up and contact forms, one box per channel, with the wording reported for proof of consent.',
  sections: {
    purpose: {
      summary:
        'Asks whether the person wants marketing messages ("Send me product news by email") as a separate, active choice that is never ticked in advance, and reports the exact wording they saw with the time, so the product can store proof of consent. It is a pattern, not legal advice: the product’s legal team decides which channels need consent in each country and the wording.',
      useWhen: [
        'Sign-up, checkout and contact forms that offer marketing by email, text message, phone or post.',
        'A settings page that shows and changes a choice made earlier (controlled `checked`).',
      ],
      avoidWhen: [
        'Agreeing to the terms: use TermsAcceptance, and keep the two choices separate.',
        'Cookie and tracking categories: use the consent components (ConsentPreferences).',
        'Service messages the person cannot turn off (receipts, security alerts): do not ask for them here.',
      ],
    },
    designPrinciples: {
      items: [
        'Never pre-ticked: there is no way to start ticked; `defaultChecked` is ignored with a development warning.',
        'One choice per channel: with several channels each has its own unticked box, so agreeing to email does not mean agreeing to calls.',
        'Say what and how often in `description`, and how to stop in `unsubscribeNote`.',
        'Proof built in: the exact wording shown travels with every change and with the form.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`MarketingConsentCheckbox` forwards its ref to the wrapper `<div>`; other div props pass through.',
        '`onChange({ channels, text, at })`: `channels` has an entry for each channel shown, `text` is the visible wording with whitespace collapsed, `at` is an ISO timestamp.',
        'With `name`, hidden inputs post one `name=<channel>` per ticked channel and `<name>_text` (or `textName`) with the wording.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`channels` is any of `email`, `sms`, `phone`, `post`; default `["email"]`.',
        'Every visible string is a prop for translation: `label`, `channelLabels`, `description`, `unsubscribeNote`, `policyLink.label`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'channels',
          values: ['one channel', 'several channels'],
          description:
            'One channel: a single labelled checkbox with the notes as its description. Several: a labelled group (fieldset) with one checkbox per channel. Exposed as `data-channels`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Unticked',
          attribute: 'aria-checked="false" (each checkbox)',
          description: 'Always the starting state. Nothing is posted for that channel.',
        },
        {
          name: 'Ticked',
          attribute: 'aria-checked="true"',
          description: 'The person opted in to that channel.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled, disabled (checkboxes)',
          description: 'The choice cannot be changed, for example while saving.',
        },
      ],
    },
    interaction: {
      items: [
        'Clicking a box or its label toggles that channel only and calls `onChange` with all channels.',
        'Resetting the form unticks every channel.',
        'The policy link opens in the same tab unless `policyLink.target` says otherwise.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves between the channel boxes and the policy link.' },
        { keys: 'Space', action: 'Ticks or clears the focused box.' },
      ],
    },
    accessibility: {
      role: 'checkbox (one channel); group of checkboxes (several)',
      name: 'One channel: `label`. Several: the group is named by `label` (legend) and each box by its channel label.',
      items: [
        'The description, unsubscribe note and policy link describe the checkbox (one channel) or the group (several).',
        'Hidden inputs are not focusable and are not announced.',
      ],
    },
    responsive: {
      items: [
        'Text wraps at 390px; long links break instead of overflowing.',
        'On touch screens each channel row is at least 44px tall.',
      ],
    },
    theme: {
      items: [
        'Checkbox, muted text and link tokens; light, dark and more-contrast themes apply automatically.',
        'In forced colours the notes use the system text colour.',
      ],
    },
    examples: {
      items: [
        { title: 'In a contact form', render: () => <ContactForm />, stack: true },
        {
          title: 'Several channels, with the reported record',
          render: () => <ChannelsWithRecord />,
          stack: true,
        },
        {
          title: 'Settings page: showing an earlier choice',
          render: () => <SavedChoice />,
        },
      ],
    },
    doDont: {
      do: [
        'Store `{ channels, text, at }` with the person’s record as proof of consent.',
        'Say what is sent and how often in `description`.',
        'Show only the channels the product actually uses.',
      ],
      dont: [
        'Do not pre-tick a box or make sign-up depend on it.',
        'Do not merge marketing consent with agreeing to the terms.',
        'Do not describe the component as making the product compliant with a law; your legal team decides the wording.',
      ],
    },
    related: {
      items: [
        {
          id: 'terms-acceptance',
          reason: 'The required terms checkbox that usually sits above it.',
        },
        { id: 'checkbox', reason: 'The control it is built on.' },
        { id: 'form-field', reason: 'Wires the description to the checkbox.' },
        { id: 'consent-preferences', reason: 'Cookie and tracking choices, a separate decision.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['checkbox', 'form-field', 'link'],
    },
    testing: {
      unit: [
        'An unticked email checkbox with the notes as its description; axe passes.',
        'Never pre-ticked: `defaultChecked` is ignored and warns once in development.',
        'Reports the channels, the exact wording shown and an ISO time.',
        'Several channels: a labelled group with separately unticked boxes; ticking one leaves the others; axe passes.',
        'A plain form posts the ticked channels and the wording (`new FormData(form)`); reset unticks.',
        'Controlled `checked`, custom `textName` and `channelLabels`, and disabled.',
        'Renders on the server, unticked.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
