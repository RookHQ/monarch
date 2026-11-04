import { useState } from 'react';
import { SwitchSettingsItem, SwitchSettingsList } from '@rookcom/monarch-react';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function wait(ms: number) {
  return new Promise<void>((resolve) => setTimeout(resolve, ms));
}

function NotificationsDemo() {
  const [prefs, setPrefs] = useState({ alerts: true, devices: true, digest: false });
  const save = (key: keyof typeof prefs) => async (next: boolean) => {
    await wait(700);
    setPrefs((p) => ({ ...p, [key]: next }));
  };
  return (
    <div style={{ width: '100%', maxWidth: 560 }}>
      <SwitchSettingsList
        title="Security notifications"
        description="Emails about activity on your account."
      >
        <SwitchSettingsItem
          label="Security alerts"
          description="Password changes and new recovery methods."
          checked={prefs.alerts}
          onCheckedChange={save('alerts')}
        />
        <SwitchSettingsItem
          label="New device sign-in"
          description="When your account is used on a device for the first time."
          checked={prefs.devices}
          onCheckedChange={save('devices')}
        />
        <SwitchSettingsItem
          label="Weekly summary"
          checked={prefs.digest}
          onCheckedChange={save('digest')}
        />
      </SwitchSettingsList>
    </div>
  );
}

function FailingDemo() {
  return (
    <div style={{ width: '100%', maxWidth: 560 }}>
      <SwitchSettingsList title="Delivery">
        <SwitchSettingsItem
          label="In-app notifications"
          description="This example always fails, to show rollback and retry."
          defaultChecked
          onCheckedChange={async () => {
            await wait(700);
            throw new Error('Check your connection and try again.');
          }}
        />
      </SwitchSettingsList>
    </div>
  );
}

export default {
  id: 'switch-settings-list',
  title: 'SwitchSettingsList',
  summary: 'On/off settings that each save on their own, with saving, error and retry per row.',
  sections: {
    purpose: {
      summary:
        'Shows a group of on/off preferences, such as notification or privacy settings, where each switch saves immediately. Every row has its own saving, saved and failed state, so one failure never hides behind a page-level toast.',
      useWhen: [
        'Each toggle is persisted on its own (a PATCH per preference) rather than with a form Save button.',
        'Saves can fail and the user must see which setting did not stick.',
      ],
      avoidWhen: [
        'Switches are submitted together with other fields: use Field with `layout="row"` and a Switch inside a form.',
        'A single setting with no group: a Switch with `label` and `description`, or SettingsRow.',
      ],
    },
    designPrinciples: {
      items: [
        'Optimistic: the switch moves at once and rolls back only if the save fails.',
        'Status lives on the row it belongs to: a spinner while saving, a brief "Saved", or the error with Retry.',
        'Results are announced, not only shown, so screen-reader users hear "Saved" or the error.',
        'No data fetching: the product passes `checked` and an `onCheckedChange` that returns its save promise.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`SwitchSettingsList` renders a `<div>` (role `group` when it has a title or label) with an optional heading and a `<ul>` of rows; the ref goes to the `<div>`.',
        '`SwitchSettingsItem` renders an `<li>` (ref) with the label, description, status and a Monarch Switch.',
        'Status wording (`labels`: saving, saved, error, retry), `savedDuration` and `getErrorMessage` are set on the list and can be overridden per row.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [],
      notes: [
        'One visual form: a bordered list of rows. Wording is configurable through `labels` for translation.',
      ],
    },
    states: {
      items: [
        {
          name: 'Idle',
          attribute: 'data-status="idle"',
          description: 'The switch shows the saved value.',
        },
        {
          name: 'Saving',
          attribute: 'data-status="saving", aria-busy="true" on the switch',
          description:
            'The switch already shows the new value; a spinner named "Saving" sits beside it and further toggles are ignored until the save settles.',
        },
        {
          name: 'Saved',
          attribute: 'data-status="saved"',
          description:
            'A check and "Saved" for `savedDuration` (2 s), announced politely. Only when `onCheckedChange` returned a promise.',
        },
        {
          name: 'Error',
          attribute: 'data-status="error"',
          description:
            'The switch rolls back, the error shows under the label and is announced, and a Retry button repeats the failed value.',
        },
        {
          name: 'Disabled',
          attribute: 'data-disabled, disabled on the switch',
          description: 'Disabled text colour; the switch cannot be toggled.',
        },
      ],
    },
    interaction: {
      items: [
        'Toggling calls `onCheckedChange(next)` with the switch already moved.',
        'Resolved promise: "Saved" appears and clears. Rejected: the switch returns to its previous value and the row shows the error and Retry.',
        'Retry calls `onCheckedChange` again with the value that failed. Toggling the switch again also retries.',
        'Uncontrolled rows (`defaultChecked`) keep the new value after a successful save. Controlled rows (`checked`) show the optimistic value until the parent’s `checked` changes, and always follow a new `checked` when nothing is saving.',
        '`onStatusChange(status)` reports each row’s status, for example to block navigation while saving.',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Moves between switches, and to a row’s Retry button when shown.' },
        { keys: 'Space / Enter', action: 'Toggles the focused switch; activates Retry.' },
      ],
    },
    accessibility: {
      role: 'group (list), switch (each row)',
      name: 'The list is named by `title` (or `aria-label`); each switch by its row `label`.',
      items: [
        'The row description is the switch’s accessible description; while a save has failed, the error is added to it.',
        'Each row has a polite `role="status"` region that announces "Saved" or the error. The saving spinner is a named status.',
        'Retry is named "Retry {label}", so several failed rows stay distinguishable.',
        'Rows are list items in a `<ul>`, so the number of settings is announced.',
      ],
    },
    responsive: {
      items: [
        'Label and description wrap; the switch and status stay on the right.',
        'Below 480px a row may wrap so a long status or the Retry button moves under the text, with no horizontal scrolling at 320px.',
      ],
    },
    theme: {
      items: [
        'Surface, borders and text are semantic tokens, so light, dark and more-contrast themes apply automatically.',
        '"Saved" uses `--mn-color-success-strong` and errors `--mn-color-danger-strong`, both readable on the surface in every theme; the switch track follows the product accent.',
        'The "Saved" fade uses the duration tokens, which are zero under reduced motion.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Notification preferences',
          stack: true,
          render: () => <NotificationsDemo />,
        },
        {
          title: 'Failed save, rollback and retry',
          stack: true,
          render: () => <FailingDemo />,
        },
      ],
    },
    doDont: {
      do: [
        'Return the save promise from `onCheckedChange`; that is what drives the states.',
        'Reject with an Error whose message tells the user what to do.',
        'Group related settings under a `title`, one list per group.',
      ],
      dont: [
        'Do not also show a toast for each save; the row already reports it.',
        'Do not use it inside a form that has its own Save button.',
        'Do not resolve the promise when the save failed.',
      ],
    },
    related: {
      items: [
        { id: 'switch', reason: 'The control used in each row.' },
        { id: 'settings-section', reason: 'Card of mixed settings with one Save footer.' },
        { id: 'form-field', reason: '`layout="row"` for a single switch row inside a form.' },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['spinner', 'switch'],
    },
    testing: {
      unit: [
        'A titled, described group of list items; switches are labelled and described by their rows.',
        'Uncontrolled: Space toggles optimistically, aria-busy and a "Saving" status while pending, toggles during a save are ignored, "Saved" is announced and clears after `savedDuration`.',
        'Failure: rolls back, the error describes the switch and is announced, "Retry {label}" repeats the failed value, and `onStatusChange` reports saving → error → saving → saved.',
        'Controlled: keeps the optimistic value until the parent updates, then follows the parent.',
        'A synchronous handler toggles without a "Saved" state; disabled rows do not toggle; `aria-label` names the group.',
        'Labels and error messages can be set per list and per row.',
        'Renders on the server.',
      ],
      axe: true,
      ssr: true,
      browser: [
        'Visual baselines: /__gallery',
        'Cross-browser axe: /components/switch-settings-list',
      ],
    },
  },
} satisfies ComponentDocs;
