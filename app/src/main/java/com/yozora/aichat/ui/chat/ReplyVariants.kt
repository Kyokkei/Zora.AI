package com.yozora.aichat.ui.chat

import org.json.JSONArray
import org.json.JSONObject

data class ReplyVariant(
    val content: String,
    val reaction: String? = null,
    val time: String = "",
    /** Concrete model that produced this reply, shown in the message action sheet. */
    val model: String? = null
)

internal fun ChatMessage.savedReplyVariants(): List<ReplyVariant> {
    val current = ReplyVariant(content, reaction, time, model)
    if (replyVariants.isEmpty()) return listOf(current)
    return replyVariants.toMutableList().apply {
        this[selectedReplyVariant.coerceIn(indices)] = current
    }
}

internal fun ChatMessage.withReplyVariant(index: Int): ChatMessage {
    val variants = savedReplyVariants()
    if (index !in variants.indices) return this
    val variant = variants[index]
    return copy(
        content = variant.content,
        reaction = variant.reaction,
        time = variant.time,
        model = variant.model,
        replyVariants = variants,
        selectedReplyVariant = index
    )
}

internal fun ChatMessage.withAdditionalReply(content: String, time: String, model: String? = null): ChatMessage {
    val variants = savedReplyVariants() + ReplyVariant(content = content, time = time, model = model)
    return copy(
        content = content,
        reaction = null,
        time = time,
        model = model,
        replyVariants = variants,
        selectedReplyVariant = variants.lastIndex
    )
}

internal fun List<ReplyVariant>.toReplyVariantsJson(): JSONArray = JSONArray().apply {
    this@toReplyVariantsJson.forEach { variant ->
        put(JSONObject()
            .put("content", variant.content)
            .put("reaction", variant.reaction ?: JSONObject.NULL)
            .put("time", variant.time)
            .put("model", variant.model ?: JSONObject.NULL))
    }
}

internal fun replyVariantsFromJson(raw: String): List<ReplyVariant> = runCatching {
    val array = JSONArray(raw)
    buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            val content = item.optString("content")
            if (content.isBlank()) continue
            add(ReplyVariant(
                content = content,
                reaction = restoreMessageReaction(item.optString("reaction")),
                time = item.optString("time"),
                model = item.optString("model").takeIf { it.isNotBlank() }
            ))
        }
    }
}.getOrDefault(emptyList())

internal fun ChatSession.swipeableReply(): ChatMessage? = messages.lastOrNull()?.takeIf {
    it.role == "model" && it.content.isNotBlank() && !it.isImageLoading &&
        it.remoteImageUrl == null && it.imageUris.isEmpty()
}
