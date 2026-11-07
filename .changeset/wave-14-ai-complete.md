---
'@rookcom/monarch-react': minor
---

Wave 14 completes the AI catalog in `@rookcom/monarch-react/ai` (85 components and templates in all):

- Conversation: `AiMessageBranch`, `AiUserMessageEdit`, `AiWelcome`, `AiStarterPrompts`, `AiFollowUpSuggestions`, `AiConversationList`, `AiConversationHeader`, `AiContextDivider`.
- Input: `AiPromptToolbar`, `useAiSlashCommands`, `useAiMentionContext`, `AiContextChips`, `AiAttachmentTray`, `AiModeSwitcher`, `AiModelPicker`, `AiTonePicker`, `AiPromptTemplatePicker`, `AiVoiceInput`, `AiInlinePrompt`.
- Rendering: `AiResponseSkeleton`, `AiStructuredAnswer`, `AiTableAnswer` (with `toCsv`).
- Agents: `AiAgentAvatar`, `AiAgentCard`, `AiAgentPicker`, `AiHandoff`, `AiMultiAgentRoster`, `AiTaskProgress`, `AiAgentRunTimeline`, `AiPermissionScope`, `AiActionLog`.
- Usage, safety, memory: `AiUsageMeter`, `AiSafetyNotice`, `AiMemoryChip`, `AiMemoryPanel`, `AiContextWindowMeter`, `AiKnowledgeScope`, `AiRelatedEntities`.
- Artifacts: `AiArtifactCard`, `AiArtifactCanvas` (HTML only in an empty-sandbox iframe), `AiArtifactVersions`, `AiGenerativeUi` (allow-listed registry, sanitised props), `AiImageGeneration`.
- Templates: `AiChatPage`, `AiCopilotLayout`, `AiAgentBuilder`, `AiEvalReview`; plus `AiOnboardingTour` and `AiSettingsSection`.
