/**
 * `@rookcom/monarch-react/ai`: interfaces for AI features. A vendor-neutral
 * message model (`AiMessage`, `AiPart`), the `useAiConversation` transport
 * with adapters, and the components that render conversations, streaming
 * answers, reasoning, tool calls and sources. It depends on no model-vendor
 * SDK. Kept out of the root entry so apps without AI never load it.
 * See docs/adr/0013-ai-components.md.
 */
export {
  applyStreamEvent,
  useAiConversation,
  messageText,
  isStreaming,
  readServerSentEvents,
  mapRookIntelligenceEvent,
  rookIntelligenceTransport,
  mockTransport,
  textDeltas,
  type AiConversationStatus,
  type AiConversationError,
  type SendOptions,
  type UseAiConversationOptions,
  type UseAiConversationResult,
  type AiRole,
  type AiAgentRef,
  type AiToolCallState,
  type AiTextPart,
  type AiReasoningPart,
  type AiToolCallPart,
  type AiCitationPart,
  type AiSourcePart,
  type AiFilePart,
  type AiArtifactPart,
  type AiEditProposalPart,
  type AiPlanStepState,
  type AiPlanPart,
  type AiUiPart,
  type AiErrorPart,
  type AiPart,
  type AiPartType,
  type AiUsage,
  type AiMessage,
  type AiStreamEvent,
  type AiTrigger,
  type AiTransportRequest,
  type AiToolResponse,
  type AiTransport,
  type RookIntelligenceTransportOptions,
  type MockTransportOptions,
} from '../components/intelligence/ai-conversation/index.js';
export {
  AI_SCROLL_ANCHOR_THRESHOLD,
  useAiScrollAnchor,
  AiScrollAnchor,
  type UseAiScrollAnchorOptions,
  type UseAiScrollAnchorResult,
  type AiScrollAnchorRenderState,
  type AiScrollAnchorProps,
} from '../components/intelligence/ai-scroll-anchor/index.js';
export {
  AiThread,
  type AiThreadLabels,
  type AiThreadRenderContext,
  type AiThreadProps,
} from '../components/intelligence/ai-thread/index.js';
export {
  AiMessageView,
  type AiMessageViewMessages,
  type AiMessageViewProps,
} from '../components/intelligence/ai-message/index.js';
export {
  AiMessageActions,
  type AiFeedback,
  type AiMessageActionsMessages,
  type AiMessageActionsProps,
} from '../components/intelligence/ai-message-actions/index.js';
export {
  findTrigger,
  usePromptDraft,
  AiPromptInput,
  type AiPromptAttachment,
  type AiPromptAttachSource,
  type AiPromptAttachHandler,
  type AiPromptSubmitDetails,
  type AiPromptTriggerApi,
  type AiPromptTrigger,
  type AiPromptTriggerState,
  type AiPromptLimitState,
  type AiPromptInputMessages,
  type AiPromptInputProps,
  type AiPromptTriggerMatch,
} from '../components/intelligence/ai-prompt-input/index.js';
export {
  AI_STREAMING_TEXT_MESSAGES,
  completeStreamingMarkdown,
  AiStreamingText,
  type AiStreamingTextMessages,
  type AiStreamingTextProps,
} from '../components/intelligence/ai-streaming-text/index.js';
export {
  AI_MARKDOWN_MESSAGES,
  AiMarkdown,
  parseMarkdownBlocks,
  parseMarkdownIncremental,
  parseInline,
  inlineText,
  blockText,
  type AiMarkdownMessages,
  type AiMarkdownProps,
  type MarkdownBlock,
  type MarkdownInline,
  type MarkdownListItem,
  type MarkdownAlign,
  type MarkdownParseState,
  type BlockTextOptions,
} from '../components/intelligence/ai-markdown/index.js';
export {
  codeLanguageName,
  AI_CODE_BLOCK_MESSAGES,
  AiCodeBlock,
  tokenizeCode,
  resolveCodeLanguage,
  type AiCodeBlockMessages,
  type AiCodeBlockProps,
  type CodeToken,
  type CodeTokenType,
} from '../components/intelligence/ai-code-block/index.js';
export {
  AiThinkingIndicator,
  type AiThinkingIndicatorMessages,
  type AiThinkingIndicatorVariant,
  type AiThinkingIndicatorProps,
} from '../components/intelligence/ai-thinking-indicator/index.js';
export {
  AiTypingDots,
  type AiTypingDotsSize,
  type AiTypingDotsProps,
} from '../components/intelligence/ai-typing-dots/index.js';
export {
  AiStopGlyph,
  AiStopButton,
  type AiStopButtonMessages,
  type AiStopButtonProps,
} from '../components/intelligence/ai-stop-button/index.js';
export {
  aiErrorKind,
  AiErrorMessage,
  type AiErrorKind,
  type AiErrorMessageMessages,
  type AiErrorMessageProps,
} from '../components/intelligence/ai-error-message/index.js';
