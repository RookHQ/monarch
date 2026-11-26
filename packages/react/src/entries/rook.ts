/**
 * `@rookcom/monarch-react/rook`: every export that encodes a Rook platform
 * contract, in one place: the Rook rail and bar, the shared `rook_rail` and
 * `rook_theme` cookies, sign-in with Rook ID and the Rook Intelligence
 * transport. Teams outside Rook do not need this entry; see
 * docs/guides/rook-specific-exports.md for the general component to use
 * instead of each one.
 *
 * The same exports are still available from `/shell`, `/patterns` and `/ai`,
 * where they are deprecated; those paths are removed in 0.15.0.
 */
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
  RookBar,
  RookBarPanelProvider,
  useRookBarPanel,
  RookBarAppCatalog,
  RookBarLinkList,
  RookBarFeaturePanel,
  RookBarStoriesPanel,
  type RookBarLinkItem,
  type RookBarMenuItem,
  type RookBarItem,
  type RookBarAccount,
  type RookBarMessages,
  type RookBarProps,
  type RookBarPanelContext,
  type RookBarLink,
  type RookBarApp,
  type RookBarAppCategory,
  type RookBarAppCatalogMessages,
  type RookBarAppCatalogProps,
  type RookBarLinkListProps,
  type RookBarFeature,
  type RookBarFeaturePanelProps,
  type RookBarStory,
  type RookBarStoriesPanelMessages,
  type RookBarStoriesPanelProps,
} from '../patterns/layout/rook-bar/index.js';
export {
  ROOK_THEME_COOKIE,
  ROOK_THEME_EVENT,
} from '../components/interaction/theme-toggle/index.js';
export {
  SignInWithRookButton,
  type SignInWithRookButtonVariant,
  type SignInWithRookButtonSize,
  type SignInWithRookButtonAsButtonProps,
  type SignInWithRookButtonAsLinkProps,
  type SignInWithRookButtonProps,
} from '../components/identity/sign-in-with-rook-button/index.js';
export {
  rookIntelligenceTransport,
  mapRookIntelligenceEvent,
  type RookIntelligenceTransportOptions,
  type RookIntelligenceMapOptions,
} from '../components/intelligence/ai-conversation/index.js';
