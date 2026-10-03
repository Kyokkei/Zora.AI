package com.yozora.aichat.ui.chat

import org.json.JSONArray
import org.json.JSONObject

data class ReplyVariant(
    val content: String,
    val reaction: String? = null,
    val time: String = ""
)

internal fun ChatMessage.savedReplyVariants(): List<ReplyVariant> {
    val current = ReplyVariant(content, reaction, time)
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
        replyVariants = variants,
        selectedReplyVariant = index
    )
}

internal fun ChatMessage.withAdditionalReply(content: String, time: String): ChatMessage {
    val variants = savedReplyVariants() + ReplyVariant(content = content, time = time)
    return copy(
        content = content,
        reaction = null,
        time = time,
        replyVariants = variants,
        selectedReplyVariant = variants.lastIndex
    )
}

internal fun List<ReplyVariant>.toReplyVariantsJson(): JSONArray = JSONArray().apply {
    this@toReplyVariantsJson.forEach { variant ->
        put(JSONObject()
            .put("content", variant.content)
            .put("reaction", variant.reaction ?: JSONObject.NULL)
            .put("time", variant.time))
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
                time = item.optString("time")
            ))
        }
    }
}.getOrDefault(emptyList())

internal fun ChatSession.swipeableReply(): ChatMessage? = messages.lastOrNull()?.takeIf {
    it.role == "model" && it.content.isNotBlank() && !it.isImageLoading &&
        it.remoteImageUrl == null && it.imageUris.isEmpty()
}
