package com.rookhq.monarch.components.intelligence.aiconversation

/**
 * Reads and writes the message model as the web's JSON: camelCase fields,
 * kebab-case enumeration values, a `type` field per part, and optional
 * fields left out when they are empty.
 */
internal object MonarchAiMessageCodec {
    private val KebabBoundary = Regex("(?<=[a-z0-9])(?=[A-Z])")

    /** The web's name of an enumeration value: `PendingApproval` is `pending-approval`. */
    fun wire(value: Enum<*>): String = value.name.replace(KebabBoundary, "-").lowercase()

    /** The enumeration value of a web name. */
    inline fun <reified E : Enum<E>> enumOf(text: String): E =
        requireNotNull(enumValues<E>().firstOrNull { wire(it) == text }) {
            "Unknown ${E::class.simpleName} \"$text\""
        }

    fun encode(message: MonarchAiConversationMessage): MonarchAiJSON = obj {
        put("id", message.id)
        put("role", wire(message.role))
        this["parts"] = MonarchAiJSON.Array(message.parts.map(::encodePart))
        message.agent?.let { this["agent"] = encodeAgent(it) }
        put("createdAt", message.createdAt)
        put("model", message.model)
        message.usage?.let { this["usage"] = encodeUsage(it) }
        message.feedback?.let { put("feedback", wire(it)) }
        message.branch?.let { branch ->
            this["branch"] = obj {
                put("index", branch.index)
                put("count", branch.count)
            }
        }
        message.finishReason?.let { put("finishReason", wire(it)) }
        put("runId", message.runId)
        if (message.queued) put("queued", true)
    }

    fun decode(json: MonarchAiJSON): MonarchAiConversationMessage {
        val fields = Fields(json, "message")
        return MonarchAiConversationMessage(
            id = fields.string("id"),
            role = enumOf(fields.string("role")),
            parts = fields.array("parts").map(::decodePart),
            createdAt = fields.string("createdAt"),
            agent = fields.optional("agent")?.let(::decodeAgent),
            model = fields.optionalString("model"),
            usage = fields.optional("usage")?.let(::decodeUsage),
            feedback = fields.optionalString("feedback")?.let { enumOf<MonarchAiMessageFeedback>(it) },
            branch = fields.optional("branch")?.let {
                val branch = Fields(it, "branch")
                MonarchAiBranchPosition(branch.int("index"), branch.int("count"))
            },
            finishReason = fields.optionalString("finishReason")?.let { enumOf<MonarchAiFinishReason>(it) },
            runId = fields.optionalString("runId"),
            queued = fields.optionalBoolean("queued") ?: false,
        )
    }

    fun encodePart(part: MonarchAiPart): MonarchAiJSON = obj {
        put("type", part.type)
        when (part) {
            is MonarchAiTextPart, is MonarchAiReasoningPart, is MonarchAiToolCallPart, is MonarchAiCitationPart,
            is MonarchAiSourcePart, is MonarchAiFilePart,
            -> encodeContent(part, this)
            else -> encodeOther(part, this)
        }
    }

    /** Text, reasoning, tool calls, citations, sources and files. */
    private fun encodeContent(part: MonarchAiPart, out: MutableMap<String, MonarchAiJSON>) = with(out) {
        when (part) {
            is MonarchAiTextPart -> {
                put("text", part.text)
                put("state", wire(part.state))
            }
            is MonarchAiReasoningPart -> {
                put("text", part.text)
                put("state", wire(part.state))
                put("durationMs", part.durationMs)
                if (part.redacted) put("redacted", true)
            }
            is MonarchAiToolCallPart -> encodeToolCall(part, this)
            is MonarchAiCitationPart -> {
                put("id", part.id)
                put("sourceId", part.sourceId)
                put("quote", part.quote)
            }
            is MonarchAiSourcePart -> {
                put("id", part.id)
                put("title", part.title)
                put("url", part.url)
                put("kind", wire(part.kind))
                put("product", part.product)
                put("favicon", part.favicon)
            }
            is MonarchAiFilePart -> {
                put("name", part.name)
                put("mediaType", part.mediaType)
                put("url", part.url)
                put("size", part.size)
                put("fileId", part.fileId)
            }
            else -> Unit
        }
    }

    /** Artifacts, edit proposals, plans, generative UI, memories and errors. */
    private fun encodeOther(part: MonarchAiPart, out: MutableMap<String, MonarchAiJSON>) = with(out) {
        when (part) {
            is MonarchAiArtifactPart -> {
                put("id", part.id)
                put("kind", wire(part.kind))
                put("title", part.title)
                put("version", part.version)
            }
            is MonarchAiEditProposalPart -> {
                put("id", part.id)
                put("target", part.target)
                this["diff"] = part.diff
                put("state", wire(part.state))
            }
            is MonarchAiPlanPart -> this["steps"] = MonarchAiJSON.Array(
                part.steps.map { step ->
                    obj {
                        put("id", step.id)
                        put("label", step.label)
                        put("state", wire(step.state))
                    }
                },
            )
            is MonarchAiUiPart -> {
                put("component", part.component)
                this["props"] = part.props
                put("id", part.id)
                part.state?.let { put("state", wire(it)) }
                put("toolCallId", part.toolCallId)
            }
            is MonarchAiMemoryPart -> {
                put("id", part.id)
                put("action", wire(part.action))
                put("text", part.text)
                put("scope", part.scope)
            }
            is MonarchAiErrorPart -> {
                put("code", part.code)
                put("message", part.message)
                put("retryable", part.retryable)
                put("retryAfterMs", part.retryAfterMs)
                if (part.resumable) put("resumable", true)
            }
            else -> Unit
        }
    }

    private fun encodeToolCall(part: MonarchAiToolCallPart, out: MutableMap<String, MonarchAiJSON>) = with(out) {
        put("id", part.id)
        put("name", part.name)
        this["input"] = part.input
        put("state", wire(part.state))
        part.output?.let { this["output"] = it }
        put("error", part.error)
        put("startedAt", part.startedAt)
        put("endedAt", part.endedAt)
        put("inputText", part.inputText)
        part.risk?.let { put("risk", wire(it)) }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod")
    fun decodePart(json: MonarchAiJSON): MonarchAiPart {
        val f = Fields(json, "part")
        return when (val type = f.string("type")) {
            "text" -> MonarchAiTextPart(f.string("text"), enumOf(f.string("state")))
            "reasoning" -> MonarchAiReasoningPart(
                text = f.string("text"),
                state = enumOf(f.string("state")),
                durationMs = f.optionalLong("durationMs"),
                redacted = f.optionalBoolean("redacted") ?: false,
            )
            "tool-call" -> MonarchAiToolCallPart(
                id = f.string("id"),
                name = f.string("name"),
                input = f.optional("input") ?: MonarchAiJSON.Null,
                state = enumOf(f.string("state")),
                output = f.optional("output"),
                error = f.optionalString("error"),
                startedAt = f.optionalString("startedAt"),
                endedAt = f.optionalString("endedAt"),
                inputText = f.optionalString("inputText"),
                risk = f.optionalString("risk")?.let { enumOf<MonarchAiToolCallRisk>(it) },
            )
            "citation" -> MonarchAiCitationPart(f.string("id"), f.string("sourceId"), f.optionalString("quote"))
            "source" -> MonarchAiSourcePart(
                id = f.string("id"),
                title = f.string("title"),
                url = f.optionalString("url"),
                kind = enumOf(f.string("kind")),
                product = f.optionalString("product"),
                favicon = f.optionalString("favicon"),
            )
            "file" -> MonarchAiFilePart(
                name = f.string("name"),
                mediaType = f.string("mediaType"),
                url = f.optionalString("url"),
                size = f.optionalLong("size"),
                fileId = f.optionalString("fileId"),
            )
            "artifact" ->
                MonarchAiArtifactPart(f.string("id"), enumOf(f.string("kind")), f.string("title"), f.int("version"))
            "edit-proposal" -> MonarchAiEditProposalPart(
                id = f.string("id"),
                target = f.string("target"),
                diff = f.optional("diff") ?: MonarchAiJSON.Null,
                state = enumOf(f.string("state")),
            )
            "plan" -> MonarchAiPlanPart(
                f.array("steps").map {
                    val step = Fields(it, "plan step")
                    MonarchAiPlanStep(step.string("id"), step.string("label"), enumOf(step.string("state")))
                },
            )
            "ui" -> MonarchAiUiPart(
                component = f.string("component"),
                props = f.optional("props") ?: MonarchAiJSON.Null,
                id = f.optionalString("id"),
                state = f.optionalString("state")?.let { enumOf<MonarchAiPartState>(it) },
                toolCallId = f.optionalString("toolCallId"),
            )
            "memory" -> MonarchAiMemoryPart(
                id = f.string("id"),
                action = enumOf(f.string("action")),
                text = f.string("text"),
                scope = f.optionalString("scope"),
            )
            "error" -> MonarchAiErrorPart(
                code = f.string("code"),
                message = f.string("message"),
                retryable = f.boolean("retryable"),
                retryAfterMs = f.optionalLong("retryAfterMs"),
                resumable = f.optionalBoolean("resumable") ?: false,
            )
            else -> throw IllegalArgumentException("Unknown part type \"$type\"")
        }
    }

    private fun encodeAgent(agent: MonarchAiAgentRef): MonarchAiJSON = obj {
        put("id", agent.id)
        put("name", agent.name)
        put("codename", agent.codename)
        put("role", agent.role)
        put("avatar", agent.avatar)
    }

    private fun decodeAgent(json: MonarchAiJSON): MonarchAiAgentRef {
        val f = Fields(json, "agent")
        return MonarchAiAgentRef(
            name = f.string("name"),
            id = f.optionalString("id"),
            codename = f.optionalString("codename"),
            role = f.optionalString("role"),
            avatar = f.optionalString("avatar"),
        )
    }

    private fun encodeUsage(usage: MonarchAiUsage): MonarchAiJSON = obj {
        put("inputTokens", usage.inputTokens)
        put("outputTokens", usage.outputTokens)
        put("costMicros", usage.costMicros)
        put("cacheReadTokens", usage.cacheReadTokens)
        put("cacheWriteTokens", usage.cacheWriteTokens)
        put("reasoningTokens", usage.reasoningTokens)
    }

    private fun decodeUsage(json: MonarchAiJSON): MonarchAiUsage {
        val f = Fields(json, "usage")
        return MonarchAiUsage(
            inputTokens = f.long("inputTokens"),
            outputTokens = f.long("outputTokens"),
            costMicros = f.optionalLong("costMicros"),
            cacheReadTokens = f.optionalLong("cacheReadTokens"),
            cacheWriteTokens = f.optionalLong("cacheWriteTokens"),
            reasoningTokens = f.optionalLong("reasoningTokens"),
        )
    }

    private fun obj(build: MutableMap<String, MonarchAiJSON>.() -> Unit): MonarchAiJSON =
        MonarchAiJSON.Object(LinkedHashMap<String, MonarchAiJSON>().apply(build))

    /** Puts a string, leaving it out when it is `null`. */
    private fun MutableMap<String, MonarchAiJSON>.put(key: String, value: String?) {
        if (value != null) this[key] = MonarchAiJSON.Text(value)
    }

    private fun MutableMap<String, MonarchAiJSON>.put(key: String, value: Boolean) {
        this[key] = MonarchAiJSON.Bool(value)
    }

    private fun MutableMap<String, MonarchAiJSON>.put(key: String, value: Long?) {
        if (value != null) this[key] = MonarchAiJSON.Number(value.toDouble())
    }

    private fun MutableMap<String, MonarchAiJSON>.put(key: String, value: Int) {
        this[key] = MonarchAiJSON.Number(value.toDouble())
    }

    /** The members of a JSON object, read with clear errors. */
    private class Fields(json: MonarchAiJSON, private val what: String) {
        private val members =
            requireNotNull((json as? MonarchAiJSON.Object)?.members) { "A $what must be an object" }

        fun optional(key: String): MonarchAiJSON? = members[key]?.takeIf { it != MonarchAiJSON.Null }

        private fun required(key: String): MonarchAiJSON = requireNotNull(optional(key)) { "A $what needs \"$key\"" }

        fun string(key: String): String =
            requireNotNull(required(key).stringValue) { "\"$key\" of a $what must be a string" }

        fun optionalString(key: String): String? = optional(key)?.let {
            requireNotNull(it.stringValue) { "\"$key\" of a $what must be a string" }
        }

        fun boolean(key: String): Boolean = requireNotNull(optionalBoolean(key)) { "A $what needs \"$key\"" }

        fun optionalBoolean(key: String): Boolean? = optional(key)?.let {
            requireNotNull((it as? MonarchAiJSON.Bool)?.value) { "\"$key\" of a $what must be a boolean" }
        }

        fun long(key: String): Long = requireNotNull(optionalLong(key)) { "A $what needs \"$key\"" }

        fun int(key: String): Int = long(key).toInt()

        fun optionalLong(key: String): Long? = optional(key)?.let {
            requireNotNull((it as? MonarchAiJSON.Number)?.value?.toLong()) { "\"$key\" of a $what must be a number" }
        }

        fun array(key: String): List<MonarchAiJSON> = requireNotNull((required(key) as? MonarchAiJSON.Array)?.values) {
            "\"$key\" of a $what must be an array"
        }
    }
}
