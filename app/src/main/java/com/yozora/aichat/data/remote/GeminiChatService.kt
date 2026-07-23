package com.yozora.aichat.data.remote

import android.graphics.Bitmap
import android.util.Base64
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.yozora.aichat.data.SkillRepository
import com.yozora.aichat.data.db.MessageEntity
import com.yozora.aichat.data.db.PersonaEntity
import com.yozora.aichat.ui.chat.ApiVendor
import com.yozora.aichat.ui.chat.SafetyLevel
import com.yozora.aichat.ui.chat.TokenEstimator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class GeminiChatReply(
    val text: String,
    val promptTokenCount: Int,
    val responseTokenCount: Int,
    val totalTokenCount: Int
)

data class GeminiDirectorReply(
    val speakerAliases: List<String>,
    val promptTokenCount: Int,
    val responseTokenCount: Int,
    val totalTokenCount: Int
) {
    fun toChatReply(): GeminiChatReply = GeminiChatReply(
        text = "",
        promptTokenCount = promptTokenCount,
        responseTokenCount = responseTokenCount,
        totalTokenCount = totalTokenCount
    )
}

data class GeminiToolCall(
    val name: String,
    val args: JSONObject
)

data class GeminiToolPlan(
    val text: String,
    val functionCall: GeminiToolCall?,
    val promptTokenCount: Int,
    val responseTokenCount: Int,
    val totalTokenCount: Int
) {
    fun toReply(): GeminiChatReply {
        return GeminiChatReply(
            text = text,
            promptTokenCount = promptTokenCount,
            responseTokenCount = responseTokenCount,
            totalTokenCount = totalTokenCount
        )
    }
}

data class PersonaAutoFillResult(
    val storyLore: String,
    val beginnerRole: String,
    val beginnerStyle: String,
    val beginnerLimits: String
)

class ApiHttpException(
    val statusCode: Int,
    val apiMessage: String,
    val responseBody: String
) : IOException("HTTP $statusCode: $apiMessage")

private const val FALLBACK_MASTER_PROMPT = """
You are a customizable AI companion. Follow the persona instruction prompt for the active session, keep replies immersive and useful, and ask for clarification when the user request is unclear.
"""

private const val FLASH_LITE_MODEL = "gemini-3.1-flash-lite"
private const val FLASH_LITE_THINKING_BUDGET = 8192
private const val SUMMARIZER_MODEL = FLASH_LITE_MODEL

private suspend fun <T> resultCatchingNonCancellation(
    block: suspend () -> T
): Result<T> {
    return try {
        Result.success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Throwable) {
        Result.failure(error)
    }
}

private const val SUMMARIZER_SYSTEM_PROMPT = """
You are a conversation archiver. Compress the following chat log into a dense, structured summary under 250 words. Format it as:
[USER FACTS]: Key facts, preferences, or details about the user revealed in this log.
[RELATIONSHIP STATE]: Current dynamic, mood, and established character voice between user and AI.
[PLOT & SITUATION]: Chronological events and the exact narrative/physical situation at the end of this log.
Rules: Short phrases only. No filler. Preserve roleplay continuity and character voice.
"""

class GeminiChatService(
    private val skillRepository: SkillRepository? = null
) {
    private val doroAutoRouter = DoroAutoRouter()

    fun supportsImageInput(vendor: ApiVendor, model: String): Boolean {
        val normalized = model.lowercase(Locale.US)
        return when (vendor) {
            ApiVendor.Google,
            ApiVendor.GPT,
            ApiVendor.Claude -> true

            ApiVendor.Grok -> listOf("build", "multi-agent", "reasoning", "fast")
                .none { marker -> normalized.contains(marker) }

            ApiVendor.Mixtral -> !normalized.startsWith("ministral")
        }
    }

    suspend fun sendMessage(
        apiKey: String,
        persona: PersonaEntity,
        history: List<MessageEntity>,
        userInput: String,
        vendor: ApiVendor,
        safetyLevel: SafetyLevel,
        images: List<Bitmap>,
        webSearchEnabled: Boolean,
        masterPrompt: String? = null
    ): Result<GeminiChatReply> = resultCatchingNonCancellation {
        val finalSystemPrompt = finalSystemPrompt(masterPrompt, persona)

        when (vendor) {
            ApiVendor.Google -> doroAutoRouter.run(
                selectedModel = persona.model,
                apiKey = apiKey
            ) { resolvedModel ->
                val resolvedPersona = persona.copy(model = resolvedModel)
                if (webSearchEnabled) {
                    sendGroundedGemini(
                        apiKey = apiKey,
                        persona = resolvedPersona,
                        history = history,
                        userInput = userInput,
                        finalSystemPrompt = finalSystemPrompt,
                        safetyLevel = safetyLevel,
                        images = images
                    )
                } else {
                    sendGemini(
                        apiKey = apiKey,
                        persona = resolvedPersona,
                        history = history,
                        userInput = userInput,
                        finalSystemPrompt = finalSystemPrompt,
                        safetyLevel = safetyLevel,
                        images = images
                    )
                }
            }

            ApiVendor.Claude -> sendClaude(
                apiKey = apiKey,
                persona = persona,
                history = history,
                userInput = userInput,
                finalSystemPrompt = finalSystemPrompt,
                images = images
            )

            ApiVendor.GPT,
            ApiVendor.Grok,
            ApiVendor.Mixtral -> sendOpenAiCompatible(
                apiKey = apiKey,
                vendor = vendor,
                persona = persona,
                history = history,
                userInput = userInput,
                finalSystemPrompt = finalSystemPrompt,
                images = images
            )
        }
    }

    suspend fun planGeminiToolUse(
        apiKey: String,
        persona: PersonaEntity,
        history: List<MessageEntity>,
        userInput: String,
        safetyLevel: SafetyLevel,
        images: List<Bitmap>,
        enabledTools: Set<String>,
        masterPrompt: String? = null
    ): Result<GeminiToolPlan> = resultCatchingNonCancellation {
        planToolUse(
            apiKey = apiKey,
            vendor = ApiVendor.Google,
            persona = persona,
            history = history,
            userInput = userInput,
            safetyLevel = safetyLevel,
            images = images,
            enabledTools = enabledTools,
            masterPrompt = masterPrompt
        ).getOrThrow()
    }

    suspend fun planToolUse(
        apiKey: String,
        vendor: ApiVendor,
        persona: PersonaEntity,
        history: List<MessageEntity>,
        userInput: String,
        safetyLevel: SafetyLevel,
        images: List<Bitmap>,
        enabledTools: Set<String>,
        masterPrompt: String? = null
    ): Result<GeminiToolPlan> = resultCatchingNonCancellation {
        require(enabledTools.isNotEmpty()) { "No tools enabled." }
        val promptedSystem = finalSystemPrompt(masterPrompt, persona) + "\n\n" + toolUseInstruction(enabledTools)
        when (vendor) {
            ApiVendor.Google -> doroAutoRouter.run(
                selectedModel = persona.model,
                apiKey = apiKey
            ) { resolvedModel ->
                sendGeminiToolPlanning(
                    apiKey = apiKey,
                    persona = persona.copy(model = resolvedModel),
                    history = history,
                    userInput = userInput,
                    finalSystemPrompt = promptedSystem,
                    safetyLevel = safetyLevel,
                    images = images,
                    enabledTools = enabledTools
                )
            }

            ApiVendor.Claude -> sendClaudeToolPlanning(
                apiKey = apiKey,
                persona = persona,
                history = history,
                userInput = userInput,
                finalSystemPrompt = promptedSystem,
                images = images,
                enabledTools = enabledTools
            )

            ApiVendor.GPT,
            ApiVendor.Grok,
            ApiVendor.Mixtral -> sendOpenAiToolPlanning(
                apiKey = apiKey,
                vendor = vendor,
                persona = persona,
                history = history,
                userInput = userInput,
                finalSystemPrompt = promptedSystem,
                images = images,
                enabledTools = enabledTools
            )
        }
    }

    suspend fun pickRule34Tags(
        apiKey: String,
        userRequest: String,
        presetHint: String = ""
    ): Result<String> = runCatching {
        val prompt = """
            Convert this image request into Rule34/Danbooru search tags.
            Preset guidance: $presetHint
            Rules:
            - Output ONLY the tags, space-separated
            - Use underscores for spaces within a tag (e.g. long_hair, serious_face)
            - Output 2 to 4 tags
            - Put the most important request-specific tag first
            - Put generic support tags like 1girl, 1boy, solo, looking_at_viewer last
            - Prefer common Booru tags over invented mood or prose words
            - Do NOT include rating tags
            - Example output: maid black_hair large_breasts 1girl
            Request: "$userRequest"
        """.trimIndent()
        val responseText = generateFlashLiteUtilityContent(
            apiKey = apiKey,
            prompt = prompt,
            temperature = 0.1,
            maxOutputTokens = 32
        )
        parseRule34Tags(responseText).ifBlank {
            fallbackRule34Tags(userRequest)
        }
    }

    suspend fun autoFillPersonaFields(
        apiKey: String,
        userPrompt: String
    ): Result<PersonaAutoFillResult> = runCatching {
        val responseSchema = JSONObject()
            .put("type", "OBJECT")
            .put(
                "properties",
                JSONObject()
                    .put("storyLore", JSONObject().put("type", "STRING"))
                    .put("beginnerRole", JSONObject().put("type", "STRING"))
                    .put("beginnerStyle", JSONObject().put("type", "STRING"))
                    .put("beginnerLimits", JSONObject().put("type", "STRING"))
            )
            .put(
                "required",
                JSONArray(listOf("storyLore", "beginnerRole", "beginnerStyle", "beginnerLimits"))
            )

        val prompt = """
            Analyze the following prompt and generate structured data to create a roleplay persona.
            Ensure each field is highly descriptive, engaging, and directly based on the user's scenario.

            User Prompt: "$userPrompt"
        """.trimIndent()

        val responseText = generateFlashLiteUtilityContent(
            apiKey = apiKey,
            prompt = prompt,
            temperature = 0.4,
            responseMimeType = "application/json",
            responseSchema = responseSchema
        )

        // The model may wrap JSON in ``` fences or add stray text; extract the
        // first {...} block before parsing so a stray character never breaks it.
        val rawText = responseText.trim()
        val jsonText = extractFirstJsonObject(rawText)
            ?: throw Exception("Empty or invalid response from Gemini")

        val json = JSONObject(jsonText)
        PersonaAutoFillResult(
            storyLore = json.optString("storyLore").trim(),
            beginnerRole = json.optString("beginnerRole").trim(),
            beginnerStyle = json.optString("beginnerStyle").trim(),
            beginnerLimits = json.optString("beginnerLimits").trim()
        )
    }

    private fun extractFirstJsonObject(raw: String): String? {
        val start = raw.indexOf('{')
        if (start < 0) return null
        var depth = 0
        var inString = false
        var escaped = false
        for (index in start until raw.length) {
            val char = raw[index]
            if (inString) {
                if (escaped) escaped = false
                else if (char == '\\') escaped = true
                else if (char == '"') inString = false
            } else {
                when (char) {
                    '"' -> inString = true
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) return raw.substring(start, index + 1)
                    }
                }
            }
        }
        return null
    }

    suspend fun prepareJapaneseSpeechText(
        apiKey: String,
        cleanedText: String
    ): Result<String> = runCatching {
        val prompt = """
            Convert the following roleplay dialogue into natural spoken Japanese for text-to-speech.

            Rules:
            - Output only the final Japanese speech text.
            - Keep only dialogue that should be spoken aloud.
            - Do not include narration, actions, thoughts, role labels, markdown, or explanations.
            - Preserve emotional tone and intimacy.
            - Make the Japanese sound natural when spoken.
            - Do not add new story content.

            Text:
            $cleanedText
        """.trimIndent()
        val responseText = generateFlashLiteUtilityContent(
            apiKey = apiKey,
            prompt = prompt,
            temperature = 0.2,
            maxOutputTokens = 2048
        )
        cleanSpeechPreparation(responseText)
            .ifBlank { cleanedText.trim() }
    }

    suspend fun summarizeConversation(
        apiKey: String,
        chatLog: String,
        masterPrompt: String?
    ): Result<GeminiChatReply> = runCatching {
        withContext(Dispatchers.IO) {
            val systemPrompt = summarizerSystemPrompt(masterPrompt)
            val body = JSONObject()
                .put(
                    "systemInstruction",
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", systemPrompt))
                    )
                )
                .put(
                    "contents",
                    JSONArray().put(
                        JSONObject()
                            .put("role", "user")
                            .put("parts", JSONArray().put(JSONObject().put("text", chatLog)))
                    )
                )
                .put(
                    "generationConfig",
                    JSONObject()
                        .put("temperature", 0.2)
                        .put("maxOutputTokens", 1024)
                        .put("thinkingConfig", JSONObject().put("thinkingBudget", FLASH_LITE_THINKING_BUDGET))
                )
                .put("safetySettings", geminiRestSafetySettings(SafetyLevel.None))

            val json = postJson(
                url = "https://generativelanguage.googleapis.com/v1beta/models/$SUMMARIZER_MODEL:generateContent",
                headers = mapOf("x-goog-api-key" to apiKey),
                body = body
            )
            val candidate = json.optJSONArray("candidates")?.optJSONObject(0)
                ?: error("No summarizer response candidate returned.")
            val usage = json.optJSONObject("usageMetadata")
            GeminiChatReply(
                text = extractGeminiRestText(candidate).trim(),
                promptTokenCount = usage?.optInt("promptTokenCount") ?: 0,
                responseTokenCount = usage?.optInt("candidatesTokenCount") ?: 0,
                totalTokenCount = usage?.optInt("totalTokenCount") ?: 0
            )
        }
    }

    suspend fun routeGroupSpeakers(
        apiKey: String,
        routingPrompt: String,
        allowedAliases: List<String>
    ): Result<GeminiDirectorReply> = runCatching {
        withContext(Dispatchers.IO) {
            val primary = runCatching {
                routeGroupSpeakersWithModel(
                    apiKey = apiKey,
                    routingPrompt = routingPrompt,
                    allowedAliases = allowedAliases,
                    model = FLASH_LITE_MODEL,
                    thinkingConfig = JSONObject().put("thinkingLevel", "minimal")
                )
            }
            primary.getOrElse { primaryError ->
                runCatching {
                    routeGroupSpeakersWithModel(
                        apiKey = apiKey,
                        routingPrompt = routingPrompt,
                        allowedAliases = allowedAliases,
                        model = "gemini-2.5-flash-lite",
                        thinkingConfig = JSONObject().put("thinkingBudget", 0)
                    )
                }.getOrElse { fallbackError ->
                    error(
                        "Director request failed. Primary: ${primaryError.message.orEmpty().take(180)}; " +
                            "fallback: ${fallbackError.message.orEmpty().take(180)}"
                    )
                }
            }
        }
    }

    private fun routeGroupSpeakersWithModel(
        apiKey: String,
        routingPrompt: String,
        allowedAliases: List<String>,
        model: String,
        thinkingConfig: JSONObject
    ): GeminiDirectorReply {
        val allowedAliasJson = JSONArray().apply { allowedAliases.forEach { alias -> put(alias) } }
        val responseSchema = JSONObject()
            .put("type", "OBJECT")
            .put(
                "properties",
                JSONObject().put(
                    "speakerIds",
                    JSONObject()
                        .put("type", "ARRAY")
                        .put(
                            "items",
                            JSONObject()
                                .put("type", "STRING")
                                .put("enum", allowedAliasJson)
                        )
                )
            )
            .put("required", JSONArray().put("speakerIds"))
        val body = JSONObject()
            .put(
                "contents",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("parts", JSONArray().put(JSONObject().put("text", routingPrompt)))
                )
            )
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.0)
                    .put("maxOutputTokens", 256)
                    .put("responseMimeType", "application/json")
                    .put("responseSchema", responseSchema)
                    .put("thinkingConfig", thinkingConfig)
            )
            .put("safetySettings", geminiRestSafetySettings(SafetyLevel.None))
        val json = postJson(
            url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent",
            headers = mapOf("x-goog-api-key" to apiKey),
            body = body
        )
        val candidate = json.optJSONArray("candidates")?.optJSONObject(0)
            ?: error("No Director response candidate returned by $model.")
        val text = extractGeminiRestText(candidate).trim()
        if (text.isBlank()) error("Director returned an empty response from $model.")
        val response = JSONObject(text)
        val aliasesJson = response.optJSONArray("speakerIds") ?: JSONArray()
        val aliases = buildList {
            for (index in 0 until aliasesJson.length()) {
                aliasesJson.optString(index).trim().takeIf { it.isNotBlank() }?.let(::add)
            }
        }
        val usage = json.optJSONObject("usageMetadata")
        return GeminiDirectorReply(
            speakerAliases = aliases,
            promptTokenCount = usage?.optInt("promptTokenCount") ?: 0,
            responseTokenCount = usage?.optInt("candidatesTokenCount") ?: 0,
            totalTokenCount = usage?.optInt("totalTokenCount") ?: 0
        )
    }

    fun estimateMessageRequestTokens(
        persona: PersonaEntity,
        history: List<MessageEntity>,
        userInput: String,
        masterPrompt: String?
    ): Int {
        return TokenEstimator.estimateRequestTokens(
            systemPrompt = finalSystemPrompt(masterPrompt, persona),
            historyContents = history.map { it.content },
            userInput = userInput
        )
    }

    fun estimateToolPlanRequestTokens(
        persona: PersonaEntity,
        history: List<MessageEntity>,
        userInput: String,
        enabledTools: Set<String>,
        masterPrompt: String?
    ): Int {
        return TokenEstimator.estimateRequestTokens(
            systemPrompt = finalSystemPrompt(masterPrompt, persona) + "\n\n" +
                toolUseInstruction(enabledTools),
            historyContents = history.map { it.content },
            userInput = userInput
        )
    }

    fun estimateSummarizerRequestTokens(
        chatLog: String,
        masterPrompt: String?
    ): Int {
        return TokenEstimator.estimateRequestTokens(
            systemPrompt = summarizerSystemPrompt(masterPrompt),
            historyContents = emptyList(),
            userInput = chatLog
        )
    }

    private suspend fun sendGemini(
        apiKey: String,
        persona: PersonaEntity,
        history: List<MessageEntity>,
        userInput: String,
        finalSystemPrompt: String,
        safetyLevel: SafetyLevel,
        images: List<Bitmap>
    ): GeminiChatReply = withContext(Dispatchers.IO) {
        val contents = JSONArray()
        history.forEach { message ->
            val role = when (message.role) {
                "model" -> "model"
                "user" -> "user"
                else -> return@forEach
            }
            contents.put(
                JSONObject()
                    .put("role", role)
                    .put("parts", JSONArray().put(JSONObject().put("text", message.content)))
            )
        }
        contents.put(
            JSONObject()
                .put("role", "user")
                .put("parts", geminiParts(userInput, images))
        )

        val body = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", finalSystemPrompt))))
            .put("contents", contents)
            .put("generationConfig", geminiGenerationConfig(persona, 8192))
            .put("safetySettings", geminiRestSafetySettings(safetyLevel))

        val json = postJson(
            url = "https://generativelanguage.googleapis.com/v1beta/models/${persona.model}:generateContent",
            headers = mapOf("x-goog-api-key" to apiKey),
            body = body
        )
        val candidate = json.optJSONArray("candidates")?.optJSONObject(0)
            ?: error("No Gemini response candidate returned.")
        val usage = json.optJSONObject("usageMetadata")
        GeminiChatReply(
            text = extractGeminiRestText(candidate),
            promptTokenCount = usage?.optInt("promptTokenCount") ?: 0,
            responseTokenCount = usage?.optInt("candidatesTokenCount") ?: 0,
            totalTokenCount = usage?.optInt("totalTokenCount") ?: 0
        )
    }

    private suspend fun sendGroundedGemini(
        apiKey: String,
        persona: PersonaEntity,
        history: List<MessageEntity>,
        userInput: String,
        finalSystemPrompt: String,
        safetyLevel: SafetyLevel,
        images: List<Bitmap>
    ): GeminiChatReply = withContext(Dispatchers.IO) {
        val contents = JSONArray()
        history.forEach { message ->
            contents.put(
                JSONObject()
                    .put("role", if (message.role == "model") "model" else "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", message.content)))
            )
        }
        contents.put(
            JSONObject()
                .put("role", "user")
                .put("parts", geminiParts(userInput, images))
        )

        val body = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", finalSystemPrompt))))
            .put("contents", contents)
            .put("generationConfig", geminiGenerationConfig(persona, 8192))
            .put("safetySettings", geminiRestSafetySettings(safetyLevel))
            .put("tools", JSONArray().put(JSONObject().put("google_search", JSONObject())))

        val json = postJson(
            url = "https://generativelanguage.googleapis.com/v1beta/models/${persona.model}:generateContent",
            headers = mapOf("x-goog-api-key" to apiKey),
            body = body
        )
        val candidate = json.getJSONArray("candidates").getJSONObject(0)
        val responseText = extractGeminiRestText(candidate)
        val groundedText = appendGroundingSources(responseText, candidate.optJSONObject("groundingMetadata"))
        val usage = json.optJSONObject("usageMetadata")
        GeminiChatReply(
            text = groundedText,
            promptTokenCount = usage?.optInt("promptTokenCount") ?: 0,
            responseTokenCount = usage?.optInt("candidatesTokenCount") ?: 0,
            totalTokenCount = usage?.optInt("totalTokenCount") ?: 0
        )
    }

    private suspend fun sendGeminiToolPlanning(
        apiKey: String,
        persona: PersonaEntity,
        history: List<MessageEntity>,
        userInput: String,
        finalSystemPrompt: String,
        safetyLevel: SafetyLevel,
        images: List<Bitmap>,
        enabledTools: Set<String>
    ): GeminiToolPlan = withContext(Dispatchers.IO) {
        val contents = JSONArray()
        history.forEach { message ->
            contents.put(
                JSONObject()
                    .put("role", if (message.role == "model") "model" else "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", message.content)))
            )
        }
        contents.put(
            JSONObject()
                .put("role", "user")
                .put("parts", geminiParts(userInput, images))
        )

        val body = JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", finalSystemPrompt))))
            .put("contents", contents)
            .put("generationConfig", geminiGenerationConfig(persona, 8192))
            .put("safetySettings", geminiRestSafetySettings(safetyLevel))
            .put(
                "tools",
                JSONArray().put(
                    JSONObject().put("functionDeclarations", geminiFunctionDeclarations(enabledTools))
                )
            )

        val json = postJson(
            url = "https://generativelanguage.googleapis.com/v1beta/models/${persona.model}:generateContent",
            headers = mapOf("x-goog-api-key" to apiKey),
            body = body
        )
        val candidate = json.getJSONArray("candidates").getJSONObject(0)
        val parts = candidate.optJSONObject("content")?.optJSONArray("parts") ?: JSONArray()
        var functionCall: GeminiToolCall? = null
        val responseText = buildString {
            for (index in 0 until parts.length()) {
                val part = parts.optJSONObject(index) ?: continue
                part.optString("text").takeIf { it.isNotBlank() }?.let { append(it) }
                val call = part.optJSONObject("functionCall") ?: continue
                functionCall = GeminiToolCall(
                    name = call.optString("name"),
                    args = call.optJSONObject("args") ?: JSONObject()
                )
            }
        }
        val usage = json.optJSONObject("usageMetadata")
        GeminiToolPlan(
            text = responseText,
            functionCall = functionCall?.takeIf { it.name.isNotBlank() },
            promptTokenCount = usage?.optInt("promptTokenCount") ?: 0,
            responseTokenCount = usage?.optInt("candidatesTokenCount") ?: 0,
            totalTokenCount = usage?.optInt("totalTokenCount") ?: 0
        )
    }

    private suspend fun sendOpenAiToolPlanning(
        apiKey: String,
        vendor: ApiVendor,
        persona: PersonaEntity,
        history: List<MessageEntity>,
        userInput: String,
        finalSystemPrompt: String,
        images: List<Bitmap>,
        enabledTools: Set<String>
    ): GeminiToolPlan = withContext(Dispatchers.IO) {
        val url = when (vendor) {
            ApiVendor.GPT -> "https://api.openai.com/v1/chat/completions"
            ApiVendor.Grok -> "https://api.x.ai/v1/chat/completions"
            ApiVendor.Mixtral -> "https://api.mistral.ai/v1/chat/completions"
            else -> error("Unsupported OpenAI-compatible vendor")
        }
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", finalSystemPrompt))
        history.forEach { message ->
            val role = if (message.role == "model") "assistant" else message.role
            messages.put(JSONObject().put("role", role).put("content", message.content))
        }
        messages.put(JSONObject().put("role", "user").put("content", openAiUserContent(userInput, images)))

        val body = JSONObject()
            .put("model", persona.model)
            .put("messages", messages)
            .put("temperature", persona.temperature.toDouble())
            .put("max_tokens", 4096)
            .put("tools", openAiTools(enabledTools))
            .put("tool_choice", "auto")

        val json = postJson(
            url = url,
            headers = mapOf("Authorization" to "Bearer $apiKey"),
            body = body
        )
        val message = json.getJSONArray("choices").getJSONObject(0).getJSONObject("message")
        val toolCall = message.optJSONArray("tool_calls")
            ?.optJSONObject(0)
            ?.optJSONObject("function")
            ?.let { function ->
                GeminiToolCall(
                    name = function.optString("name"),
                    args = parseJsonObject(function.optString("arguments"))
                )
            }
        val usage = json.optJSONObject("usage")
        GeminiToolPlan(
            text = extractOpenAiContent(message),
            functionCall = toolCall?.takeIf { it.name.isNotBlank() },
            promptTokenCount = usage?.optInt("prompt_tokens") ?: 0,
            responseTokenCount = usage?.optInt("completion_tokens") ?: 0,
            totalTokenCount = usage?.optInt("total_tokens") ?: 0
        )
    }

    private suspend fun sendClaudeToolPlanning(
        apiKey: String,
        persona: PersonaEntity,
        history: List<MessageEntity>,
        userInput: String,
        finalSystemPrompt: String,
        images: List<Bitmap>,
        enabledTools: Set<String>
    ): GeminiToolPlan = withContext(Dispatchers.IO) {
        val messages = JSONArray()
        history.forEach { message ->
            val role = if (message.role == "model") "assistant" else "user"
            messages.put(JSONObject().put("role", role).put("content", message.content))
        }
        messages.put(JSONObject().put("role", "user").put("content", claudeUserContent(userInput, images)))

        val body = JSONObject()
            .put("model", persona.model)
            .put("system", finalSystemPrompt)
            .put("messages", messages)
            .put("temperature", persona.temperature.toDouble())
            .put("max_tokens", 4096)
            .put("tools", claudeTools(enabledTools))
            .put("tool_choice", JSONObject().put("type", "auto"))

        val json = postJson(
            url = "https://api.anthropic.com/v1/messages",
            headers = mapOf(
                "x-api-key" to apiKey,
                "anthropic-version" to "2023-06-01"
            ),
            body = body
        )
        val content = json.getJSONArray("content")
        var toolCall: GeminiToolCall? = null
        val text = buildString {
            for (index in 0 until content.length()) {
                val part = content.optJSONObject(index) ?: continue
                when (part.optString("type")) {
                    "text" -> append(part.optString("text"))
                    "tool_use" -> toolCall = GeminiToolCall(
                        name = part.optString("name"),
                        args = part.optJSONObject("input") ?: JSONObject()
                    )
                }
            }
        }
        val usage = json.optJSONObject("usage")
        val inputTokens = usage?.optInt("input_tokens") ?: 0
        val outputTokens = usage?.optInt("output_tokens") ?: 0
        GeminiToolPlan(
            text = text,
            functionCall = toolCall?.takeIf { it.name.isNotBlank() },
            promptTokenCount = inputTokens,
            responseTokenCount = outputTokens,
            totalTokenCount = inputTokens + outputTokens
        )
    }

    private suspend fun sendOpenAiCompatible(
        apiKey: String,
        vendor: ApiVendor,
        persona: PersonaEntity,
        history: List<MessageEntity>,
        userInput: String,
        finalSystemPrompt: String,
        images: List<Bitmap>
    ): GeminiChatReply = withContext(Dispatchers.IO) {
        val url = when (vendor) {
            ApiVendor.GPT -> "https://api.openai.com/v1/chat/completions"
            ApiVendor.Grok -> "https://api.x.ai/v1/chat/completions"
            ApiVendor.Mixtral -> "https://api.mistral.ai/v1/chat/completions"
            else -> error("Unsupported OpenAI-compatible vendor")
        }
        val messages = JSONArray()
            .put(JSONObject().put("role", "system").put("content", finalSystemPrompt))
        history.forEach { message ->
            val role = if (message.role == "model") "assistant" else message.role
            messages.put(JSONObject().put("role", role).put("content", message.content))
        }
        messages.put(JSONObject().put("role", "user").put("content", openAiUserContent(userInput, images)))

        val body = JSONObject()
            .put("model", persona.model)
            .put("messages", messages)
            .put("temperature", persona.temperature.toDouble())
            .put("max_tokens", 4096)

        val json = postJson(
            url = url,
            headers = mapOf("Authorization" to "Bearer $apiKey"),
            body = body
        )
        val content = extractOpenAiContent(json.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message"))
        val usage = json.optJSONObject("usage")
        GeminiChatReply(
            text = content,
            promptTokenCount = usage?.optInt("prompt_tokens") ?: 0,
            responseTokenCount = usage?.optInt("completion_tokens") ?: 0,
            totalTokenCount = usage?.optInt("total_tokens") ?: 0
        )
    }

    private suspend fun sendClaude(
        apiKey: String,
        persona: PersonaEntity,
        history: List<MessageEntity>,
        userInput: String,
        finalSystemPrompt: String,
        images: List<Bitmap>
    ): GeminiChatReply = withContext(Dispatchers.IO) {
        val messages = JSONArray()
        history.forEach { message ->
            val role = if (message.role == "model") "assistant" else "user"
            messages.put(JSONObject().put("role", role).put("content", message.content))
        }
        messages.put(JSONObject().put("role", "user").put("content", claudeUserContent(userInput, images)))

        val body = JSONObject()
            .put("model", persona.model)
            .put("system", finalSystemPrompt)
            .put("messages", messages)
            .put("temperature", persona.temperature.toDouble())
            .put("max_tokens", 4096)

        val json = postJson(
            url = "https://api.anthropic.com/v1/messages",
            headers = mapOf(
                "x-api-key" to apiKey,
                "anthropic-version" to "2023-06-01"
            ),
            body = body
        )
        val content = extractClaudeText(json.getJSONArray("content"))
        val usage = json.optJSONObject("usage")
        val inputTokens = usage?.optInt("input_tokens") ?: 0
        val outputTokens = usage?.optInt("output_tokens") ?: 0
        GeminiChatReply(
            text = content,
            promptTokenCount = inputTokens,
            responseTokenCount = outputTokens,
            totalTokenCount = inputTokens + outputTokens
        )
    }

    private fun safetySettings(level: SafetyLevel): List<SafetySetting> {
        val threshold = when (level) {
            SafetyLevel.None -> BlockThreshold.NONE
            SafetyLevel.Low -> BlockThreshold.ONLY_HIGH
            SafetyLevel.Medium -> BlockThreshold.MEDIUM_AND_ABOVE
            SafetyLevel.High -> BlockThreshold.LOW_AND_ABOVE
        }
        return listOf(
            SafetySetting(HarmCategory.HARASSMENT, threshold),
            SafetySetting(HarmCategory.HATE_SPEECH, threshold),
            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, threshold),
            SafetySetting(HarmCategory.DANGEROUS_CONTENT, threshold)
        )
    }

    private fun finalSystemPrompt(masterPrompt: String?, persona: PersonaEntity): String {
        val baseMasterPrompt = masterPrompt?.takeIf { it.isNotBlank() } ?: FALLBACK_MASTER_PROMPT
        return listOfNotNull(
            baseMasterPrompt.trimIndent(),
            skillRepository?.combinedContent?.takeIf { it.isNotBlank() },
            persona.systemPrompt.trim()
        ).joinToString("\n\n---\n\n")
    }

    private fun summarizerSystemPrompt(masterPrompt: String?): String {
        val baseMasterPrompt = masterPrompt
            ?.takeIf { it.isNotBlank() }
            ?.trimIndent()
            ?: FALLBACK_MASTER_PROMPT.trimIndent()
        return listOf(
            baseMasterPrompt,
            SUMMARIZER_SYSTEM_PROMPT.trimIndent()
        ).joinToString("\n\n---\n\n")
    }

    private fun toolUseInstruction(enabledTools: Set<String>): String {
        return buildString {
            appendLine("App tool policy:")
            appendLine("- The following function tools are enabled by the user in the app UI and relevant to the current message: ${enabledTools.joinToString()}.")
            appendLine("- If a tool is listed here, that tool is ON. If a tool is not listed here, treat it as OFF or unavailable.")
            appendLine("- Decide tool use only from the current user message, not from an older request in chat history.")
            appendLine("- If the user is only chatting, answer normally without calling a tool.")
            if ("web_search" in enabledTools) {
                appendLine("- Call web_search only when the current message asks for current, recent, factual, source-backed, or explicitly searched information.")
            }
            if ("anime_image_search" in enabledTools) {
                appendLine("- Call anime_image_search only when the current message asks you to show, find, fetch, get, or display an anime-style image.")
            }
            appendLine("- Do not claim a tool was used unless you call it.")
        }
    }

    private fun geminiFunctionDeclarations(enabledTools: Set<String>): JSONArray {
        return JSONArray().apply {
            if ("web_search" in enabledTools) {
                put(
                    JSONObject()
                        .put("name", "web_search")
                        .put("description", "Search the web for current or source-backed information.")
                        .put(
                            "parameters",
                            JSONObject()
                                .put("type", "OBJECT")
                                .put(
                                    "properties",
                                    JSONObject().put(
                                        "query",
                                        JSONObject()
                                            .put("type", "STRING")
                                            .put("description", "The search query to retrieve web context for.")
                                    )
                                )
                                .put("required", JSONArray().put("query"))
                        )
                )
            }
            if ("anime_image_search" in enabledTools) {
                put(
                    JSONObject()
                        .put("name", "anime_image_search")
                        .put("description", "Find an anime-style image matching the user's request.")
                        .put(
                            "parameters",
                            JSONObject()
                                .put("type", "OBJECT")
                                .put(
                                    "properties",
                                    JSONObject()
                                        .put(
                                            "request",
                                            JSONObject()
                                                .put("type", "STRING")
                                                .put("description", "The user's image request in natural language.")
                                        )
                                        .put(
                                            "preset",
                                            JSONObject()
                                                .put("type", "STRING")
                                                .put("description", "Optional image preset: auto, portrait, character, or explicit.")
                                        )
                                )
                                .put("required", JSONArray().put("request"))
                        )
                )
            }
        }
    }

    private fun openAiTools(enabledTools: Set<String>): JSONArray {
        return JSONArray().apply {
            geminiFunctionDeclarations(enabledTools).forEachObject { declaration ->
                put(
                    JSONObject()
                        .put("type", "function")
                        .put("function", declaration)
                )
            }
        }
    }

    private fun claudeTools(enabledTools: Set<String>): JSONArray {
        return JSONArray().apply {
            geminiFunctionDeclarations(enabledTools).forEachObject { declaration ->
                put(
                    JSONObject()
                        .put("name", declaration.optString("name"))
                        .put("description", declaration.optString("description"))
                        .put("input_schema", declaration.optJSONObject("parameters") ?: JSONObject())
                )
            }
        }
    }

    private fun parseRule34Tags(raw: String): String {
        return raw
            .trim()
            .substringBefore('\n')
            .lowercase(Locale.US)
            .replace(',', ' ')
            .split(Regex("\\s+"))
            .map { tag ->
                tag.trim('"', '\'', '`', ':', '.', ';')
                    .replace('-', '_')
                    .replace(Regex("[^a-z0-9_]+"), "_")
                    .trim('_')
            }
            .filter { tag -> tag.isNotBlank() && !tag.startsWith("rating") }
            .take(4)
            .joinToString(" ")
    }

    private fun cleanSpeechPreparation(raw: String): String {
        return raw
            .replace(Regex("(?s)```.*?```"), "")
            .replace(Regex("^\\s*日本語\\s*[:：]\\s*", RegexOption.IGNORE_CASE), "")
            .trim()
            .trim('"', '\'', '`')
            .lines()
            .map { line -> line.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }

    fun fallbackRule34Tags(userRequest: String): String {
        val request = userRequest.lowercase(Locale.US)
        val mappedTags = linkedSetOf<String>()
        val phraseMap = listOf(
            "school uniform" to "school_uniform",
            "bunny girl" to "bunny_girl",
            "cat girl" to "cat_girl",
            "catgirl" to "cat_girl",
            "maid" to "maid",
            "nurse" to "nurse",
            "teacher" to "teacher",
            "secretary" to "secretary",
            "uniform" to "uniform",
            "kimono" to "kimono",
            "swimsuit" to "swimsuit",
            "bikini" to "bikini",
            "stockings" to "stockings",
            "thighhighs" to "thighhighs",
            "glasses" to "glasses",
            "white hair" to "white_hair",
            "black hair" to "black_hair",
            "blonde" to "blonde_hair",
            "red hair" to "red_hair",
            "blue hair" to "blue_hair",
            "pink hair" to "pink_hair",
            "long hair" to "long_hair",
            "short hair" to "short_hair",
            "large breasts" to "large_breasts",
            "big boobs" to "large_breasts",
            "oppai" to "large_breasts",
            "ass" to "ass",
            "feet" to "feet",
            "oral" to "fellatio",
            "blowjob" to "fellatio",
            "genshin" to "genshin_impact",
            "raiden" to "raiden_shogun",
            "ayaka" to "kamisato_ayaka",
            "marin" to "kitagawa_marin",
            "rem" to "rem_(re:zero)"
        )
        phraseMap.forEach { (needle, tag) ->
            if (request.contains(needle)) mappedTags += tag
        }
        request
            .replace(Regex("[^a-z0-9_\\s-]+"), " ")
            .split(Regex("\\s+"))
            .map { token -> token.replace('-', '_').trim('_') }
            .filter { token ->
                token.length > 2 &&
                    token !in setOf("show", "give", "want", "with", "that", "this", "girl", "anime", "image", "picture", "photo", "please")
            }
            .forEach { token -> mappedTags += token }
        return mappedTags.take(4).joinToString(" ").ifBlank { "1girl" }
    }

    private fun openAiUserContent(userInput: String, images: List<Bitmap>): Any {
        if (images.isEmpty()) return userInput
        return JSONArray().apply {
            put(JSONObject().put("type", "text").put("text", userInput))
            images.forEach { image ->
                put(
                    JSONObject()
                        .put("type", "image_url")
                        .put(
                            "image_url",
                            JSONObject().put("url", "data:image/jpeg;base64,${image.toBase64Jpeg()}")
                        )
                )
            }
        }
    }

    private fun claudeUserContent(userInput: String, images: List<Bitmap>): Any {
        if (images.isEmpty()) return userInput
        return JSONArray().apply {
            put(JSONObject().put("type", "text").put("text", userInput))
            images.forEach { image ->
                put(
                    JSONObject()
                        .put("type", "image")
                        .put(
                            "source",
                            JSONObject()
                                .put("type", "base64")
                                .put("media_type", "image/jpeg")
                                .put("data", image.toBase64Jpeg())
                        )
                )
            }
        }
    }

    private fun geminiParts(userInput: String, images: List<Bitmap>): JSONArray {
        return JSONArray().apply {
            put(JSONObject().put("text", userInput))
            images.forEach { image ->
                put(
                    JSONObject().put(
                        "inlineData",
                        JSONObject()
                            .put("mimeType", "image/jpeg")
                            .put("data", image.toBase64Jpeg())
                    )
                )
            }
        }
    }

    private fun geminiGenerationConfig(persona: PersonaEntity, maxOutputTokens: Int): JSONObject {
        val effectiveBudget = if (persona.model.equals(FLASH_LITE_MODEL, ignoreCase = true)) {
            FLASH_LITE_THINKING_BUDGET
        } else {
            persona.thinkingBudget?.takeIf { budget -> budget > 0 }
        }
        return JSONObject()
            .put("temperature", persona.temperature.toDouble())
            .put("maxOutputTokens", maxOutputTokens)
            .apply {
                effectiveBudget?.let { budget ->
                    put("thinkingConfig", JSONObject().put("thinkingBudget", budget))
                }
            }
    }

    private suspend fun generateFlashLiteUtilityContent(
        apiKey: String,
        prompt: String,
        temperature: Double,
        maxOutputTokens: Int? = null,
        responseMimeType: String? = null,
        responseSchema: JSONObject? = null
    ): String = withContext(Dispatchers.IO) {
        val generationConfig = JSONObject()
            .put("temperature", temperature)
            .put("thinkingConfig", JSONObject().put("thinkingBudget", FLASH_LITE_THINKING_BUDGET))
            .apply {
                maxOutputTokens?.let { put("maxOutputTokens", it) }
                responseMimeType?.let { put("responseMimeType", it) }
                responseSchema?.let { put("responseSchema", it) }
            }
        val body = JSONObject()
            .put(
                "contents",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                )
            )
            .put("generationConfig", generationConfig)
            .put("safetySettings", geminiRestSafetySettings(SafetyLevel.None))
        val json = postJson(
            url = "https://generativelanguage.googleapis.com/v1beta/models/$FLASH_LITE_MODEL:generateContent",
            headers = mapOf("x-goog-api-key" to apiKey),
            body = body
        )
        json.optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.optJSONObject(0)
            ?.optString("text")
            .orEmpty()
    }

    private fun geminiRestSafetySettings(level: SafetyLevel): JSONArray {
        val threshold = when (level) {
            SafetyLevel.None -> "BLOCK_NONE"
            SafetyLevel.Low -> "BLOCK_ONLY_HIGH"
            SafetyLevel.Medium -> "BLOCK_MEDIUM_AND_ABOVE"
            SafetyLevel.High -> "BLOCK_LOW_AND_ABOVE"
        }
        return JSONArray().apply {
            listOf(
                "HARM_CATEGORY_HARASSMENT",
                "HARM_CATEGORY_HATE_SPEECH",
                "HARM_CATEGORY_SEXUALLY_EXPLICIT",
                "HARM_CATEGORY_DANGEROUS_CONTENT"
            ).forEach { category ->
                put(JSONObject().put("category", category).put("threshold", threshold))
            }
        }
    }

    private fun Bitmap.toBase64Jpeg(): String {
        val output = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 90, output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }

    private fun extractGeminiRestText(candidate: JSONObject): String {
        val parts = candidate.optJSONObject("content")?.optJSONArray("parts") ?: return ""
        return buildString {
            for (index in 0 until parts.length()) {
                append(parts.optJSONObject(index)?.optString("text").orEmpty())
            }
        }
    }

    private fun appendGroundingSources(text: String, metadata: JSONObject?): String {
        val chunks = metadata?.optJSONArray("groundingChunks") ?: return text
        val sources = mutableListOf<Pair<String, String>>()
        for (index in 0 until chunks.length()) {
            val web = chunks.optJSONObject(index)?.optJSONObject("web") ?: continue
            val uri = web.optString("uri")
            if (uri.isBlank()) continue
            val title = web.optString("title").ifBlank { uri }
            sources += title to uri
        }
        if (sources.isEmpty()) return text
        val uniqueSources = sources.distinctBy { it.second }.take(5)
        return buildString {
            append(text.trimEnd())
            append("\n\nSources:")
            uniqueSources.forEachIndexed { index, (title, uri) ->
                append("\n[${index + 1}] $title - $uri")
            }
        }
    }

    private fun extractOpenAiContent(message: JSONObject): String {
        return when (val content = message.opt("content")) {
            is String -> content
            is JSONArray -> buildString {
                for (index in 0 until content.length()) {
                    val part = content.optJSONObject(index) ?: continue
                    if (part.optString("type") == "text") {
                        append(part.optString("text"))
                    }
                }
            }
            else -> ""
        }
    }

    private fun extractClaudeText(content: JSONArray): String {
        return buildString {
            for (index in 0 until content.length()) {
                val part = content.optJSONObject(index) ?: continue
                if (part.optString("type") == "text") {
                    append(part.optString("text"))
                }
            }
        }
    }

    private fun postJson(
        url: String,
        headers: Map<String, String>,
        body: JSONObject
    ): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 30_000
                readTimeout = 120_000
                setRequestProperty("Content-Type", "application/json")
                headers.forEach { (key, value) -> setRequestProperty(key, value) }
            }
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(body.toString())
            }
            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (statusCode !in 200..299) {
                val apiMessage = apiErrorMessage(responseText).ifBlank { responseText.take(160) }
                throw ApiHttpException(
                    statusCode = statusCode,
                    apiMessage = apiMessage,
                    responseBody = responseText
                )
            }
            JSONObject(responseText)
        } finally {
            connection.disconnect()
        }
    }

    private fun apiErrorMessage(responseText: String): String {
        return runCatching {
            JSONObject(responseText)
                .optJSONObject("error")
                ?.optString("message")
                .orEmpty()
        }.getOrDefault("")
    }

    private fun parseJsonObject(raw: String): JSONObject {
        return runCatching { JSONObject(raw.ifBlank { "{}" }) }.getOrElse { JSONObject() }
    }

    private inline fun JSONArray.forEachObject(block: (JSONObject) -> Unit) {
        for (index in 0 until length()) {
            optJSONObject(index)?.let(block)
        }
    }
}
