import { useState } from 'react';
import { Button } from '@rookcom/monarch-react';
import { IncomingCall } from '@rookcom/monarch-react/collaboration';
import type { ComponentDocs } from '../../../internal/component-docs.js';

function Ringing({ kind }: { kind: 'audio' | 'video' }) {
  const [state, setState] = useState<'ringing' | 'joining' | string>('ringing');
  if (state !== 'ringing' && state !== 'joining')
    return <Button onClick={() => setState('ringing')}>{state} · Ring again</Button>;
  return (
    <IncomingCall
      placement="inline"
      kind={kind}
      caller={{ name: 'Robin Park' }}
      detail="Design team"
      joining={state === 'joining'}
      onAccept={({ video }) => {
        setState('joining');
        window.setTimeout(() => setState(video ? 'Answered with video' : 'Answered'), 1000);
      }}
      onDecline={() => setState('Declined')}
    />
  );
}

export default {
  id: 'incoming-call',
  title: 'IncomingCall',
  summary: 'The ringing banner for an incoming call, with Decline and Answer.',
  sections: {
    purpose: {
      summary:
        'Tells someone a person is calling and lets them answer (with or without video) or decline, without taking over the page. It never answers by itself and plays no sound; the product rings and joins. Needed by: Chat.',
      useWhen: ['A direct incoming call while the person is anywhere in the product.'],
      avoidWhen: [
        'Group meetings people join at will: show a "Join" button on the meeting instead.',
        'Missed-call history: use a notification or an inbox row.',
      ],
    },
    designPrinciples: {
      items: [
        'Never auto-answer: the camera and microphone only open after a deliberate choice.',
        'Do not steal focus by default; a ringing call should not interrupt typing.',
        'Decline is red and first; Answer is green and last, matching phone conventions.',
        'A video call can always be answered with audio only.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        '`IncomingCall` renders a non-modal `role="alertdialog"` and forwards its ref there.',
        'Render it while the call rings and remove it when it is answered, declined or ends.',
      ],
    },
    typedProperties: {
      source: 'typescript',
      notes: [
        '`onAccept({ video })`: `video` is true only for "Answer with video".',
        '`timeoutMs` + `onTimeout` stop ringing on the client; leave them out when the server ends the ring.',
        'Migrating from the app copy (Rook Chat `IncomingCall`): `useCallState().incoming` → render it when ringing; the member lookup → `caller={{ name, avatarUrl }}`; `incoming.call.kind === "VIDEO"` → `kind="video"`; `calls.join(call, { cameraOn })` → `onAccept({ video })`; `calls.decline()` → `onDecline`; `joining` is unchanged; `RING_TIMEOUT_MS` + `dismissIncoming` → `timeoutMs` + `onTimeout`; `startRinging()` stays in the product.',
      ],
    },
    variants: {
      items: [
        {
          prop: 'kind',
          values: ['audio', 'video'],
          description: 'Video adds "Answer with audio only". Exposed as `data-kind`.',
        },
        {
          prop: 'placement',
          values: ['floating', 'inline'],
          description:
            '`floating` fixes it to the top centre of the viewport; `inline` lets the product place it. Exposed as `data-placement`.',
        },
      ],
    },
    states: {
      items: [
        {
          name: 'Joining',
          attribute: 'disabled, aria-busy',
          description:
            'Answer buttons are disabled while the product joins; Decline stays available.',
        },
      ],
    },
    interaction: {
      items: [
        'Answer calls `onAccept({ video })`; Decline calls `onDecline`.',
        'After `timeoutMs`, `onTimeout` is called once.',
        'With `focusAnswer`, focus moves to Answer when it appears (for a call the person started waiting for).',
      ],
    },
    keyboard: {
      items: [
        { keys: 'Tab', action: 'Decline, then (for video) Answer with audio only, then Answer.' },
        { keys: 'Enter / Space', action: 'Activates the focused button.' },
      ],
      notes: ['Escape does nothing, so a call is never declined by accident.'],
    },
    accessibility: {
      role: 'alertdialog (aria-modal="false")',
      name: '"Incoming call from Robin Park" (`messages.label`)',
      items: [
        'Screen readers announce the alert dialog when it appears; its description says the kind of call.',
        'Buttons are named "Decline", "Answer", "Answer with video" and "Answer with audio only".',
        'The pulsing ring is decorative and stops with reduced motion.',
      ],
    },
    responsive: {
      items: [
        'Floating: full width minus 2rem, at most 24rem. At 390px the name truncates and the three 44px buttons stay on one row.',
      ],
    },
    theme: {
      items: [
        'Raised surface (`--mn-color-bg-raised`) with `--mn-shadow-xl` and `--mn-z-toast`.',
        'Decline uses the danger solid, Answer the success solid.',
        'Forced colours: a CanvasText border and no ring. Reduced motion: no slide-in and no ring.',
      ],
    },
    examples: {
      items: [
        { title: 'Audio call', render: () => <Ringing kind="audio" /> },
        { title: 'Video call', render: () => <Ringing kind="video" /> },
      ],
    },
    doDont: {
      do: [
        'Ring softly and stop ringing when the banner goes away.',
        'Keep it on screen until it is answered, declined or the caller hangs up.',
      ],
      dont: [
        'Do not answer automatically or open the camera before Answer.',
        'Do not stack several banners; show the newest call and queue the rest.',
      ],
    },
    related: {
      items: [
        {
          id: 'pre-join-lobby',
          reason: 'For calls people start or join deliberately, with a preview.',
        },
        { id: 'call-stage', reason: 'What opens after answering.' },
        { id: 'toast', reason: 'For a missed-call notice afterwards.' },
      ],
    },
    dependencies: { packages: ['react'], components: ['avatar', 'call-controls'] },
    testing: {
      unit: [
        'Named alert dialog with a description; Answer and Decline callbacks; axe passes.',
        'Video calls offer audio-only and video answers.',
        'Does not take focus by default; keyboard order; focusAnswer focuses Answer.',
        'Joining disables answering but not declining.',
        'timeoutMs calls onTimeout once; messages and detail; renders on the server.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
