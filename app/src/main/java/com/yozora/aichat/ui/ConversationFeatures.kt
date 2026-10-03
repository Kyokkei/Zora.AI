package com.yozora.aichat.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yozora.aichat.R
import com.yozora.aichat.ui.chat.ChatMessage
import com.yozora.aichat.ui.theme.AppAccent
import com.yozora.aichat.ui.theme.AppAccentSoft
import com.yozora.aichat.ui.theme.AppStroke
import com.yozora.aichat.ui.theme.AppSurface
import com.yozora.aichat.ui.theme.AppSurface2
import com.yozora.aichat.ui.theme.AppTextPrimary
import com.yozora.aichat.ui.theme.AppTextSecondary

@Composable
internal fun Modifier.replySwipeGestures(
    message: ChatMessage,
    enabled: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
): Modifier {
    val threshold = with(LocalDensity.current) { 64.dp.toPx() }
    val previous by rememberUpdatedState(onPrevious)
    val next by rememberUpdatedState(onNext)
    return if (!enabled) this else pointerInput(message.id, message.selectedReplyVariant, threshold) {
        var distance = 0f
        detectHorizontalDragGestures(
            onDragStart = { distance = 0f },
            onDragCancel = { distance = 0f },
            onDragEnd = {
                if (distance <= -threshold) next()
                else if (distance >= threshold) previous()
                distance = 0f
            },
            onHorizontalDrag = { change, amount ->
                change.consume()
                distance += amount
            }
        )
    }
}

@Composable
internal fun ReplyVariantControls(
    message: ChatMessage,
    isSending: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val count = message.replyVariants.size.coerceAtLeast(1)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious, enabled = !isSending && message.selectedReplyVariant > 0) {
            Icon(Icons.Rounded.ChevronLeft, stringResource(R.string.previous_reply), tint = AppAccentSoft)
        }
        Text(
            text = "${message.selectedReplyVariant + 1} / $count",
            color = AppTextSecondary,
            style = MaterialTheme.typography.labelMedium
        )
        IconButton(onClick = onNext, enabled = !isSending) {
            if (isSending) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = AppAccent)
            } else {
                Icon(
                    Icons.Rounded.ChevronRight,
                    stringResource(if (message.selectedReplyVariant < count - 1) R.string.next_reply else R.string.generate_another_reply),
                    tint = AppAccentSoft
                )
            }
        }
    }
}

@Composable
internal fun ConversationSearchNavigation(
    resultIndex: Int,
    resultCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(R.string.search_result_position, resultIndex + 1, resultCount),
            color = AppTextSecondary, style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f))
        IconButton(onClick = onPrevious, enabled = resultCount > 1) {
            Icon(Icons.Rounded.ChevronLeft, stringResource(R.string.previous_search_result), tint = AppAccentSoft)
        }
        IconButton(onClick = onNext, enabled = resultCount > 1) {
            Icon(Icons.Rounded.ChevronRight, stringResource(R.string.next_search_result), tint = AppAccentSoft)
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Rounded.Close, stringResource(R.string.close_search), tint = AppTextSecondary)
        }
    }
}

internal fun conversationSearchResults(messages: List<ChatMessage>, query: String): List<ChatMessage> {
    val keyword = query.trim()
    if (keyword.isEmpty()) return emptyList()
    return messages.filter {
        !it.isImageLoading && (it.content.contains(keyword, ignoreCase = true) ||
            it.speakerName.orEmpty().contains(keyword, ignoreCase = true))
    }
}

private fun searchSnippet(content: String, query: String): String {
    val match = content.indexOf(query, ignoreCase = true)
    val start = (match - 70).coerceAtLeast(0)
    val end = (start + maxOf(220, query.length + 140)).coerceAtMost(content.length)
    return (if (start > 0) "…" else "") + content.substring(start, end) +
        if (end < content.length) "…" else ""
}

private fun highlightedSearchText(content: String, query: String): AnnotatedString = buildAnnotatedString {
    append(content)
    if (query.isBlank()) return@buildAnnotatedString
    var start = content.indexOf(query, ignoreCase = true)
    while (start >= 0) {
        addStyle(SpanStyle(background = AppAccent.copy(alpha = 0.25f), fontWeight = FontWeight.Bold), start, start + query.length)
        start = content.indexOf(query, startIndex = start + query.length, ignoreCase = true)
    }
}

@Composable
internal fun ConversationSearchDialog(
    messages: List<ChatMessage>,
    query: String,
    onQueryChange: (String) -> Unit,
    onJumpToMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val keyword = query.trim()
    val results = remember(messages, keyword) { conversationSearchResults(messages, keyword) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            color = AppSurface,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, AppStroke),
            modifier = Modifier.fillMaxSize().imePadding().padding(horizontal = 12.dp, vertical = 24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.search_conversation), color = AppTextPrimary,
                        style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, stringResource(R.string.close_search), tint = AppTextSecondary)
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.search_conversation)) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTextPrimary, unfocusedTextColor = AppTextPrimary,
                        focusedBorderColor = AppAccent, unfocusedBorderColor = AppStroke,
                        focusedContainerColor = AppSurface2, unfocusedContainerColor = AppSurface2,
                        cursorColor = AppAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (keyword.isEmpty()) stringResource(R.string.search_conversation_hint)
                        else stringResource(R.string.search_result_count, results.size),
                    color = AppTextSecondary, style = MaterialTheme.typography.bodySmall
                )
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results, key = { it.id }) { message ->
                        Surface(
                            color = AppSurface2, shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().clickable { onJumpToMessage(message.id) }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    highlightedSearchText(if (message.role == "user") "You" else message.speakerName ?: "AI", keyword),
                                    color = AppAccentSoft, style = MaterialTheme.typography.labelLarge
                                )
                                Text(highlightedSearchText(searchSnippet(message.content, keyword), keyword),
                                    color = AppTextPrimary, style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 4, overflow = TextOverflow.Ellipsis)
                                if (message.time.isNotBlank()) {
                                    Text(message.time, color = AppTextSecondary, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
