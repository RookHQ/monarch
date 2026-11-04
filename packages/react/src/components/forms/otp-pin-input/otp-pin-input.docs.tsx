import { useState } from 'react';
import {
  Button,
  Field,
  FieldDescription,
  FieldError,
  FieldLabel,
  OTPPinInput,
} from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function VerifyDemo() {
  const [code, setCode] = useState('');
  const [state, setState] = useState<'idle' | 'checking' | 'wrong' | 'ok'>('idle');
  const verify = (value: string) => {
    setState('checking');
    // Demo only: the product verifies the code on its server.
    setTimeout(() => setState(value === '123456' ? 'ok' : 'wrong'), 500);
  };
  return (
    <div style={{ display: 'grid', gap: 12, width: '100%' }}>
      <Field invalid={state === 'wrong'} disabled={state === 'checking'}>
        <FieldLabel>Verification code</FieldLabel>
        <OTPPinInput
          value={code}
          onValueChange={(v) => {
            setCode(v);
            if (state === 'wrong') setState('idle');
          }}
          onComplete={verify}
        />
        <FieldDescription>Try 123456.</FieldDescription>
        <FieldError>
          {state === 'wrong' ? 'That code is not right. Check it and try again.' : null}
        </FieldError>
      </Field>
      <div>
        <Button
          variant="primary"
          loading={state === 'checking'}
          disabled={code.length !== 6}
          onClick={() => verify(code)}
        >
          {state === 'ok' ? 'Verified' : 'Confirm'}
        </Button>
      </div>
    </div>
  );
}

export default {
  id: 'otp-pin-input',
  title: 'OTPPinInput',
  summary: 'One-time code or PIN entry in separate boxes, backed by one accessible input.',
  sections: {
    purpose: {
      summary:
        'Enters a short numeric or alphanumeric code (an email or SMS one-time code, a step-up PIN) shown as one box per character, while remaining a single real input for autofill, paste, mobile keypads and screen readers.',
      useWhen: [
        'Sign-in or step-up verification with a code sent to the user.',
        'A short PIN where the box per character shows how many are needed.',
      ],
      avoidWhen: [
        'Passwords or long secrets: use a password Input.',
        'Codes of variable length: use Input.',
      ],
    },
    designPrinciples: {
      items: [
        'One field, many boxes: the boxes are a picture of one `<input>`, so the platform features for one-time codes just work.',
        'Forgiving entry: spaces, dashes and other characters in a pasted or typed code are dropped.',
        'Verification is the product’s: `onComplete` hands over the code, and the product shows the result through Field.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'Renders `.mn-otp-pin-input` with `aria-hidden` slots and a transparent `<input>` laid over them. The ref and native input props go to the input; `className` goes to the wrapper.',
        'Reads Field context through `useFieldControl` for its label, description, `invalid` and `disabled`.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`maxLength`, `minLength` and `pattern` are set by the component from `length` and `mode` and cannot be passed.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'length',
          values: ['number (default 6)'],
          description: 'Number of boxes and characters.',
        },
        {
          prop: 'mode',
          values: ['numeric', 'alphanumeric'],
          description:
            '`numeric` keeps digits and opens the number keypad; `alphanumeric` keeps A–Z and 0–9, upper-cased.',
        },
        {
          prop: 'mask',
          values: ['boolean'],
          description: 'Dots instead of characters; the input becomes `type="password"`.',
        },
        {
          prop: 'groupSize',
          values: ['number'],
          description: 'A visual gap after every N boxes (e.g. 3 for "123 456").',
        },
        { prop: 'size', values: ['md', 'lg'], description: 'Box size. Default `md`.' },
      ],
    },
    states: {
      items: [
        {
          name: 'Focused',
          attribute: 'data-focused',
          description: 'The next box to fill shows the focus ring and a blinking caret.',
        },
        {
          name: 'Filled',
          attribute: 'data-filled on a slot',
          description: 'The box shows its character or a dot.',
        },
        {
          name: 'Complete',
          attribute: 'data-complete',
          description: 'Every box is filled; `onComplete` has fired.',
        },
        {
          name: 'Invalid',
          attribute: 'data-invalid, aria-invalid="true"',
          description: 'Danger borders, from `invalid` or the Field.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled, disabled',
          description: 'Muted boxes, for example while verifying.',
        },
      ],
    },
    interaction: {
      items: [
        'Typing fills the boxes in order; characters that do not fit the `mode` are dropped.',
        'Pasting a full code replaces the value; pasting a fragment appends it. Spaces and dashes are removed.',
        'Autofill from SMS or email (`autocomplete="one-time-code"`) fills every box at once.',
        '`onComplete` fires each time the code reaches `length` characters, whether typed, pasted or autofilled.',
        'The caret always stays at the end, so editing is by Backspace; clicking any box focuses the field.',
        'Controlled with `value` + `onValueChange` (set `""` to reset after a failed attempt), or uncontrolled with `defaultValue`.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Characters', action: 'Fill the next box.' },
        { keys: 'Backspace', action: 'Clears the last filled box.' },
        { keys: 'Ctrl/⌘ + V', action: 'Pastes a code.' },
        { keys: 'Tab', action: 'Moves focus in and out as one field.' },
      ],
    },
    accessibility: {
      role: 'textbox (one native input)',
      name: 'A FieldLabel or `aria-label` such as "6-digit code". Screen readers hear one field and its value, not six unnamed boxes.',
      items: [
        'The visual boxes are `aria-hidden`.',
        'Errors and help come from Field and are linked with `aria-describedby`.',
        '`inputmode="numeric"` and `pattern="[0-9]*"` open the number keypad on phones without blocking paste.',
        'With `mask`, the input is `type="password"`, so assistive technology does not read the PIN aloud.',
      ],
    },
    responsive: {
      items: [
        'Boxes are 2.75rem wide (3.25rem at `lg`) and shrink evenly when the container is narrower, down to 1.5rem each, so even an 8-character grouped code fits a 320px screen. In a shrink-to-fit container (inline or `width: max-content`) give the field a width, or the boxes stay at that minimum.',
        'The input font size is 16px, which keeps iOS from zooming on focus.',
      ],
    },
    theme: {
      items: [
        'Box borders, surfaces and the focus ring are semantic tokens, so light, dark and more-contrast themes apply.',
        'The caret blink and box transitions stop under `prefers-reduced-motion`.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Verify a code',
          stack: true,
          render: () => <VerifyDemo />,
          code: `<Field invalid={wrong} disabled={checking}>
  <FieldLabel>Verification code</FieldLabel>
  <OTPPinInput value={code} onValueChange={setCode} onComplete={verify} />
  <FieldError>{wrong ? 'That code is not right. Check it and try again.' : null}</FieldError>
</Field>`,
        },
        {
          title: 'PIN and groups',
          stack: true,
          render: () => (
            <div style={{ display: 'grid', gap: 16, width: '100%' }}>
              <OTPPinInput aria-label="4-digit PIN" length={4} mask defaultValue="25" />
              <OTPPinInput aria-label="Grouped code" groupSize={3} defaultValue="4829" />
              <OTPPinInput
                aria-label="Recovery code"
                length={8}
                mode="alphanumeric"
                groupSize={4}
                defaultValue="K7P2"
              />
            </div>
          ),
          code: `<OTPPinInput aria-label="4-digit PIN" length={4} mask />
<OTPPinInput aria-label="Grouped code" groupSize={3} />
<OTPPinInput aria-label="Recovery code" length={8} mode="alphanumeric" groupSize={4} />`,
        },
      ],
    },
    doDont: {
      do: [
        'Verify in `onComplete` and also offer a Confirm button for users who type slowly.',
        'Disable the field (through Field) while the code is verified, and clear it after a wrong code.',
        'Tell the user where the code was sent and how many characters it has.',
      ],
      dont: [
        'Do not build one input per box; autofill and screen readers break.',
        'Do not reveal whether a code was close; just say it was not right.',
      ],
    },
    related: {
      items: [
        { id: 'text-field', reason: 'Input for codes of variable length.' },
        { id: 'form-field', reason: 'Label, help and error wiring.' },
        { id: 'button', reason: 'The Confirm action next to it.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['form-field'],
    },
    testing: {
      unit: [
        'One labelled input with `one-time-code`, numeric keypad and pattern; slots are aria-hidden.',
        'Typing fills slots, drops other characters, and `onComplete` fires once at full length.',
        'Backspace deletes.',
        'Pasting a full code (with spaces and dashes) replaces; a fragment appends.',
        'Other lengths, alphanumeric mode with upper-casing, and groups.',
        'Mask shows dots and uses a password input.',
        'Controlled mode and reset by the parent.',
        'Reads Field label, error, invalid and disabled.',
      ],
      axe: true,
      ssr: true,
      browser: ['Visual baselines: /__gallery', 'Cross-browser axe: /components/otp-pin-input'],
    },
  },
} satisfies ComponentDocs;
