/**
 * `@rookcom/monarch-react/editor`: the Tiptap rich-text editor and the
 * message composer built on it. The only entry that imports Tiptap, so apps
 * that never import it never need the optional `@tiptap/*` peer dependencies
 * (docs/architecture/PACKAGE-ARCHITECTURE.md).
 */
export * from '../components/collaboration/rich-text-editor/index.js';
export * from '../components/collaboration/message-composer/index.js';
