'use client';

import {
  forwardRef,
  useEffect,
  useMemo,
  useRef,
  useState,
  type HTMLAttributes,
  type ReactNode,
} from 'react';
import { Button } from '../../actions/button/index.js';
import { ErrorPage } from '../../feedback/error-page/index.js';
import { cx } from '../../../internal/cx.js';
import { ShellLink, type ShellLinkComponent } from '../../../internal/shell/link.js';
import { LockGlyph } from '../../../internal/shell/panel-icons.js';

/** Where an access request stands. */
export type AccessRequestState = 'idle' | 'pending' | 'sent' | 'error';

export interface NoAppAccessMessages {
  title: (appName: string) => string;
  description: (appName: string, workspaceName?: string) => string;
  requestAccess: string;
  requested: string;
  /** Announced once the request went out. */
  requestSent: (appName: string) => string;
  requestFailed: string;
  contactAdmin: string;
  /** Default "Back to workspace". */
  home: string;
  switchWorkspace: string;
}

const DEFAULT_MESSAGES: NoAppAccessMessages = {
  title: (app) => `You don’t have access to ${app}`,
  description: (app, ws) =>
    ws
      ? `Ask an admin of ${ws} to give you access to ${app}.`
      : `Ask a workspace admin to give you access to ${app}.`,
  requestAccess: 'Request access',
  requested: 'Access requested',
  requestSent: (app) => `Request sent. You can open ${app} once an admin approves it.`,
  requestFailed: 'The request didn’t go through. Try again.',
  contactAdmin: 'Contact an admin',
  home: 'Back to workspace',
  switchWorkspace: 'Switch workspace',
};

export interface NoAppAccessProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** The app the person can't open, e.g. "Rook Tickets". */
  appName: string;
  /** The current workspace, named in the explanation. */
  workspaceName?: string | undefined;
  /** Replaces the default heading. */
  title?: ReactNode;
  /** Replaces the default explanation. */
  description?: ReactNode;
  /**
   * Shows "Request access". A returned promise drives the pending, sent and
   * failed states (unless `requestState` is controlled).
   */
  onRequestAccess?: (() => unknown) | undefined;
  /** Request state (controlled), e.g. `sent` when a request is already open. */
  requestState?: AccessRequestState | undefined;
  /** "Contact an admin": a mailto: or a page that lists the admins. */
  contactHref?: string | undefined;
  /** "Back to workspace": the workspace home in Rook ID or another app. */
  homeHref?: string | undefined;
  /** "Switch workspace" as an action… */
  onSwitchWorkspace?: (() => void) | undefined;
  /** …or a link. */
  switchWorkspaceHref?: string | undefined;
  /** Or your own control (e.g. a WorkspaceSwitcher) in place of the switch button. */
  workspaceSwitcher?: ReactNode;
  /** Router link for the internal links. Default `<a>`. */
  linkAs?: ShellLinkComponent | undefined;
  /** Fill the viewport (no app shell). Default false: sits in the shell's content area. */
  fullScreen?: boolean | undefined;
  /** Heading level. Default `h1` (the screen replaces the page). */
  headingLevel?: 'h1' | 'h2' | undefined;
  /** Focus the heading on mount, after a client-side navigation. Default false. */
  focusOnMount?: boolean | undefined;
  /** Interface text, for translation. */
  messages?: Partial<NoAppAccessMessages> | undefined;
}

function isThenable(v: unknown): v is PromiseLike<unknown> {
  return !!v && typeof (v as { then?: unknown }).then === 'function';
}

/**
 * The "you don't have access to this app" screen: the person is a member of
 * the workspace but has no grant for this app. It explains who can fix it and
 * offers request access, contact an admin, back to the workspace and switch
 * workspace. Built on ErrorPage (`forbidden`). The grant check stays in the app.
 */
export const NoAppAccess = forwardRef<HTMLElement, NoAppAccessProps>(function NoAppAccess(
  {
    appName,
    workspaceName,
    title,
    description,
    onRequestAccess,
    requestState: requestStateProp,
    contactHref,
    homeHref,
    onSwitchWorkspace,
    switchWorkspaceHref,
    workspaceSwitcher,
    linkAs,
    fullScreen = false,
    headingLevel = 'h1',
    focusOnMount = false,
    messages: messagesProp,
    className,
    ...rest
  },
  ref,
) {
  const messages = useMemo(() => ({ ...DEFAULT_MESSAGES, ...messagesProp }), [messagesProp]);
  const [innerState, setInnerState] = useState<AccessRequestState>('idle');
  const state = requestStateProp ?? innerState;
  const alive = useRef(true);
  useEffect(
    () => () => {
      alive.current = false;
    },
    [],
  );

  const request = () => {
    if (!onRequestAccess || state === 'pending' || state === 'sent') return;
    let result: unknown;
    try {
      result = onRequestAccess();
    } catch {
      setInnerState('error');
      return;
    }
    if (!isThenable(result)) {
      setInnerState('sent');
      return;
    }
    setInnerState('pending');
    result.then(
      () => alive.current && setInnerState('sent'),
      () => alive.current && setInnerState('error'),
    );
  };

  const actions = (
    <>
      {onRequestAccess ? (
        <Button
          variant="primary"
          loading={state === 'pending'}
          disabled={state === 'sent'}
          onClick={request}
        >
          {state === 'sent' ? messages.requested : messages.requestAccess}
        </Button>
      ) : null}
      {homeHref ? (
        <Button asChild variant={onRequestAccess ? 'secondary' : 'primary'}>
          <ShellLink as={linkAs} href={homeHref}>
            {messages.home}
          </ShellLink>
        </Button>
      ) : null}
      {workspaceSwitcher ??
        (switchWorkspaceHref ? (
          <Button asChild variant="ghost">
            <ShellLink as={linkAs} href={switchWorkspaceHref}>
              {messages.switchWorkspace}
            </ShellLink>
          </Button>
        ) : onSwitchWorkspace ? (
          <Button variant="ghost" onClick={onSwitchWorkspace}>
            {messages.switchWorkspace}
          </Button>
        ) : null)}
    </>
  );
  const hasActions = Boolean(
    onRequestAccess || homeHref || workspaceSwitcher || switchWorkspaceHref || onSwitchWorkspace,
  );

  return (
    <ErrorPage
      ref={ref}
      {...rest}
      kind="forbidden"
      numeral={false}
      icon={<LockGlyph />}
      fullScreen={fullScreen}
      headingLevel={headingLevel}
      focusOnMount={focusOnMount}
      className={cx('mn-no-app-access', className)}
      data-request={state}
      title={title ?? messages.title(appName)}
      description={description ?? messages.description(appName, workspaceName)}
      actions={hasActions ? actions : undefined}
      support={
        contactHref ? (
          <a className="mn-no-app-access__contact" href={contactHref}>
            {messages.contactAdmin}
          </a>
        ) : undefined
      }
    >
      <p className="mn-no-app-access__status" role="status">
        {state === 'sent' ? messages.requestSent(appName) : null}
      </p>
      {state === 'error' ? (
        <p className="mn-no-app-access__error" role="alert">
          {messages.requestFailed}
        </p>
      ) : null}
    </ErrorPage>
  );
});
