package com.yozora.aichat.ui

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.yozora.aichat.R
import com.yozora.aichat.ui.chat.ChatViewModel
import com.yozora.aichat.ui.chat.GeminiLiveVoice
import com.yozora.aichat.ui.companion.CompanionStage
import com.yozora.aichat.ui.companion.VrmPicker
import kotlinx.coroutines.delay

private data class CompanionCharacter(
    val sessionId: String,
    val name: String,
    val avatarUri: String?,
    val isGoogle: Boolean,
)

@Composable
private fun rememberCompanionCharacters(viewModel: ChatViewModel): List<CompanionCharacter> {
    val sessions = viewModel.sessions
    return remember(sessions) {
        sessions
            .filter { session -> session.members.any { it.persona.displayName.isNotBlank() } }
            .map { session ->
                val member = session.members.first { it.persona.displayName.isNotBlank() }
                val persona = member.persona
                CompanionCharacter(
                    sessionId = session.id,
                    name = persona.displayName,
                    avatarUri = session.headerAvatarUri?.toString() ?: persona.avatarUri?.toString(),
                    isGoogle = persona.vendor.id == "google",
                )
            }
    }
}

@Composable
internal fun CompanionHubScreen(
    viewModel: ChatViewModel,
    onNavigateToCreate: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("companion_vrm_prefs", Context.MODE_PRIVATE) }
    val colors = LocalRoleplayColors.current
    val characters = rememberCompanionCharacters(viewModel)

    var selectedIndex by rememberSaveable { mutableStateOf(0) }
    val safeIndex = selectedIndex.coerceIn(0, (characters.size - 1).coerceAtLeast(0))
    val selected = characters.getOrNull(safeIndex)
    val canStart = selected?.isGoogle == true

    // Persistent VRM per character session
    var selectedVrmUri by remember(selected?.sessionId) {
        val saved = selected?.sessionId?.let { prefs.getString("vrm_$it", null) }
        mutableStateOf(saved?.let { Uri.parse(it) })
    }
    var vrmError by remember { mutableStateOf<String?>(null) }

    val voices = GeminiLiveVoice.entries
    val selectedVoice = viewModel.selectedGeminiLiveVoice

    // Lip-sync calculation when call is active
    val isCallActive = viewModel.voiceCallActive && viewModel.voiceCallCompanionMode
    var mouthAa by remember { mutableStateOf(0f) }
    LaunchedEffect(isCallActive) {
        if (isCallActive) {
            var lastTime = System.currentTimeMillis()
            while (true) {
                val now = System.currentTimeMillis()
                val dt = (now - lastTime).toFloat()
                lastTime = now
                val head = viewModel.voiceCallPlaybackHeadFrames()
                mouthAa = viewModel.audioEnvelope.mouthAt(head, dt)
                delay(33L)
            }
        } else {
            mouthAa = 0f
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp),
    ) {
        // Character Carousel at Top
        if (characters.isEmpty()) {
            EmptyCharactersState(colors = colors, onNavigateToCreate = onNavigateToCreate)
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(characters) { index, char ->
                    CompanionCharacterCard(
                        character = char,
                        selected = index == safeIndex,
                        colors = colors,
                        onClick = {
                            selectedIndex = index
                        },
                    )
                }
            }
        }

        // 3D VRM Stage (Grok Ani style: visible directly on the tab!)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(colors.accent.copy(alpha = 0.04f))
                .border(1.dp, colors.accent.copy(alpha = 0.15f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center,
        ) {
            CompanionStage(
                modifier = Modifier.fillMaxSize(),
                vrmUri = selectedVrmUri,
                mouthAa = mouthAa,
                onError = { vrmError = it }
            )

            // Status chip overlay at top-start
            if (isCallActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.accent.copy(alpha = 0.85f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = viewModel.voiceCallStatus,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Settings & Action Controls
        if (selected != null) {
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selected.name,
                    color = colors.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )

                // VRM Choose / Change button
                VrmPicker(
                    onVrmSelected = { uri ->
                        selectedVrmUri = uri
                        vrmError = null
                        selected.sessionId.let {
                            prefs.edit().putString("vrm_$it", uri.toString()).apply()
                        }
                    },
                    onError = { vrmError = it },
                ) {
                    Text(
                        text = if (selectedVrmUri != null) "Change 3D" else "Pick 3D VRM",
                        fontSize = 12.sp,
                    )
                }
            }

            if (!selected.isGoogle) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.companion_not_google_hint),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }

            vrmError?.let { err ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            // Voice selection chips
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Voice",
                    color = colors.onSurface.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    modifier = Modifier.width(48.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(voices) { voice ->
                        val isSelected = voice == selectedVoice
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.updateGeminiLiveVoice(voice) },
                            label = { Text(voice.label, fontSize = 12.sp, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.accent.copy(alpha = 0.18f),
                                selectedLabelColor = colors.accent,
                            ),
                        )
                    }
                }
            }

            // Call Action Buttons (Call / End / Mute)
            Spacer(Modifier.height(20.dp))
            if (isCallActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = viewModel::toggleVoiceCallMute,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (viewModel.voiceCallMuted) colors.accent.copy(alpha = 0.2f) else colors.accent.copy(alpha = 0.12f),
                            contentColor = colors.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = if (viewModel.voiceCallMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (viewModel.voiceCallMuted) "Unmute" else "Mute", fontSize = 14.sp)
                    }

                    Button(
                        onClick = viewModel::closeVoiceCall,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CallEnd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("End Call", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                Button(
                    onClick = {
                        viewModel.selectSession(selected.sessionId)
                        viewModel.openVoiceCall(companion = true, vrmUri = selectedVrmUri)
                    },
                    enabled = canStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        disabledContainerColor = colors.onSurface.copy(alpha = 0.12f),
                    ),
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.companion_start_call),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun CompanionCharacterCard(
    character: CompanionCharacter,
    selected: Boolean,
    colors: RoleplayColors,
    onClick: () -> Unit,
) {
    val cardShape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .width(88.dp)
            .clip(cardShape)
            .background(colors.accent.copy(alpha = if (selected) 0.12f else 0.05f))
            .border(
                if (selected) 1.5.dp else 0.75.dp,
                if (selected) colors.accent else colors.onSurface.copy(alpha = 0.12f),
                cardShape
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(50.dp).clip(CircleShape).background(colors.onSurface.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            if (character.avatarUri != null) {
                AsyncImage(model = character.avatarUri, contentDescription = character.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Default.Person, contentDescription = null, tint = colors.onSurface.copy(alpha = 0.4f), modifier = Modifier.size(26.dp))
            }
            if (!character.isGoogle) {
                Box(
                    modifier = Modifier.align(Alignment.BottomEnd).size(16.dp).clip(CircleShape).background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center,
                ) { Text("🔒", fontSize = 8.sp) }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = character.name,
            color = if (character.isGoogle) colors.onSurface else colors.onSurface.copy(alpha = 0.45f),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EmptyCharactersState(colors: RoleplayColors, onNavigateToCreate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.Person, contentDescription = null, tint = colors.onSurface.copy(alpha = 0.2f), modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.companion_no_characters), color = colors.onSurface.copy(alpha = 0.5f), fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onNavigateToCreate) {
            Text("Create a character →", color = colors.accent, fontWeight = FontWeight.SemiBold)
        }
    }
}
