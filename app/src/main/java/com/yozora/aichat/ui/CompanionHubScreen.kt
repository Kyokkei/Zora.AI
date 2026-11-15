package com.yozora.aichat.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yozora.aichat.ui.chat.ChatViewModel
import com.yozora.aichat.ui.chat.GeminiLiveVoice
import com.yozora.aichat.ui.chat.GeminiLiveVoiceGender
import com.yozora.aichat.ui.companion.CompanionSettingsStore
import com.yozora.aichat.ui.companion.CompanionStage
import com.yozora.aichat.ui.companion.VrmPicker

@Composable
internal fun CompanionHubScreen(
    viewModel: ChatViewModel,
    entryError: String?,
    settingsVisible: Boolean,
    onSettingsClick: () -> Unit,
    onSettingsDismiss: () -> Unit,
    onPromptSave: (String) -> Unit,
    onVrmSelected: (Uri) -> Unit,
    onVoiceSelected: (GeminiLiveVoice) -> Unit,
    onRetry: () -> Unit,
) {
    val context = LocalContext.current
    val sessionId = viewModel.activeSessionId
    val settings = remember(context) { CompanionSettingsStore(context) }
    val colors = LocalRoleplayColors.current
    var selectedVrmUri by remember(sessionId) { mutableStateOf(settings.vrmUri(sessionId)) }
    var selectedVoice by remember(sessionId) { mutableStateOf(viewModel.companionVoice(sessionId)) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF090B12))) {
        CompanionStage(
            modifier = Modifier.fillMaxSize(),
            vrmUri = selectedVrmUri,
            mouthAa = 0f,
        )

        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "Companion settings",
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }

        if (!entryError.isNullOrBlank()) {
            Surface(
                color = Color(0xD917131F),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 28.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Companion isn’t ready",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = entryError,
                        color = Color.White.copy(alpha = 0.78f),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                    TextButton(onClick = onRetry) {
                        Text("Try again", color = colors.accent, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    CompanionSettingsDialog(
        visible = settingsVisible,
        currentPrompt = viewModel.companionSystemPrompt(sessionId),
        currentVrmUri = selectedVrmUri,
        selectedVoice = selectedVoice,
        onDismiss = onSettingsDismiss,
        onSavePrompt = onPromptSave,
        onVrmSelected = { uri ->
            selectedVrmUri = uri
            onVrmSelected(uri)
        },
        onVoiceSelected = { voice ->
            selectedVoice = voice
            onVoiceSelected(voice)
        },
    )
}

@Composable
internal fun CompanionSettingsDialog(
    visible: Boolean,
    currentPrompt: String,
    currentVrmUri: Uri?,
    selectedVoice: GeminiLiveVoice,
    onDismiss: () -> Unit,
    onSavePrompt: (String) -> Unit,
    onVrmSelected: (Uri) -> Unit,
    onVoiceSelected: (GeminiLiveVoice) -> Unit,
) {
    if (!visible) return

    var prompt by remember(visible, currentPrompt) { mutableStateOf(currentPrompt) }
    val colors = LocalRoleplayColors.current
    val panelShape = RoundedCornerShape(26.dp)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color(0xFF17131F),
            shape = panelShape,
            modifier = Modifier.fillMaxWidth().heightIn(max = 680.dp),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "Companion settings",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "These instructions apply only during Companion calls. Your persona and regular chats stay unchanged.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )

                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Companion system prompt") },
                    minLines = 4,
                    maxLines = 8,
                    shape = RoundedCornerShape(16.dp),
                )

                Text(
                    text = "3D model",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentVrmUri?.lastPathSegment ?: "No VRM selected",
                        color = Color.White.copy(alpha = 0.68f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    VrmPicker(onVrmSelected = onVrmSelected) {
                        Text(if (currentVrmUri == null) "Choose VRM" else "Change")
                    }
                }

                Text(
                    text = "Voice",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (selectedVoice.gender == null) {
                    Text(
                        text = "Current voice: ${selectedVoice.label}. It isn’t categorized yet; choose a verified voice below to switch.",
                        color = Color.White.copy(alpha = 0.66f),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                    )
                }
                GeminiLiveVoiceGender.entries.forEach { gender ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = gender.name,
                            color = colors.accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            GeminiLiveVoice.entries.filter { it.gender == gender }.forEach { voice ->
                                FilterChip(
                                    selected = selectedVoice == voice,
                                    onClick = { onVoiceSelected(voice) },
                                    label = { Text(voice.label, maxLines = 1, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = colors.accent.copy(alpha = 0.2f),
                                        selectedLabelColor = colors.accent,
                                    ),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = { onSavePrompt(prompt); onDismiss() },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                    ) {
                        Text("Save prompt", color = Color.White)
                    }
                }
            }
        }
    }
}
