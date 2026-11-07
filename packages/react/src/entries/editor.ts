/**
 * `@rookcom/monarch-react/editor`: the Tiptap rich-text editor and the
 * message composer built on it. The only entry that imports Tiptap, so apps
 * that never import it never need the optional `@tiptap/*` peer dependencies
 * (docs/architecture/PACKAGE-ARCHITECTURE.md).
 */
export * from '../components/collaboration/rich-text-editor/index.js';
export * from '../components/collaboration/message-composer/index.js';
export {
  diffVersionLines,
  versionDiffStats,
  collapseVersionDiff,
  DEFAULT_VERSION_HISTORY_PANEL_MESSAGES,
  VersionHistoryPanel,
  type VersionDiffLine,
  type VersionDiffGap,
  type VersionHistoryItem,
  type VersionHistoryPanelMessages,
  type VersionHistoryPanelProps,
} from '../components/files/version-history-panel/index.js';
export {
  DEFAULT_DOCUMENT_RELATION_TYPES,
  DEFAULT_DOCUMENT_RELATIONS_MESSAGES,
  DocumentRelations,
  type DocumentRelationType,
  type DocumentRelationTarget,
  type DocumentRelation,
  type DocumentRelationsMessages,
  type DocumentRelationsProps,
} from '../components/files/document-relations/index.js';
export {
  DEFAULT_PAGE_TREE_MESSAGES,
  PageTree,
  type PageTreePage,
  type PageTreeGroup,
  type PageTreeMove,
  type PageTreeMessages,
  type PageTreeProps,
} from '../components/files/page-tree/index.js';
