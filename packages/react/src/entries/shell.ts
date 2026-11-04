/**
 * `@rookcom/monarch-react/shell`: the Rook app shell (rail, identity bar,
 * Space and Workspace switchers, theme toggle). It encodes Rook platform
 * contracts (the shared `rook_rail` and `rook_theme` cookies), so it is kept
 * out of the root entry. See docs/architecture/PACKAGE-ARCHITECTURE.md.
 */
export {
  ThemeToggle,
  applyThemePreference,
  ROOK_THEME_COOKIE,
  ROOK_THEME_EVENT,
  type ThemeToggleProps,
  type ThemeToggleMessages,
  type ThemePreference,
} from '../components/interaction/theme-toggle/index.js';
export {
  RookRail,
  rookRailScript,
  ROOK_RAIL_COOKIE,
  ROOK_RAIL_EVENT,
  type RookRailProps,
  type RookRailItem,
  type RookRailSection,
  type RookRailBrand,
  type RookRailSupport,
  type RookRailNotifications,
  type RookRailAccount,
  type RookRailSlot,
  type RookRailSlotContext,
  type RookRailMessages,
  type RookRailScriptOptions,
} from '../patterns/navigation/rook-rail/index.js';
export {
  SpaceSwitcher,
  type SpaceSwitcherProps,
  type SpaceSwitcherMessages,
  type SpaceOption,
} from '../components/identity/space-switcher/index.js';
export {
  WorkspaceSwitcher,
  type WorkspaceSwitcherProps,
  type WorkspaceSwitcherMessages,
  type WorkspaceOption,
} from '../components/identity/workspace-switcher/index.js';
export {
  IdentityBar,
  type IdentityBarProps,
  type IdentityBarProduct,
  type IdentityBarMessages,
} from '../patterns/navigation/identity-bar/index.js';
export { AppShell, type AppShellProps } from '../patterns/layout/app-shell/index.js';
export type { ShellAccount } from '../internal/shell/account-menu.js';
export type { ShellCookieStore, ShellCookieOptions } from '../internal/shell/cookies.js';
export type { ShellLinkComponent } from '../internal/shell/link.js';
export {
  SupportPanel,
  useSupportChat,
  SUPPORT_MESSAGE_MAX,
  type SupportPanelProps,
  type SupportPanelMessages,
  type SupportContact,
  type SupportStatus,
  type SupportChatClient,
  type SupportChatMessage,
  type SupportChatSnapshot,
  type SupportChatEvent,
  type SupportChatAvailability,
  type SupportMessageAuthor,
  type SupportChatEntry,
  type SupportChatPhase,
  type SupportChatState,
  type SupportChatController,
} from '../patterns/overlays/support-panel/index.js';
export {
  ChatDock,
  createChatDockBridge,
  encodeChatEmbedMessage,
  decodeChatEmbedMessage,
  cleanOpenDetail,
  chatPageContext,
  CHAT_EMBED_PROTOCOL,
  CHAT_EMBED_VERSION,
  DEFAULT_CHAT_SANDBOX,
  DEFAULT_CHAT_ALLOW,
  type ChatDockProps,
  type ChatDockMessages,
  type ChatDockController,
  type ChatDockLayout,
  type ChatEmbedStatus,
  type ChatEmbedMessage,
  type ChatEmbedHostMessage,
  type ChatHostContext,
  type ChatOpenDetail,
  type ChatShareTarget,
  type ChatCreateRequest,
  type ChatDockBridge,
  type ChatDockBridgeOptions,
  type ChatFrameWindow,
} from '../patterns/collaboration/chat-dock/index.js';
export {
  EmbeddedAppFrame,
  useEmbedReady,
  encodeAppEmbedMessage,
  decodeAppEmbedChildMessage,
  decodeAppEmbedHostMessage,
  APP_EMBED_PROTOCOL,
  APP_EMBED_VERSION,
  MAX_EMBED_HEIGHT,
  DEFAULT_EMBED_SANDBOX,
  type EmbeddedAppFrameProps,
  type EmbeddedAppFrameMessages,
  type EmbeddedAppFrameStatus,
  type EmbedSandboxToken,
  type EmbedFocusDirection,
  type AppEmbedChildMessage,
  type AppEmbedHostMessage,
  type UseEmbedReadyOptions,
  type UseEmbedReadyResult,
} from '../patterns/technical/embedded-app-frame/index.js';
export {
  NoAppAccess,
  type NoAppAccessProps,
  type NoAppAccessMessages,
  type AccessRequestState,
} from '../components/identity/no-app-access/index.js';
export {
  LinkedDocsPanel,
  parseLinkUrl,
  type LinkedDocsPanelProps,
  type LinkedDocsPanelMessages,
  type LinkedDocItem,
  type LinkedDocKind,
} from '../components/collaboration/linked-docs-panel/index.js';
