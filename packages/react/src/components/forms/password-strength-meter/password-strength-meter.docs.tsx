import { useState, type ChangeEvent } from 'react';
import {
  Field,
  FieldLabel,
  PasswordInput,
  PasswordStrengthMeter,
  type PasswordRule,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function SignUpDemo() {
  const [password, setPassword] = useState('');
  return (
    <Field style={{ width: '100%', maxWidth: 320 }}>
      <FieldLabel>Choose a password</FieldLabel>
      <PasswordInput
        purpose="new"
        value={password}
        onChange={(e: ChangeEvent<HTMLInputElement>) => setPassword(e.target.value)}
        aria-describedby="signup-strength"
      />
      <PasswordStrengthMeter id="signup-strength" password={password} />
    </Field>
  );
}

const PASSPHRASE_RULES: PasswordRule[] = [
  { id: 'length', label: 'At least 16 characters', test: (p: string) => p.length >= 16 },
  {
    id: 'words',
    label: 'Three or more words',
    test: (p: string) => p.trim().split(/\s+/).length >= 3,
  },
];

export default {
  id: 'password-strength-meter',
  title: 'PasswordStrengthMeter',
  summary: 'Strength bar, strength in words and a met / not-met rules list for a new password.',
  sections: {
    purpose: {
      summary:
        'Tells people, while they type a new password, how strong it is and which requirements are still missing, in words and icons as well as colour. Needed by: rookid sign-up and password reset, and any app that sets passwords (rookcalendar, rookdossier, rookdots, rookpulse frontends). There is no shadcn/ui copy in the apps.',
      useWhen: [
        'Under a PasswordInput with `purpose="new"`: sign-up, reset, change password.',
        'When the server enforces rules that people should see before they submit.',
      ],
      avoidWhen: [
        'Signing in: never rate an existing password.',
        'A progress or score that is not a password: use ProgressBar.',
      ],
    },
    designPrinciples: {
      items: [
        'Never colour only: each segment colour comes with the strength in words, and each rule with an icon and hidden "Met:" / "Not met:" text.',
        'Pluggable: pass `rules` that match your server, and `score` for a real estimator such as zxcvbn.',
        'Nothing is stored or sent: the component only reads the `password` prop.',
        'Quiet: only the strength line is live, and it changes only when the level changes.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders `.mn-password-strength-meter`: a decorative four-part bar, a strength line and a list of rules. The ref and HTML attributes go to the root `<div>`.',
        'Exports `DEFAULT_PASSWORD_RULES`, `scorePassword(password, rules)` and `PASSWORD_STRENGTH_METER_MESSAGES`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        'The default score is the share of rules met (0 to 3), with 4 for 14 or more characters once every rule is met.',
        'Rule labels are data, so translate them by passing your own `rules`.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'showRules',
          values: ['true', 'false'],
          description:
            'The rules list under the bar (default), or only the bar and the strength line.',
        },
        {
          prop: 'rules',
          values: ['PasswordRule[]'],
          description: 'Requirements with their own tests; `[]` hides the list.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Empty',
          attribute: 'data-empty',
          description: '"Password strength: Not entered", no filled segments, every rule not met.',
        },
        {
          name: 'Score 0–4',
          attribute: 'data-score="0"…"4"',
          description:
            'Very weak and Weak fill one segment in danger, Fair two in warning, Good three and Strong four in success.',
        },
        {
          name: 'Rule met',
          attribute: 'data-met on the list item',
          description: 'Check icon, success text colour and the hidden prefix "Met:".',
        },
      ],
    },
    interaction: {
      items: ['Updates on every render from the `password` prop; it has no controls of its own.'],
    },
    keyboard: {
      items: [],
      notes: [
        'Not interactive: it adds no tab stops. Keyboard behaviour belongs to the PasswordInput it describes.',
      ],
    },
    accessibility: {
      role: 'none on the root; a list for the rules',
      name: 'The rules list is named by `messages.rulesLabel` ("Password requirements").',
      items: [
        'The strength line is `aria-live="polite"`, so a level change is announced once.',
        'The bar is `aria-hidden`; the strength line says the same thing in words.',
        'Each rule reads "Met: A number" or "Not met: A number" through visually hidden text.',
        'Give it an `id` and add that id to the PasswordInput’s `aria-describedby`; use `showRules={false}` there if the full list is too long to hear on every focus.',
      ],
    },
    responsive: {
      items: ['The bar and list fill the container at every width; rule text wraps.'],
    },
    theme: {
      items: [
        'Segments use `--mn-color-danger-solid`, `--mn-color-warning-solid` and `--mn-color-success-solid` on `--mn-color-bg-emphasis`; met rules use `--mn-color-success-strong`.',
        'Forced colours: segments are outlined and filled with CanvasText, so the count stays visible.',
        'The segment fill transition is removed with reduced motion.',
      ],
    },
    examples: {
      items: [
        { title: 'With a new password', stack: true, render: () => <SignUpDemo /> },
        {
          title: 'States',
          stack: true,
          render: () => (
            <div style={{ display: 'grid', gap: 16, width: '100%', maxWidth: 320 }}>
              <PasswordStrengthMeter password="sample" showRules={false} />
              <PasswordStrengthMeter password="Sample12" showRules={false} />
              <PasswordStrengthMeter password="Sample12!" showRules={false} />
              <PasswordStrengthMeter password="Sample12! with more" showRules={false} />
            </div>
          ),
        },
        {
          title: 'Custom passphrase rules',
          stack: true,
          render: () => (
            <PasswordStrengthMeter
              password="blue paper lamp"
              rules={PASSPHRASE_RULES}
              style={{ width: '100%', maxWidth: 320 }}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Use the same rules as the server, so a password that passes here is accepted.',
        'Plug in a real estimator through `score` for high-value accounts.',
      ],
      dont: [
        'Do not show it on sign-in forms.',
        'Do not block submit on the score alone; block on the rules the server enforces.',
        'Do not rely on the bar colour to explain what is missing; keep the rules list visible on sign-up.',
      ],
    },
    related: {
      items: [
        { id: 'password-input', reason: 'The field it describes.' },
        { id: 'progress-bar', reason: 'General progress, not password strength.' },
        { id: 'form-field', reason: 'Label and description wiring.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['visually-hidden'] },
    testing: {
      unit: [
        'scorePassword levels and the default rule ids.',
        'Strength in a polite live region; each rule says met or not met in text.',
        'Empty state: "Not entered" and no filled segments.',
        'Custom rules, custom score and translated messages.',
        '`showRules={false}` hides the list.',
        'Describes a PasswordInput through aria-describedby and updates while typing.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
