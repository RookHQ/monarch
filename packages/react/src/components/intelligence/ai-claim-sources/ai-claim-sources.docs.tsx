import { useState } from 'react';
import { AiClaimSources, type AiClaim, type AiClaimSource } from '@rookcom/monarch-react/ai';
import type { ComponentDocs } from '../../../internal/component-docs.js';
// The scenario of the primary example, shared with iOS and Android (CROSS-PLATFORM.md section 8).
import fixture from './ai-claim-sources.fixture.json';

const sources: AiClaimSource[] = fixture.data.sources.map((s) => ({
  id: s.id,
  title: s.title,
  kind: s.kind as AiClaimSource['kind'],
  ...('url' in s ? { url: s.url } : {}),
  ...('publishedAt' in s ? { publishedAt: s.publishedAt } : {}),
}));

const claims: AiClaim[] = fixture.data.claims.map((c) => ({
  id: c.id,
  text: c.text,
  support: c.support as AiClaim['support'],
  evidence: ('evidence' in c ? c.evidence : []).map((ev) => ({
    id: ev.id,
    sourceId: ev.sourceId,
    quote: ev.quote,
    location: ev.location,
    stance: ev.stance as 'supports' | 'contradicts',
  })),
}));

function SelectDemo() {
  const [selected, setSelected] = useState<string | null>('c2');
  const [log, setLog] = useState('Nothing reported yet.');
  return (
    <>
      <p>
        {claims.map((claim) => (
          <span key={claim.id}>
            {claim.id === selected ? <mark>{claim.text}</mark> : claim.text}{' '}
          </span>
        ))}
      </p>
      <AiClaimSources
        claims={claims}
        sources={sources}
        locale="en-GB"
        selectedClaimId={selected}
        onSelectClaim={setSelected}
        defaultExpandedIds={['c2']}
        onOpenSource={(source) => setLog(`Open ${source.title}`)}
        onFlag={(id) => setLog(`Reported a problem with claim ${id}`)}
        onVerify={(id) => setLog(`Checking claim ${id} again`)}
      />
      <p role="status">{log}</p>
    </>
  );
}

export default {
  id: 'ai-claim-sources',
  title: 'AiClaimSources',
  summary: 'Each claim of an AI answer with the exact passages that back or contradict it.',
  sections: {
    purpose: {
      summary:
        'Lets a person check an AI answer without leaving it. The answer is broken into its claims; each claim says in words how well the sources back it and opens to show the exact passages, where they are in the source and whether they agree. A claim with no source says so.',
      useWhen: [
        'Your product checks an answer against its sources and can report claims with the passages found for each.',
        'People act on the answer (policy, finance, health, legal, support) and need to see the evidence, not only a list of links.',
        'Beside or under an answer, with `onSelectClaim` highlighting the matching sentence.',
      ],
      avoidWhen: [
        'You only know which sources were used, not which claim each one backs: use AiSourceList.',
        'One inline reference in a sentence: use AiCitation.',
        'Reading a whole source with the passage highlighted: use AiSourcePreview (open it from `onOpenSource`).',
        'A single confidence number for the answer: use AiConfidenceMeter.',
      ],
    },
    designPrinciples: {
      items: [
        'Bad news is never hidden or softened: "No source supports this" and "Sources disagree" are on the closed row, in words, with an icon.',
        'Evidence is quoted word for word in a blockquote, always with a visible source title; location, date and stance follow when known.',
        'The component only shows what it is given. It never fetches, never checks a claim and never invents a passage; "Check again" and "Report a problem" are explicit buttons that call back.',
        'Untrusted output stays safe: only safe URLs become links, and they open with `noopener noreferrer nofollow`.',
        'Built from existing parts: SegmentedControl, Button, CopyButton and the source glyph from AiCitation.',
      ],
    },
    apiReference: {
      source: 'typescript',
      notes: [
        'The ref goes to the root `<section>`; native attributes pass through.',
        '`sources` accepts `AiSourcePart` objects from a message as they are; `publishedAt` is optional.',
        '`aiClaimSummary(claims, messages.summary?)` returns the summary line, and `aiClaimCounts(claims)` the numbers behind it, for use outside the component (a badge, a tooltip).',
        'A source with a safe URL is a real link. `onOpenSource` is still called on click with the event: call `event.preventDefault()` to open your own preview instead.',
        'Expanded claims, the filter and the selected claim are each controlled or uncontrolled.',
      ],
    },
    typedProperties: { source: 'typescript' },
    variants: {
      items: [],
      notes: [
        'There are no visual variants. Each claim looks according to its `support`: supported, partial, unsupported, conflicting or unchecked.',
        'Optional actions appear only when their callbacks are given: `onFlag`, `onVerify` (unchecked claims only) and `onOpenSource`.',
      ],
    },
    states: {
      items: [
        {
          name: 'Support level',
          attribute: 'data-support (on the claim)',
          description:
            'supported, partial, unsupported, conflicting or unchecked. Shown as an icon and text; the edge colour only repeats it.',
        },
        {
          name: 'Expanded',
          attribute: 'aria-expanded on the claim button, data-expanded on the claim',
          description: 'The passages, the plain-words note and the claim actions are shown.',
        },
        {
          name: 'Selected',
          attribute: 'data-selected on the claim, aria-current on its button',
          description:
            'The claim matching `selectedClaimId`, e.g. the sentence picked in the answer.',
        },
        {
          name: 'Filter',
          attribute: 'data-filter (on the root)',
          description:
            '`all`, or `needs-checking` to show every claim that is not fully supported.',
        },
        {
          name: 'Stance',
          attribute: 'data-stance (on the passage)',
          description:
            '`supports` or `contradicts`, shown as icon and text; a contradicting passage has a dashed edge.',
        },
        {
          name: 'Empty',
          description:
            'No claims: a short message and no filter. "Needs checking" with nothing to show says every claim is supported.',
        },
      ],
    },
    interaction: {
      items: [
        'Activating a claim opens or closes its evidence and selects it (`onSelectClaim`), so the product can highlight that sentence.',
        'The filter switches between all claims and those that need checking; the result is announced.',
        '"Open source" is a link in a new tab for a safe URL, otherwise a button that calls `onOpenSource`; with neither it is not shown.',
        '"Copy quote" copies the exact passage and confirms it visibly and to screen readers.',
        '"Report a problem" calls `onFlag(claimId)`; "Check again" calls `onVerify(claimId)` on unchecked claims. Nothing runs until the person presses them.',
      ],
    },
    keyboard: {
      items: [
        {
          keys: 'Tab',
          action:
            'Moves through the filter, then each claim; inside an open claim through Open source, Copy quote and the claim actions before the next claim.',
        },
        {
          keys: 'Enter / Space',
          action: 'Opens or closes the focused claim and selects it; activates buttons and links.',
        },
        {
          keys: 'Arrow Down / Arrow Up',
          action: 'On a claim: moves to the next or previous claim.',
        },
        { keys: 'Home / End', action: 'On a claim: moves to the first or last claim.' },
        { keys: 'Arrow Left / Arrow Right', action: 'On the filter: changes the filter.' },
      ],
    },
    accessibility: {
      role: 'group (a named `<section>`)',
      name: 'The visible heading, `messages.heading` ("Where this came from").',
      items: [
        'Claims are an ordered list; each is a disclosure button with `aria-expanded` and `aria-controls`, described by its support level and passage count.',
        'Support and stance are always an icon plus text, never colour alone.',
        'Each passage is a `<figure>` with a `<blockquote>` and a caption naming the source in a `<cite>`; dates use `<time>`.',
        'Repeated actions have distinct names ("Copy quote from Team handbook"); external links say they open in a new tab.',
        'The selected claim has `aria-current="true"`. Filter changes are announced through a polite live region.',
        'The filter is a radio group named by `messages.filterLabel`.',
      ],
    },
    responsive: {
      items: [
        'The header wraps so the filter drops under the summary; claims, quotes and captions wrap at any width.',
        'On coarse pointers claim buttons and actions grow to 44px targets.',
        'RTL: logical properties throughout; the chevron and edges follow the start side.',
      ],
    },
    theme: {
      items: [
        'Success, warning and danger tokens for the support chip and edges; `--mn-color-ai-subtle` and `--mn-color-ai-border` for the selected claim.',
        'Forced colours: borders and chips use CanvasText, the selected claim gets a Highlight outline.',
        'Reduced motion: the chevron does not animate.',
      ],
    },
    examples: {
      items: [
        {
          title: 'Claims with supporting and contradicting passages',
          stack: true,
          render: () => (
            <AiClaimSources
              claims={claims}
              sources={sources}
              locale={fixture.data.locale}
              defaultExpandedIds={fixture.data.expanded}
            />
          ),
        },
        {
          title: 'Only what needs checking, with actions',
          stack: true,
          render: () => (
            <AiClaimSources
              claims={claims}
              sources={sources}
              locale="en-GB"
              defaultFilter="needs-checking"
              defaultExpandedIds={['c4', 'c5']}
              onFlag={() => {}}
              onVerify={() => {}}
            />
          ),
        },
        {
          title: 'Highlighting the selected claim in the answer',
          stack: true,
          render: () => <SelectDemo />,
        },
        {
          title: 'Translated',
          stack: true,
          render: () => (
            <AiClaimSources
              claims={claims.slice(0, 2)}
              sources={sources}
              locale="de-DE"
              defaultExpandedIds={['c1']}
              messages={{
                heading: 'Woher das stammt',
                summary: (c) => `${c.total} Aussagen, ${c.needsChecking} zu prüfen`,
                filterLabel: 'Aussagen anzeigen',
                filters: { all: 'Alle', 'needs-checking': 'Zu prüfen' },
                support: { supported: 'Belegt', conflicting: 'Quellen widersprechen sich' },
                passages: (n) => (n === 1 ? '1 Textstelle' : `${n} Textstellen`),
                stances: {
                  supports: 'Stützt die Aussage',
                  contradicts: 'Widerspricht der Aussage',
                },
                published: (date) => `Veröffentlicht am ${date}`,
                kinds: { doc: 'Dokument', web: 'Webseite' },
                openSource: 'Quelle öffnen',
                newTab: '(öffnet in neuem Tab)',
                copyQuote: 'Zitat kopieren',
                copied: 'Kopiert',
                fromSource: (title) => `aus ${title}`,
              }}
            />
          ),
        },
      ],
    },
    doDont: {
      do: [
        'Pass every claim, including the ones with no source; that is the point of the view.',
        'Quote the passage exactly as it is in the source, and give a location when you have one.',
        'Mark a claim `unchecked` rather than `supported` when the check has not run.',
        'Use `onSelectClaim` and `selectedClaimId` to tie the list to the sentences of the answer.',
      ],
      dont: [
        'Do not drop or reword unsupported and conflicting claims to make the answer look better.',
        'Do not paraphrase passages or pass model-written summaries as quotes.',
        'Do not start a costly re-check on your own when the view opens; wait for "Check again".',
      ],
    },
    related: {
      items: [
        {
          id: 'ai-source-list',
          reason: 'Lists the sources of an answer without mapping them to claims.',
        },
        {
          id: 'ai-citation',
          reason: 'The inline marker in the answer; shares the source helpers.',
        },
        {
          id: 'ai-source-preview',
          reason: 'Open it from `onOpenSource` to read the source with the quote highlighted.',
        },
        {
          id: 'ai-confidence-meter',
          reason: 'One overall confidence value rather than per-claim evidence.',
        },
      ],
    },
    dependencies: {
      packages: ['react'],
      components: ['button', 'copy-button', 'segmented-control', 'ai-citation'],
    },
    testing: {
      unit: [
        '`aiClaimCounts`, `aiClaimNeedsChecking` and `aiClaimSummary`, including empty levels and a translated summary.',
        'Labelled region and list; every claim shows its support in words and a passage count that describes its button; ref and props forwarding.',
        'A claim opens to blockquotes with source title, kind, location, formatted date and stance, and closes again.',
        'Conflicting, unsupported and unchecked claims state it plainly.',
        'Safe URLs are links with a safe `rel`; no URL or an unsafe one falls back to `onOpenSource`; a link click can be taken over; unknown sources are named.',
        'Copy quote writes the exact passage and announces it.',
        'Filter: uncontrolled with announcement and arrow keys, controlled, and the empty result.',
        'Controlled and uncontrolled expanded ids and selected claim.',
        'Report a problem on every open claim, Check again only on unchecked ones.',
        'Keyboard order through filter, claim, its actions and the next claim; arrows, Home and End between claims.',
        'Translated strings with partial groups; empty state.',
      ],
      axe: true,
      ssr: true,
    },
  },
} satisfies ComponentDocs;
