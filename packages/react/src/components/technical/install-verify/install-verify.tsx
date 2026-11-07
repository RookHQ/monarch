'use client';

import {
  forwardRef,
  useEffect,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { cx } from '../../../internal/cx.js';
import { InfoGlyph, SuccessGlyph } from '../../../internal/icons.js';
import { Button } from '../../actions/button/index.js';
import { CopyButton } from '../../actions/copy-button/index.js';
import { CopyField } from '../../actions/copy-field/index.js';
import { RelativeTime, type DateInput } from '../../data-display/relative-time/index.js';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../../navigation/tabs/index.js';

/** One way to install: an HTML tag, a framework component, a plugin… */
export interface InstallSnippet {
  id: string;
  /** Tab label ("HTML", "React"). */
  label: string;
  /** The code the user copies. */
  code: string;
  /** One line on where the code goes. */
  description?: ReactNode;
}

/** A site the install is expected on, and when it was last seen there. */
export interface InstallTarget {
  /** Stable key. Default: `label`. */
  id?: string | undefined;
  /** Usually the hostname (`www.example.com`). */
  label: string;
  /** When the installed code last reported from this site. `null`/unset: not yet. */
  lastSeenAt?: DateInput | null | undefined;
}

/** `waiting` for the first report, `connected` once seen, `unavailable` when the product cannot check. */
export type InstallVerifyStatus = 'waiting' | 'connected' | 'unavailable';

export interface InstallVerifyMessages {
  installHeading: string;
  verifyHeading: string;
  /** Accessible name of the snippet tabs. */
  platforms: string;
  /** Names the snippet's copy box and button. */
  snippetLabel: (label: string) => string;
  copyInstructions: string;
  instructionsCopied: string;
  emailDeveloper: string;
  waitingTitle: string;
  waitingBody: (targets: string[]) => string;
  connectedTitle: (hosts: string[]) => string;
  connectedBody: string;
  unavailableTitle: string;
  unavailableBody: string;
  targetsLabel: string;
  seen: (time: ReactNode) => ReactNode;
  notSeen: string;
  openTest: string;
  newTab: string;
}

const DEFAULT_MESSAGES: InstallVerifyMessages = {
  installHeading: 'Install',
  verifyHeading: 'Verify',
  platforms: 'Platform',
  snippetLabel: (label) => `${label} snippet`,
  copyInstructions: 'Copy instructions for your developer',
  instructionsCopied: 'Instructions copied',
  emailDeveloper: 'Email them',
  waitingTitle: 'Waiting for your site…',
  waitingBody: (targets) =>
    `Add the snippet and open any page of ${targets.length ? targets.join(', ') : 'your site'}. This updates by itself.`,
  connectedTitle: (hosts) => (hosts.length ? `Connected on ${hosts.join(', ')}` : 'Connected'),
  connectedBody: 'The snippet loaded there. Try it out to see it working end to end.',
  unavailableTitle: 'Live verification isn’t available',
  unavailableBody: 'Open a page where you added the snippet and check that it appears.',
  targetsLabel: 'Websites',
  seen: (time) => <>seen {time}</>,
  notSeen: 'not seen yet',
  openTest: 'Open a test page',
  newTab: '(opens in a new tab)',
};

export interface InstallVerifyProps extends HTMLAttributes<HTMLDivElement> {
  /** The install options, one tab each. A single snippet shows without tabs. */
  snippets: readonly InstallSnippet[];
  /** Controlled selected snippet id. */
  snippet?: string | undefined;
  /** Initial snippet id when uncontrolled. Default: the first. */
  defaultSnippet?: string | undefined;
  /** Called when the user picks another snippet. */
  onSnippetChange?: ((id: string) => void) | undefined;
  /** Whether the installed code has reported in. */
  status: InstallVerifyStatus;
  /** Hosts it was seen on, for the connected message. Default: targets with `lastSeenAt`. */
  connectedOn?: readonly string[] | undefined;
  /** Sites the install is expected on, listed with when each was last seen. */
  targets?: readonly InstallTarget[] | undefined;
  /** Called once when `status` first becomes `connected` while shown. */
  onConnected?: (() => void) | undefined;
  /**
   * Shown above the snippets: the key or credential the snippet needs and
   * its state (creating, ready, failed with a retry).
   */
  credential?: ReactNode;
  /** Plain-text handoff for a developer. Shows a "Copy instructions" button. */
  developerInstructions?: string | undefined;
  /** A `mailto:` link with the same handoff. Shows an "Email them" link. */
  developerEmailHref?: string | undefined;
  /** A page where the user can try the install. Opens in a new tab. */
  testHref?: string | undefined;
  /** Disables the test page link, with `testHint` saying why. */
  testDisabled?: boolean | undefined;
  /** One line beside the test link. */
  testHint?: ReactNode;
  /** Show both halves (default), or only one for a wizard step. */
  part?: 'all' | 'install' | 'verify' | undefined;
  /** Level of the "Install" and "Verify" headings (shown when `part` is `all`). Default `h3`. */
  headingLevel?: 'h2' | 'h3' | 'h4' | undefined;
  /** Reference time for `lastSeenAt`, for identical server and client output. */
  now?: DateInput | undefined;
  /** Interface text, for translation. */
  messages?: Partial<InstallVerifyMessages> | undefined;
}

/**
 * Install a snippet on a website and watch for it to report in: code per
 * platform with copy, a handoff for a developer, then "Waiting for your
 * site…" until the product sees it and says "Connected on example.com".
 */
export const InstallVerify = forwardRef<HTMLDivElement, InstallVerifyProps>(function InstallVerify(
  {
    snippets,
    snippet,
    defaultSnippet,
    onSnippetChange,
    status,
    connectedOn,
    targets = [],
    onConnected,
    credential,
    developerInstructions,
    developerEmailHref,
    testHref,
    testDisabled = false,
    testHint,
    part = 'all',
    headingLevel = 'h3',
    now,
    messages,
    className,
    ...rest
  },
  ref,
) {
  const m = { ...DEFAULT_MESSAGES, ...messages };
  const H = headingLevel;
  const [own, setOwn] = useState(defaultSnippet ?? snippets[0]?.id ?? '');
  const current = snippet ?? own;
  const showInstall = part !== 'verify';
  const showVerify = part !== 'install';

  // Fire onConnected once per mount, on the first transition to connected.
  const announced = useRef(false);
  useEffect(() => {
    if (status === 'connected' && !announced.current) {
      announced.current = true;
      onConnected?.();
    }
  }, [status, onConnected]);

  const hosts = connectedOn ?? targets.filter((t) => t.lastSeenAt).map((t) => t.label);

  const snippetBody = (s: InstallSnippet) => (
    <>
      {s.description ? <p className="mn-install-verify__help">{s.description}</p> : null}
      <CopyField multiline rows={6} value={s.code} label={m.snippetLabel(s.label)} />
    </>
  );

  return (
    <div
      ref={ref}
      {...rest}
      className={cx('mn-install-verify', className)}
      data-status={status}
      data-part={part}
    >
      {showInstall ? (
        <div className="mn-install-verify__section" data-section="install">
          {part === 'all' ? <H className="mn-install-verify__heading">{m.installHeading}</H> : null}
          {credential ? <div className="mn-install-verify__credential">{credential}</div> : null}
          {snippets.length > 1 ? (
            <Tabs
              value={current}
              onValueChange={(id) => {
                if (snippet === undefined) setOwn(id);
                onSnippetChange?.(id);
              }}
            >
              <TabsList variant="pill" scrollable aria-label={m.platforms}>
                {snippets.map((s) => (
                  <TabsTrigger key={s.id} value={s.id}>
                    {s.label}
                  </TabsTrigger>
                ))}
              </TabsList>
              {snippets.map((s) => (
                <TabsContent key={s.id} value={s.id} className="mn-install-verify__snippet">
                  {snippetBody(s)}
                </TabsContent>
              ))}
            </Tabs>
          ) : snippets[0] ? (
            <div className="mn-install-verify__snippet">{snippetBody(snippets[0])}</div>
          ) : null}
          {developerInstructions || developerEmailHref ? (
            <div className="mn-install-verify__handoff">
              {developerInstructions ? (
                <CopyButton
                  value={developerInstructions}
                  label={m.copyInstructions}
                  copiedLabel={m.instructionsCopied}
                />
              ) : null}
              {developerEmailHref ? (
                <Button asChild variant="ghost">
                  <a href={developerEmailHref}>{m.emailDeveloper}</a>
                </Button>
              ) : null}
            </div>
          ) : null}
        </div>
      ) : null}

      {showVerify ? (
        <div className="mn-install-verify__section" data-section="verify">
          {part === 'all' ? <H className="mn-install-verify__heading">{m.verifyHeading}</H> : null}
          <div className="mn-install-verify__state" role="status" data-status={status}>
            <span className="mn-install-verify__indicator" aria-hidden>
              {status === 'connected' ? (
                <SuccessGlyph />
              ) : status === 'unavailable' ? (
                <InfoGlyph />
              ) : (
                <span className="mn-install-verify__pulse" />
              )}
            </span>
            <div className="mn-install-verify__state-text">
              <p className="mn-install-verify__state-title">
                {status === 'connected'
                  ? m.connectedTitle([...hosts])
                  : status === 'unavailable'
                    ? m.unavailableTitle
                    : m.waitingTitle}
              </p>
              <p className="mn-install-verify__help">
                {status === 'connected'
                  ? m.connectedBody
                  : status === 'unavailable'
                    ? m.unavailableBody
                    : m.waitingBody(targets.map((t) => t.label))}
              </p>
            </div>
          </div>
          {targets.length > 0 ? (
            <ul className="mn-install-verify__targets" aria-label={m.targetsLabel}>
              {targets.map((t) => (
                <li
                  key={t.id ?? t.label}
                  className="mn-install-verify__target"
                  data-seen={t.lastSeenAt ? '' : undefined}
                >
                  <span className="mn-install-verify__target-dot" aria-hidden />
                  <span className="mn-install-verify__target-label">{t.label}</span>
                  <span className="mn-install-verify__target-seen">
                    {t.lastSeenAt
                      ? m.seen(
                          <RelativeTime
                            date={t.lastSeenAt}
                            {...(now !== undefined ? { now } : {})}
                            tooltip={false}
                          />,
                        )
                      : m.notSeen}
                  </span>
                </li>
              ))}
            </ul>
          ) : null}
          {testHref ? (
            <div className="mn-install-verify__test">
              {testDisabled ? (
                <Button disabled>{m.openTest}</Button>
              ) : (
                <Button asChild>
                  <a href={testHref} target="_blank" rel="noopener noreferrer">
                    {m.openTest}
                    <span className="mn-visually-hidden"> {m.newTab}</span>
                  </a>
                </Button>
              )}
              {testHint ? <p className="mn-install-verify__help">{testHint}</p> : null}
            </div>
          ) : null}
        </div>
      ) : null}
    </div>
  );
});
