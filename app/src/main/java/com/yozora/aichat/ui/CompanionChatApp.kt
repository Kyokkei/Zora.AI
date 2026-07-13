package com.yozora.aichat.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.content.res.Configuration
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.activity.compose.BackHandler
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ScreenShare
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.rememberAsyncImagePainter
import com.yozora.aichat.R
import com.yozora.aichat.ui.chat.AnimeImagePreset
import com.yozora.aichat.ui.chat.AppUpdateState
import com.yozora.aichat.ui.chat.AppIconChoice
import com.yozora.aichat.ui.chat.AppNameChoice
import com.yozora.aichat.ui.chat.ApiVendor
import com.yozora.aichat.ui.chat.ChatBackground
import com.yozora.aichat.ui.chat.ChatMessage
import com.yozora.aichat.ui.chat.ChatSession
import com.yozora.aichat.ui.chat.ChatViewModel
import com.yozora.aichat.ui.chat.CreateDraftState
import com.yozora.aichat.ui.chat.GeminiLiveVoice
import com.yozora.aichat.ui.chat.GeminiThinkingEffort
import com.yozora.aichat.ui.chat.GroupMember
import com.yozora.aichat.ui.chat.HubCharacter
import com.yozora.aichat.ui.chat.ImportPreviewState
import com.yozora.aichat.ui.chat.InstructionMode
import com.yozora.aichat.ui.chat.LiveCallTranscriptLine
import com.yozora.aichat.ui.chat.PersonaUiState
import com.yozora.aichat.ui.chat.ProjectUiState
import com.yozora.aichat.ui.chat.QuotaUsageState
import com.yozora.aichat.ui.chat.SafetyLevel
import com.yozora.aichat.ui.chat.sessionLevelState
import com.yozora.aichat.ui.chat.TtsPreviewState
import com.yozora.aichat.ui.chat.VoiceCallCameraFacing
import com.yozora.aichat.ui.theme.AppAccent
import com.yozora.aichat.ui.theme.AppAccentDim
import com.yozora.aichat.ui.theme.AppAccentSoft
import com.yozora.aichat.ui.theme.AppBackground
import com.yozora.aichat.ui.theme.AppStroke
import com.yozora.aichat.ui.theme.AppSurface
import com.yozora.aichat.ui.theme.AppSurface2
import com.yozora.aichat.ui.theme.AppTextPrimary
import com.yozora.aichat.ui.theme.AppTextMuted
import com.yozora.aichat.ui.theme.AppTextSecondary
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import java.io.File
import java.io.ByteArrayOutputStream
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

private const val APP_VERSION_NAME = "2.2.5"
private const val APP_VERSION_CODE = 230

private fun Context.applyLanguageOverride(languageCode: String) {
    val locale = Locale.forLanguageTag(if (languageCode == "vi") "vi" else "en")
    Locale.setDefault(locale)
    val config = Configuration(resources.configuration)
    config.setLocales(android.os.LocaleList(locale))
    resources.updateConfiguration(config, resources.displayMetrics)
}

private enum class DrawerSection {
    Chats,
    Projects
}

private enum class PersonaSettingsSection(val label: String) {
    Context("Context"),
    Ui("UI"),
    ApiVault("API vault")
}

@Composable
fun CompanionChatApp(
    viewModel: ChatViewModel = viewModel()
) {
    var viewedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var actionMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var aboutDialogVisible by remember { mutableStateOf(false) }
    val appVersionLabel = "${viewModel.appNameChoice.label} v$APP_VERSION_NAME ($APP_VERSION_CODE)"
    var activeRoleplayTab by remember { mutableStateOf(RoleplayTab.Discover) }
    var activeChatOpen by remember { mutableStateOf(false) }
    var pendingConfigExportSession by remember { mutableStateOf<ChatSession?>(null) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel.languageCode) {
        context.applyLanguageOverride(viewModel.languageCode)
    }
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshUpdateStatusAfterResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    val clipboardManager = LocalClipboardManager.current
    val voicePermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.openVoiceCall()
        } else {
            viewModel.handleVoicePermissionDenied()
        }
    }
    val cameraPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.enableVoiceCallVideo()
        }
    }
    val screenShareLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            viewModel.startVoiceCallScreenShare(result.resultCode, data)
        }
    }
    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importSessionFromUri(uri)
        }
    }
    val importConfigLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importConfigFromUri(context, uri)
        }
    }
    val exportConfigFolderLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        val session = pendingConfigExportSession
        pendingConfigExportSession = null
        if (uri != null && session != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            viewModel.setDefaultExportFolder(uri)
            viewModel.exportConfigOnly(
                session = session,
                folderUri = uri,
                onNeedsFolder = { pendingConfigExportSession = session },
                onSuccess = { path ->
                    android.widget.Toast.makeText(context, "Saved: $path", android.widget.Toast.LENGTH_LONG).show()
                },
                onFailure = { err ->
                    android.widget.Toast.makeText(context, "Export failed: $err", android.widget.Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    if (viewModel.voiceCallActive) {
        BackHandler(onBack = viewModel::closeVoiceCall)
        VoiceCallScreen(
            persona = viewModel.persona,
            isOnline = viewModel.activeApiKeyLabel != null,
            status = viewModel.voiceCallStatus,
            error = viewModel.voiceCallError,
            modelId = viewModel.geminiLiveModelId,
            selectedVoice = viewModel.selectedGeminiLiveVoice,
            muted = viewModel.voiceCallMuted,
            videoEnabled = viewModel.voiceCallVideoEnabled,
            cameraFacing = viewModel.voiceCallCameraFacing,
            screenShareEnabled = viewModel.voiceCallScreenShareEnabled,
            transcriptLines = viewModel.voiceCallTranscriptLines,
            onCameraFrame = viewModel::sendVoiceCallVideoFrame,
            onBack = viewModel::closeVoiceCall,
            onEndCall = viewModel::closeVoiceCall,
            onRetry = viewModel::retryVoiceCall,
            onVoiceChange = viewModel::updateGeminiLiveVoice,
            onToggleMute = viewModel::toggleVoiceCallMute,
            onToggleVideo = {
                if (viewModel.voiceCallVideoEnabled) {
                    viewModel.disableVoiceCallVideo()
                } else {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        viewModel.enableVoiceCallVideo()
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            },
            onSwitchCamera = viewModel::switchVoiceCallCamera,
            onToggleScreenShare = {
                if (viewModel.voiceCallScreenShareEnabled) {
                    viewModel.stopVoiceCallScreenShare()
                } else {
                    val manager = context.getSystemService(MediaProjectionManager::class.java)
                    val captureIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        manager.createScreenCaptureIntent(
                            MediaProjectionConfig.createConfigForDefaultDisplay()
                        )
                    } else {
                        manager.createScreenCaptureIntent()
                    }
                    screenShareLauncher.launch(captureIntent)
                }
            }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        if (viewModel.roleplayUiModeEnabled) {
            if (activeChatOpen) {
                ChatScreen(
                    persona = viewModel.persona,
                    groupMembers = viewModel.groupMembers,
                    sessionHeaderName = viewModel.sessionHeaderName,
                    sessionHeaderAvatarUri = viewModel.sessionHeaderAvatarUri,
                    sessionHeaderAvatarScale = viewModel.sessionHeaderAvatarScale,
                    sessionHeaderAvatarOffsetX = viewModel.sessionHeaderAvatarOffsetX,
                    sessionHeaderAvatarOffsetY = viewModel.sessionHeaderAvatarOffsetY,
                    background = viewModel.background,
                    messages = viewModel.messages,
                    draft = viewModel.draft,
                    isOnline = viewModel.activeApiKeyLabel != null,
                    isSending = viewModel.isSending,
                    chatError = viewModel.chatError,
                    attachedImageUris = viewModel.attachedImageUris,
                    webSearchEnabled = viewModel.webSearchEnabled,
                    canUseWebSearch = viewModel.canUseWebSearch,
                    animeImageModeEnabled = viewModel.animeImageModeEnabled,
                    animeImagePreset = viewModel.animeImagePreset,
                    onDraftChange = viewModel::updateDraft,
                    onSend = viewModel::sendDraft,
                    onAttachImages = viewModel::attachImages,
                    onRemoveAttachedImage = viewModel::removeAttachedImage,
                    onWebSearchChange = viewModel::updateWebSearchEnabled,
                    onAnimeImageModeChange = viewModel::updateAnimeImageModeEnabled,
                    onAnimeImagePresetChange = viewModel::updateAnimeImagePreset,
                    onOpenImage = { viewedImageUri = it },
                    onMessageLongPress = { actionMessage = it },
                    onCopyMessage = { message ->
                        val copyText = message.content.ifBlank {
                            message.remoteImageUrl ?: message.imageUris.joinToString("\n") { it.toString() }
                        }
                        clipboardManager.setText(AnnotatedString(copyText))
                    },
                    onEditMessage = viewModel::beginEditMessage,
                    onRetryMessage = viewModel::retryMessage,
                    onSpeakMessage = viewModel::speakMessage,
                    onOpenSessions = { activeChatOpen = false },
                    onOpenPersona = viewModel::openPersonaSheet,
                    onOpenVoiceCall = {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (granted) {
                            viewModel.openVoiceCall()
                        } else {
                            voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onClearChatError = viewModel::clearChatError,
                    isRoleplayMode = true
                )
            } else {
                RoleplayHubLayout(
                    viewModel = viewModel,
                    activeTab = activeRoleplayTab,
                    onTabChange = { activeRoleplayTab = it },
                    onOpenChat = { activeChatOpen = true },
                    onImportSession = { importLauncher.launch(arrayOf("application/octet-stream")) },
                    onImportConfig = { importConfigLauncher.launch(arrayOf("application/octet-stream")) }
                )
            }
        } else {
            ChatScreen(
                persona = viewModel.persona,
                groupMembers = viewModel.groupMembers,
                sessionHeaderName = viewModel.sessionHeaderName,
                sessionHeaderAvatarUri = viewModel.sessionHeaderAvatarUri,
                sessionHeaderAvatarScale = viewModel.sessionHeaderAvatarScale,
                sessionHeaderAvatarOffsetX = viewModel.sessionHeaderAvatarOffsetX,
                sessionHeaderAvatarOffsetY = viewModel.sessionHeaderAvatarOffsetY,
                background = viewModel.background,
                messages = viewModel.messages,
                draft = viewModel.draft,
                isOnline = viewModel.activeApiKeyLabel != null,
                isSending = viewModel.isSending,
                chatError = viewModel.chatError,
                attachedImageUris = viewModel.attachedImageUris,
                webSearchEnabled = viewModel.webSearchEnabled,
                canUseWebSearch = viewModel.canUseWebSearch,
                animeImageModeEnabled = viewModel.animeImageModeEnabled,
                animeImagePreset = viewModel.animeImagePreset,
                onDraftChange = viewModel::updateDraft,
                onSend = viewModel::sendDraft,
                onAttachImages = viewModel::attachImages,
                onRemoveAttachedImage = viewModel::removeAttachedImage,
                onWebSearchChange = viewModel::updateWebSearchEnabled,
                onAnimeImageModeChange = viewModel::updateAnimeImageModeEnabled,
                onAnimeImagePresetChange = viewModel::updateAnimeImagePreset,
                onOpenImage = { viewedImageUri = it },
                onMessageLongPress = { actionMessage = it },
                onCopyMessage = { message ->
                    val copyText = message.content.ifBlank {
                        message.remoteImageUrl ?: message.imageUris.joinToString("\n") { it.toString() }
                    }
                    clipboardManager.setText(AnnotatedString(copyText))
                },
                onEditMessage = viewModel::beginEditMessage,
                onRetryMessage = viewModel::retryMessage,
                onSpeakMessage = viewModel::speakMessage,
                onOpenSessions = viewModel::openSessionDrawer,
                onOpenPersona = viewModel::openPersonaSheet,
                onOpenVoiceCall = {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        viewModel.openVoiceCall()
                    } else {
                        voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onClearChatError = viewModel::clearChatError,
                isRoleplayMode = false
            )
        }

        AnimatedVisibility(
            visible = viewModel.sessionDrawerVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.56f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = viewModel::closeSessionDrawer
                    )
            )
        }

        AnimatedVisibility(
            visible = viewModel.sessionDrawerVisible,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it }),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            SessionDrawer(
                sessions = viewModel.sessions,
                projects = viewModel.projects,
                activeSessionId = viewModel.activeSessionId,
                activeProject = viewModel.activeProject,
                persona = viewModel.persona,
                appVersionLabel = appVersionLabel,
                onNewSession = { viewModel.createSession() },
                onNewProject = viewModel::createProject,
                onSelectSession = viewModel::selectSession,
                onNewSessionInProject = viewModel::createSessionInProject,
                onMoveSessionToProject = viewModel::moveSessionToProject,
                onUpdateProjectName = viewModel::updateProjectName,
                onUpdateProjectDescription = viewModel::updateProjectDescription,
                onUpdateProjectInstruction = viewModel::updateProjectInstruction,
                onDeleteProject = viewModel::deleteProject,
                onDeleteSession = viewModel::deleteSession,
                onRenameSession = viewModel::renameSession,
                onCloneSession = viewModel::cloneSession,
                onDuplicateSessionSettings = viewModel::duplicateSessionSettings,
                onImportSession = { importLauncher.launch(arrayOf("application/octet-stream")) },
                onImportConfig = { importConfigLauncher.launch(arrayOf("application/octet-stream")) },
                onOpenSettings = viewModel::openAppSettings,
                onOpenAbout = { aboutDialogVisible = true },
                onClose = viewModel::closeSessionDrawer,
                onExportSession = { session ->
                    viewModel.autoExportSession(
                        sessionId = session.id,
                        onSuccess = { path ->
                            android.widget.Toast.makeText(context, "Saved: $path", android.widget.Toast.LENGTH_LONG).show()
                        },
                        onFailure = { err ->
                            android.widget.Toast.makeText(context, "Export failed: $err", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onExportConfigSession = { session ->
                    viewModel.exportConfigOnly(
                        session = session,
                        onNeedsFolder = {
                            pendingConfigExportSession = session
                            exportConfigFolderLauncher.launch(null)
                        },
                        onSuccess = { path ->
                            android.widget.Toast.makeText(context, "Saved: $path", android.widget.Toast.LENGTH_LONG).show()
                        },
                        onFailure = { err ->
                            android.widget.Toast.makeText(context, "Export failed: $err", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                onLongPressSession = {}
            )
        }

        AnimatedVisibility(
            visible = viewModel.personaSheetVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.58f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = viewModel::closePersonaSheet
                    )
            )
        }

        AnimatedVisibility(
            visible = viewModel.personaSheetVisible,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it }),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            PersonaSettingsSheet(
                persona = viewModel.persona,
                groupMembers = viewModel.groupMembers,
                showSessionHeaderControls = viewModel.showSessionHeaderControls,
                sessionHeaderName = viewModel.sessionHeaderName,
                sessionHeaderAvatarUri = viewModel.sessionHeaderAvatarUri,
                sessionHeaderAvatarScale = viewModel.sessionHeaderAvatarScale,
                sessionHeaderAvatarOffsetX = viewModel.sessionHeaderAvatarOffsetX,
                sessionHeaderAvatarOffsetY = viewModel.sessionHeaderAvatarOffsetY,
                activeMemberId = viewModel.activeMemberId,
                responseRounds = viewModel.responseRounds,
                memoryEnabled = viewModel.memoryEnabled,
                storyLore = viewModel.storyLore,
                levelSystemEnabled = viewModel.levelSystemEnabled,
                levelXp = viewModel.levelXp,
                background = viewModel.background,
                moreOptions = viewModel.morePersonaOptions,
                activeApiKeyLabel = viewModel.activeApiKeyLabel,
                tavilyApiKeyLabel = viewModel.tavilyApiKeyLabel,
                rule34UserIdLabel = viewModel.rule34UserIdLabel,
                rule34ApiKeyLabel = viewModel.rule34ApiKeyLabel,
                elevenLabsApiKeyLabel = viewModel.elevenLabsApiKeyLabel,
                elevenLabsVoiceIdLabel = viewModel.elevenLabsVoiceIdLabel,
                elevenLabsModelIdLabel = viewModel.elevenLabsModelIdLabel,
                summarizerUsesSeparateKey = viewModel.summarizerUsesSeparateKey,
                summarizerApiKeyLabel = viewModel.summarizerApiKeyLabel,
                quotaUsage = viewModel.quotaUsage,
                dailyRequestLimit = viewModel.dailyRequestLimit,
                onBack = viewModel::closePersonaSheet,
                onVendorChange = viewModel::updateVendor,
                onSelectMember = viewModel::selectGroupMember,
                onAddMember = viewModel::addGroupMember,
                onRemoveMember = viewModel::removeGroupMember,
                onResponseRoundsChange = viewModel::updateResponseRounds,
                onMemoryEnabledChange = viewModel::updateMemoryEnabled,
                onLevelSystemEnabledChange = viewModel::updateLevelSystemEnabled,
                onNameChange = viewModel::updatePersonaName,
                onAuthorChange = viewModel::updateAuthor,
                onTaglineChange = viewModel::updateTagline,
                onTraitsChange = viewModel::updateTraits,
                onSessionHeaderNameChange = viewModel::updateSessionHeaderName,
                onInstructionModeChange = viewModel::updateInstructionMode,
                onBeginnerRoleChange = viewModel::updateBeginnerRole,
                onBeginnerStyleChange = viewModel::updateBeginnerStyle,
                onBeginnerLimitsChange = viewModel::updateBeginnerLimits,
                onStoryLoreChange = viewModel::updateStoryLore,
                onPromptChange = viewModel::updatePersonaPrompt,
                onModelChange = viewModel::updatePersonaModel,
                onSafetyLevelChange = viewModel::updateSafetyLevel,
                onThinkingEffortChange = viewModel::updateThinkingEffort,
                onTemperatureChange = viewModel::updateTemperature,
                onAvatarChange = viewModel::updateAvatar,
                onAvatarTransform = viewModel::transformAvatar,
                onSessionHeaderAvatarChange = viewModel::updateSessionHeaderAvatar,
                onSessionHeaderAvatarTransform = viewModel::transformSessionHeaderAvatar,
                onBackgroundChange = viewModel::updateBackground,
                onCustomBackgroundChange = viewModel::updateCustomBackground,
                onToggleMore = viewModel::toggleMorePersonaOptions,
                onEditApiKey = viewModel::openApiKeyDialog,
                onClearApiKey = viewModel::clearApiKey,
                onEditTavilyKey = viewModel::openTavilyApiKeyDialog,
                onClearTavilyKey = viewModel::clearTavilyApiKey,
                onEditRule34UserId = viewModel::openRule34UserIdDialog,
                onClearRule34UserId = viewModel::clearRule34UserId,
                onEditRule34ApiKey = viewModel::openRule34ApiKeyDialog,
                onClearRule34ApiKey = viewModel::clearRule34ApiKey,
                onEditElevenLabsKey = viewModel::openElevenLabsApiKeyDialog,
                onClearElevenLabsKey = viewModel::clearElevenLabsApiKey,
                onEditElevenLabsVoiceId = viewModel::openElevenLabsVoiceIdDialog,
                onClearElevenLabsVoiceId = viewModel::clearElevenLabsVoiceId,
                onEditElevenLabsModelId = viewModel::openElevenLabsModelIdDialog,
                onClearElevenLabsModelId = viewModel::clearElevenLabsModelId,
                onSummarizerKeyModeChange = viewModel::updateSummarizerUsesSeparateKey,
                onEditSummarizerKey = viewModel::openSummarizerApiKeyDialog,
                onClearSummarizerKey = viewModel::clearSummarizerApiKey,
                onTriggerAutoFill = { viewModel.openAutoFillDialog() },
                onDeleteSession = viewModel::deleteActiveSession,
                onShareConfig = { viewModel.shareActiveConfig(context) },
                onSave = viewModel::savePersona
            )
        }

    }

    if (viewModel.apiKeyDialogVisible) {
        ApiKeyDialog(
            title = viewModel.apiKeyDialogTitle,
            value = viewModel.apiKeyDraft,
            onValueChange = viewModel::updateApiKeyDraft,
            onDismiss = viewModel::closeApiKeyDialog,
            onSave = viewModel::saveApiKey,
            secureInput = viewModel.apiKeyDialogSecure
        )
    }

    if (viewModel.appSettingsVisible) {
        AppSettingsDialog(
            selectedName = viewModel.appNameChoice,
            selectedIcon = viewModel.appIconChoice,
            nsfwModeEnabled = viewModel.nsfwModeEnabled,
            roleplayUiModeEnabled = viewModel.roleplayUiModeEnabled,
            useLightColors = viewModel.roleplayLightModeEnabled,
            languageCode = viewModel.languageCode,
            onNameChange = viewModel::updateAppName,
            onIconChange = viewModel::updateAppIcon,
            onNsfwModeChange = viewModel::updateNsfwModeEnabled,
            onRoleplayUiModeChange = viewModel::updateRoleplayUiModeEnabled,
            onLanguageChange = viewModel::updateLanguage,
            onDismiss = viewModel::closeAppSettings
        )
    }

    AutoFillPromptDialog(
        visible = viewModel.autoFillDialogVisible,
        isGenerating = viewModel.isAutoFilling,
        onDismiss = { viewModel.closeAutoFillDialog() },
        onConfirm = { viewModel.autoFillWithAI(it) }
    )

    viewedImageUri?.let { uri ->
        FullscreenImageViewer(
            uri = uri,
            onDismiss = { viewedImageUri = null }
        )
    }

    actionMessage?.let { message ->
        MessageActionSheet(
            message = message,
            onDismiss = { actionMessage = null },
            onCopy = {
                val copyText = message.content.ifBlank {
                    message.remoteImageUrl ?: message.imageUris.joinToString("\n") { it.toString() }
                }
                clipboardManager.setText(AnnotatedString(copyText))
                actionMessage = null
            },
            onEdit = {
                viewModel.beginEditMessage(message.id)
                actionMessage = null
            },
            onRetry = {
                viewModel.retryMessage(message.id)
                actionMessage = null
            },
            onSpeak = {
                viewModel.speakMessage(message)
                actionMessage = null
            }
        )
    }

    viewModel.ttsPreviewState?.let { preview ->
        SpeechPreviewSheet(
            state = preview,
            onTextChange = viewModel::updateTtsPreviewText,
            onDismiss = viewModel::dismissTtsPreview,
            onGenerate = viewModel::generateAndPlayTtsPreview
        )
    }

    viewModel.importPreviewState?.let { preview ->
        ImportPreviewSheet(
            state = preview,
            onDismiss = viewModel::dismissImportPreview,
            onImport = { viewModel.confirmImportPreview(asCopy = false) },
            onImportAsCopy = { viewModel.confirmImportPreview(asCopy = true) }
        )
    }

    if (aboutDialogVisible) {
        AboutAppDialog(
            appVersionLabel = appVersionLabel,
            onDismiss = { aboutDialogVisible = false }
        )
    }

    if (viewModel.rateLimitDialogVisible) {
        RateLimitRetryDialog(
            onDismiss = viewModel::dismissRateLimitDialog,
            onRetry = viewModel::retryAfterRateLimit
        )
    }

    when {
        viewModel.appSettingsVisible -> BackHandler(onBack = viewModel::closeAppSettings)
        aboutDialogVisible -> BackHandler { aboutDialogVisible = false }
        viewModel.rateLimitDialogVisible -> BackHandler(onBack = viewModel::dismissRateLimitDialog)
        viewModel.ttsPreviewState != null -> BackHandler(onBack = viewModel::dismissTtsPreview)
        actionMessage != null -> BackHandler { actionMessage = null }
        viewModel.personaSheetVisible -> BackHandler(onBack = viewModel::closePersonaSheet)
        viewModel.sessionDrawerVisible -> BackHandler(onBack = viewModel::closeSessionDrawer)
    }
}

@Composable
private fun ChatScreen(
    persona: PersonaUiState,
    groupMembers: List<GroupMember>,
    sessionHeaderName: String,
    sessionHeaderAvatarUri: android.net.Uri?,
    sessionHeaderAvatarScale: Float,
    sessionHeaderAvatarOffsetX: Float,
    sessionHeaderAvatarOffsetY: Float,
    background: ChatBackground,
    messages: List<ChatMessage>,
    draft: String,
    isOnline: Boolean,
    isSending: Boolean,
    chatError: String?,
    attachedImageUris: List<android.net.Uri>,
    webSearchEnabled: Boolean,
    canUseWebSearch: Boolean,
    animeImageModeEnabled: Boolean,
    animeImagePreset: AnimeImagePreset,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachImages: (List<android.net.Uri>) -> Unit,
    onRemoveAttachedImage: (android.net.Uri) -> Unit,
    onWebSearchChange: (Boolean) -> Unit,
    onAnimeImageModeChange: (Boolean) -> Unit,
    onAnimeImagePresetChange: (AnimeImagePreset) -> Unit,
    onOpenImage: (android.net.Uri) -> Unit,
    onMessageLongPress: (ChatMessage) -> Unit,
    onCopyMessage: (ChatMessage) -> Unit,
    onEditMessage: (String) -> Unit,
    onRetryMessage: (String) -> Unit,
    onSpeakMessage: (ChatMessage) -> Unit,
    onOpenSessions: () -> Unit,
    onOpenPersona: () -> Unit,
    onOpenVoiceCall: () -> Unit,
    onClearChatError: () -> Unit,
    isRoleplayMode: Boolean = false
) {
    var toolsSheetVisible by remember { mutableStateOf(false) }
    var inlineActionMessageId by remember { mutableStateOf<String?>(null) }
    var cameraOutputUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val photoPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents(),
        onResult = onAttachImages
    )
    val filePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments(),
        onResult = onAttachImages
    )
    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicture(),
        onResult = { saved ->
            if (saved) {
                cameraOutputUri?.let { onAttachImages(listOf(it)) }
            }
            cameraOutputUri = null
        }
    )
    val hasChatContent = messages.isNotEmpty() || isSending
    val mentionTokenStart = draft.indexOfLast { it.isWhitespace() } + 1
    val mentionToken = draft.substring(mentionTokenStart)
    val mentionQuery = mentionToken.takeIf { it.startsWith("@") }?.drop(1).orEmpty()
    val mentionCandidates = if (groupMembers.size > 1 && mentionToken.startsWith("@")) {
        groupMembers.filter { member ->
            member.persona.displayName.contains(mentionQuery, ignoreCase = true)
        }
    } else {
        emptyList()
    }
    val showJumpToLatest by remember {
        derivedStateOf {
            hasChatContent &&
                (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 24)
        }
    }
    fun displayIndexForMessage(originalIndex: Int): Int {
        val typingOffset = if (isSending) 1 else 0
        return typingOffset + (messages.lastIndex - originalIndex).coerceAtLeast(0)
    }
    val topAnchorIndex = if (messages.isNotEmpty()) displayIndexForMessage(0) else 0
    val bottomAnchorIndex = 0
    suspend fun scrollDisplayIndexToTop(index: Int) {
        val totalItems = listState.layoutInfo.totalItemsCount
        if (totalItems <= 0) return
        val safeIndex = index.coerceIn(0, totalItems - 1)
        if (abs(listState.firstVisibleItemIndex - safeIndex) > 12) {
            listState.scrollToItem(safeIndex)
        } else {
            listState.animateScrollToItem(safeIndex)
        }
        yield()
        var itemInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == safeIndex }
        if (itemInfo == null) {
            listState.scrollToItem(safeIndex)
            yield()
            itemInfo = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == safeIndex }
        }
        itemInfo ?: return
        val deltaToTop = itemInfo.offset - listState.layoutInfo.viewportStartOffset
        if (abs(deltaToTop) > 1) {
            listState.scrollBy(deltaToTop.toFloat())
        }
    }
    suspend fun scrollToNewest() {
        if (!hasChatContent) return
        if (listState.firstVisibleItemIndex > 10) {
            listState.scrollToItem(bottomAnchorIndex)
        } else {
            listState.animateScrollToItem(bottomAnchorIndex)
        }
    }
    suspend fun scrollToOldest() {
        if (!hasChatContent) return
        scrollDisplayIndexToTop(topAnchorIndex)
    }

    LaunchedEffect(messages.lastOrNull()?.id, isSending) {
        if (hasChatContent) {
            scrollToNewest()
        }
    }
    val startedTypewriterMessageIds = remember { mutableSetOf<String>() }
    val newestTextModelMessageId = messages.lastOrNull { message ->
        message.role == "model" &&
            message.content.isNotBlank() &&
            !message.isImageLoading &&
            message.remoteImageUrl == null
    }?.id
    val displayMessages = remember(messages) { messages.asReversed() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        ChatBackgroundLayer(background = background)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            ChatHeader(
                persona = persona,
                groupMembers = groupMembers,
                sessionHeaderName = sessionHeaderName,
                sessionHeaderAvatarUri = sessionHeaderAvatarUri,
                sessionHeaderAvatarScale = sessionHeaderAvatarScale,
                sessionHeaderAvatarOffsetX = sessionHeaderAvatarOffsetX,
                sessionHeaderAvatarOffsetY = sessionHeaderAvatarOffsetY,
                isOnline = isOnline,
                onOpenSessions = onOpenSessions,
                onOpenPersona = onOpenPersona,
                onOpenVoiceCall = onOpenVoiceCall,
                isRoleplayMode = isRoleplayMode
            )
            LazyColumn(
                state = listState,
                reverseLayout = messages.isNotEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        EmptyChatState(
                            persona = persona,
                            onCreatePersona = onOpenPersona,
                            onSuggestion = onDraftChange
                        )
                    }
                } else {
                    if (isSending) {
                        item(key = "typing") {
                            TypingBubble(persona = persona)
                        }
                    }
                    itemsIndexed(displayMessages, key = { _, message -> message.id }) { index, message ->
                        val newerMessage = displayMessages.getOrNull(index - 1)
                        val olderMessage = displayMessages.getOrNull(index + 1)
                        val sameAsNewer = newerMessage?.sameBubbleActor(message) == true
                        val sameAsOlder = olderMessage?.sameBubbleActor(message) == true
                        AnimatedMessageBubble(
                            message = message,
                            persona = persona,
                            groupMembers = groupMembers,
                            showAvatar = !sameAsNewer,
                            showSpeakerLabel = !sameAsOlder,
                            compactTop = sameAsOlder,
                            compactBottom = sameAsNewer,
                            actionsVisible = inlineActionMessageId == message.id,
                            animateText = message.id == newestTextModelMessageId &&
                                message.id !in startedTypewriterMessageIds,
                            onTextAnimationStart = { startedTypewriterMessageIds += it },
                            onClick = {
                                inlineActionMessageId = if (inlineActionMessageId == message.id) null else message.id
                            },
                            onOpenImage = onOpenImage,
                            onMessageLongPress = onMessageLongPress,
                            onCopy = {
                                onCopyMessage(message)
                                inlineActionMessageId = null
                            },
                            onEdit = {
                                onEditMessage(message.id)
                                inlineActionMessageId = null
                            },
                            onRetry = {
                                onRetryMessage(message.id)
                                inlineActionMessageId = null
                            },
                            onSpeak = {
                                onSpeakMessage(message)
                                inlineActionMessageId = null
                            }
                        )
                    }
                }
            }
            if (chatError != null) {
                ChatErrorLine(message = chatError, onDismiss = onClearChatError)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(bottom = 10.dp)
            ) {
                AnimatedVisibility(
                    visible = mentionCandidates.isNotEmpty(),
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut()
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(mentionCandidates, key = { it.id }) { member ->
                            Surface(
                                color = AppSurface2,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, AppStroke),
                                modifier = Modifier.clickable {
                                    val prefix = draft.substring(0, mentionTokenStart)
                                    onDraftChange("$prefix@${member.persona.displayName} ")
                                }
                            ) {
                                Text(
                                    text = "@${member.persona.displayName}",
                                    color = AppAccentSoft,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
                ChatInputBar(
                    placeholder = "Message ${persona.displayName.ifBlank { "New Persona" }}...",
                    draft = draft,
                    isSending = isSending,
                    attachedImageUris = attachedImageUris,
                    webSearchEnabled = webSearchEnabled,
                    animeImageModeEnabled = animeImageModeEnabled,
                    animeImagePreset = animeImagePreset,
                    onDraftChange = onDraftChange,
                    onSend = {
                        focusManager.clearFocus()
                        onSend()
                    },
                    onOpenTools = {
                        focusManager.clearFocus(force = true)
                        toolsSheetVisible = true
                    },
                    onVoiceInput = onOpenVoiceCall,
                    onRemoveAttachedImage = onRemoveAttachedImage,
                    onOpenImage = onOpenImage
                )
            }
        }
        AnimatedVisibility(
            visible = showJumpToLatest,
            enter = fadeIn() + scaleIn(initialScale = 0.88f),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .imePadding()
                .navigationBarsPadding()
                .padding(end = 22.dp, bottom = 104.dp)
        ) {
            Surface(
                color = AppAccent,
                shape = CircleShape,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(46.dp)
                    .clickable {
                        coroutineScope.launch {
                            if (hasChatContent) {
                                scrollToNewest()
                            }
                        }
                    }
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Jump to latest message",
                    tint = Color.White,
                    modifier = Modifier.padding(9.dp)
                )
            }
        }
    }

    AnimatedVisibility(
        visible = toolsSheetVisible,
        enter = fadeIn(animationSpec = tween(150)) + slideInVertically(
            animationSpec = tween(220),
            initialOffsetY = { it / 4 }
        ),
        exit = fadeOut(animationSpec = tween(120)) + slideOutVertically(
            animationSpec = tween(180),
            targetOffsetY = { it / 4 }
        )
    ) {
        ChatToolsSheet(
            onDismiss = { toolsSheetVisible = false },
            onScrollTop = {
                toolsSheetVisible = false
                coroutineScope.launch {
                    delay(190)
                    scrollToOldest()
                }
            },
            onScrollBottom = {
                toolsSheetVisible = false
                coroutineScope.launch {
                    delay(190)
                    scrollToNewest()
                }
            },
            onCamera = {
                val uri = createCameraImageUri(context)
                cameraOutputUri = uri
                toolsSheetVisible = false
                cameraLauncher.launch(uri)
            },
            onPhotos = {
                toolsSheetVisible = false
                photoPicker.launch("image/*")
            },
            onFiles = {
                toolsSheetVisible = false
                filePicker.launch(arrayOf("image/*"))
            },
            messages = messages,
            onJumpToMessage = { index ->
                toolsSheetVisible = false
                coroutineScope.launch {
                    delay(190)
                    listState.animateScrollToItem(displayIndexForMessage(index))
                }
            },
            webSearchEnabled = webSearchEnabled,
            webSearchAvailable = canUseWebSearch,
            onWebSearchChange = onWebSearchChange,
            animeImageModeEnabled = animeImageModeEnabled,
            animeImagePreset = animeImagePreset,
            onAnimeImageModeChange = onAnimeImageModeChange,
            onAnimeImagePresetChange = onAnimeImagePresetChange
        )
    }

    if (toolsSheetVisible) {
        BackHandler { toolsSheetVisible = false }
    }
}

@Composable
private fun VoiceCallScreen(
    persona: PersonaUiState,
    isOnline: Boolean,
    status: String,
    error: String?,
    modelId: String,
    selectedVoice: GeminiLiveVoice,
    muted: Boolean,
    videoEnabled: Boolean,
    cameraFacing: VoiceCallCameraFacing,
    screenShareEnabled: Boolean,
    transcriptLines: List<LiveCallTranscriptLine>,
    onCameraFrame: (ByteArray) -> Unit,
    onBack: () -> Unit,
    onEndCall: () -> Unit,
    onRetry: () -> Unit,
    onVoiceChange: (GeminiLiveVoice) -> Unit,
    onToggleMute: () -> Unit,
    onToggleVideo: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleScreenShare: () -> Unit
) {
    var voiceMenuExpanded by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableStateOf(0L) }
    val startedAt = remember { System.currentTimeMillis() }
    val compactLayout = LocalConfiguration.current.screenHeightDp < 760
    val avatarOuterSize = if (compactLayout) 112 else 148
    val avatarImageSize = if (compactLayout) 92 else 122

    LaunchedEffect(startedAt) {
        while (true) {
            elapsedSeconds = (System.currentTimeMillis() - startedAt) / 1000L
            delay(1000L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .background(
                Brush.radialGradient(
                    colors = listOf(AppAccent.copy(alpha = 0.24f), AppBackground, Color.Black),
                    radius = 980f
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back to chat",
                        tint = AppTextPrimary
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = persona.displayName.ifBlank { "New Persona" },
                        color = AppTextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) Color(0xFF35D07F) else Color(0xFF8B8898))
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = "AI Voice Call",
                            color = AppTextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Box {
                    IconButton(onClick = { voiceMenuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Voice settings",
                            tint = AppTextPrimary
                        )
                    }
                    DropdownMenu(
                        expanded = voiceMenuExpanded,
                        onDismissRequest = { voiceMenuExpanded = false },
                        modifier = Modifier.background(AppSurface2)
                    ) {
                        GeminiLiveVoice.entries.forEach { voice ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = voice.label,
                                            color = AppTextPrimary,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                        Text(
                                            text = voice.description,
                                            color = AppTextSecondary,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                },
                                onClick = {
                                    onVoiceChange(voice)
                                    voiceMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = if (compactLayout) 2.dp else 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.VolumeUp,
                    contentDescription = null,
                    tint = AppAccentSoft,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Gemini 3 Flash Live - ${formatCallDuration(elapsedSeconds)}",
                    color = AppTextPrimary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Text(
                text = modelId,
                color = AppTextSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 3.dp)
            )

            Spacer(modifier = Modifier.height(if (compactLayout) 8.dp else 14.dp))
            if (videoEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .heightIn(max = if (compactLayout) 170.dp else 220.dp)
                        .aspectRatio(4f / 3f)
                ) {
                    LiveCameraPreview(
                        cameraFacing = cameraFacing,
                        onFrame = onCameraFrame,
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, AppAccentSoft.copy(alpha = 0.82f), RoundedCornerShape(24.dp))
                    )
                    Surface(
                        color = AppSurface.copy(alpha = 0.78f),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, AppStroke),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        IconButton(onClick = onSwitchCamera) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Switch camera",
                                tint = AppTextPrimary
                            )
                        }
                    }
                    Surface(
                        color = AppSurface.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, AppStroke.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = if (cameraFacing == VoiceCallCameraFacing.Front) "Front camera" else "Back camera",
                            color = AppTextPrimary,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                        )
                    }
                }
            } else {
                VoiceAvatar(
                    persona = persona,
                    outerSize = avatarOuterSize,
                    imageSize = avatarImageSize
                )
            }

            Spacer(modifier = Modifier.height(if (compactLayout) 10.dp else 18.dp))
            val callDisplayName = persona.displayName.ifBlank { "New Persona" }
            Text(
                text = buildAnnotatedString {
                    val splitIndex = callDisplayName.lastIndexOf(' ')
                    if (splitIndex > 0) {
                        append(callDisplayName.substring(0, splitIndex))
                        append(" ")
                        withStyle(SpanStyle(color = AppAccentSoft, fontWeight = FontWeight.Bold)) {
                            append(callDisplayName.substring(splitIndex + 1))
                        }
                    } else {
                        withStyle(SpanStyle(color = AppAccentSoft, fontWeight = FontWeight.Bold)) {
                            append(callDisplayName)
                        }
                    }
                },
                color = AppTextPrimary,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isOnline) Color(0xFF35D07F) else Color(0xFF8B8898))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isOnline) "Online" else "Offline",
                    color = AppTextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            VoiceWaveform(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 8.dp)
                    .height(if (compactLayout) 38.dp else 48.dp)
            )

            LiveCallTranscriptPanel(
                lines = transcriptLines,
                status = status.takeIf { it.isNotBlank() && it != "Idle" }
                    ?: if (muted) "Muted" else "Listening...",
                voiceName = selectedVoice.label,
                error = error,
                onRetry = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 4.dp, bottom = 10.dp)
            )
            Surface(
                color = AppSurface.copy(alpha = 0.86f),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, AppStroke),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VoiceControlButton(
                        icon = if (muted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                        label = "Mute",
                        selected = muted,
                        onClick = onToggleMute
                    )
                    VoiceControlButton(
                        icon = Icons.Rounded.Videocam,
                        label = "Video",
                        selected = videoEnabled,
                        onClick = onToggleVideo
                    )
                    VoiceControlButton(
                        icon = Icons.Rounded.ScreenShare,
                        label = "Share",
                        selected = screenShareEnabled,
                        onClick = onToggleScreenShare
                    )
                    VoiceControlButton(
                        icon = Icons.Rounded.CallEnd,
                        label = "End",
                        danger = true,
                        onClick = onEndCall
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveCallTranscriptPanel(
    lines: List<LiveCallTranscriptLine>,
    status: String,
    voiceName: String,
    error: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recentLines = lines.takeLast(3)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AppSurface.copy(alpha = 0.74f))
            .border(1.dp, AppStroke.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (error != null) {
                Surface(
                    color = Color(0xFF321820),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF7E8B).copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = error,
                            color = Color(0xFFFFB4BD),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        TextButton(onClick = onRetry) {
                            Text("Reconnect", color = AppAccentSoft)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (recentLines.isEmpty()) {
                Text(
                    text = if (status == "Speaking...") "Speaking..." else "Start talking",
                    color = AppTextMuted,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            } else {
                recentLines.forEachIndexed { index, line ->
                    val latest = index == recentLines.lastIndex
                    Text(
                        text = line.text,
                        color = when {
                            latest -> AppTextPrimary
                            line.role == "user" -> AppAccentSoft.copy(alpha = 0.68f)
                            else -> AppTextSecondary.copy(alpha = 0.62f)
                        },
                        style = if (latest) {
                            MaterialTheme.typography.bodyLarge
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                        textAlign = TextAlign.Center,
                        maxLines = if (latest) 5 else 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (index == 0) 0.dp else 7.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (error == null) AppAccentSoft else Color(0xFFFF7E8B))
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = "$status  -  $voiceName",
                    color = AppTextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun LiveCameraPreview(
    cameraFacing: VoiceCallCameraFacing,
    onFrame: (ByteArray) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    DisposableEffect(lifecycleOwner, previewView, cameraFacing) {
        val analysisExecutor = Executors.newSingleThreadExecutor()
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        var provider: ProcessCameraProvider? = null
        var disposed = false
        var lastFrameAt = 0L

        providerFuture.addListener({
            if (disposed) return@addListener
            provider = runCatching { providerFuture.get() }.getOrNull()
            val cameraProvider = provider ?: return@addListener
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { useCase ->
                    useCase.setAnalyzer(analysisExecutor) { image ->
                        val now = System.currentTimeMillis()
                        try {
                            if (now - lastFrameAt >= 1_000L) {
                                lastFrameAt = now
                                cameraImageToJpeg(image)?.let(onFrame)
                            }
                        } finally {
                            image.close()
                        }
                    }
            }
            runCatching {
                val selector = when (cameraFacing) {
                    VoiceCallCameraFacing.Front -> CameraSelector.DEFAULT_FRONT_CAMERA
                    VoiceCallCameraFacing.Back -> CameraSelector.DEFAULT_BACK_CAMERA
                }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    analysis
                )
            }
        }, mainExecutor)

        onDispose {
            disposed = true
            provider?.unbindAll()
            analysisExecutor.shutdownNow()
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}

private fun cameraImageToJpeg(image: androidx.camera.core.ImageProxy): ByteArray? {
    val plane = image.planes.firstOrNull() ?: return null
    val width = image.width
    val height = image.height
    val rowPadding = plane.rowStride - plane.pixelStride * width
    val paddedWidth = width + rowPadding / plane.pixelStride
    val padded = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
    plane.buffer.rewind()
    padded.copyPixelsFromBuffer(plane.buffer)
    val cropped = Bitmap.createBitmap(padded, 0, 0, width, height)
    if (cropped !== padded) padded.recycle()
    val matrix = Matrix().apply {
        postRotate(image.imageInfo.rotationDegrees.toFloat())
    }
    val rotated = Bitmap.createBitmap(
        cropped,
        0,
        0,
        cropped.width,
        cropped.height,
        matrix,
        true
    )
    if (rotated !== cropped) cropped.recycle()
    return ByteArrayOutputStream().use { output ->
        rotated.compress(Bitmap.CompressFormat.JPEG, 70, output)
        rotated.recycle()
        output.toByteArray()
    }
}

@Composable
private fun VoiceAvatar(
    persona: PersonaUiState,
    outerSize: Int,
    imageSize: Int
) {
    val pulse by rememberInfiniteTransition(label = "voice-pulse").animateFloat(
        initialValue = 0.84f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voice-pulse-scale"
    )
    Box(
        modifier = Modifier
            .size(outerSize.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(pulse)
                .clip(CircleShape)
                .border(2.dp, AppAccentSoft.copy(alpha = 0.72f), CircleShape)
                .background(AppAccentDim.copy(alpha = 0.18f))
        )
        Avatar(
            persona = persona,
            size = imageSize,
            modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
        )
    }
}

@Composable
private fun VoiceWaveform(modifier: Modifier = Modifier) {
    val phase by rememberInfiniteTransition(label = "voice-wave").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voice-wave-phase"
    )
    val bars = listOf(0.15f, 0.25f, 0.55f, 0.82f, 0.42f, 0.68f, 0.35f, 0.92f, 0.58f, 0.25f, 0.18f)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        bars.forEachIndexed { index, base ->
            val animated = (base + phase * if (index % 2 == 0) 0.34f else 0.18f).coerceIn(0.1f, 1f)
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .width(5.dp)
                    .height((16 + animated * 54).dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(AppAccent.copy(alpha = 0.32f), AppAccentSoft, AppAccent.copy(alpha = 0.32f))
                        )
                    )
            )
        }
    }
}

@Composable
private fun VoiceControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean = false,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val color = when {
        danger -> Color(0xFFFF405A)
        selected -> AppAccent
        else -> AppSurface2
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(68.dp)
    ) {
        Surface(
            color = color,
            shape = CircleShape,
            border = BorderStroke(1.dp, if (danger) Color(0xFFFF6D7B) else AppAccent.copy(alpha = 0.55f)),
            modifier = Modifier
                .size(48.dp)
                .clickable(onClick = onClick)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.padding(12.dp)
            )
        }
        Text(
            text = label,
            color = if (danger) Color(0xFFFF7E8B) else AppTextPrimary,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
        )
    }
}

private fun formatCallDuration(seconds: Long): String {
    val minutes = seconds / 60L
    val remainder = seconds % 60L
    return String.format(Locale.US, "%02d:%02d", minutes, remainder)
}

@Composable
private fun ChatBackgroundLayer(background: ChatBackground) {
    when (background) {
        ChatBackground.DarkMode -> Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
        )

        ChatBackground.LightMode -> Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFEDECF4))
        )

        ChatBackground.GreyMode -> Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF24242C))
        )

        ChatBackground.PureWhite -> Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        )

        ChatBackground.PureBlack -> Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        )

        ChatBackground.PresetBlack -> BackgroundImage(
            painter = painterResource(id = R.drawable.background_black),
            darkScrim = 0.24f
        )

        ChatBackground.PresetWhite -> BackgroundImage(
            painter = painterResource(id = R.drawable.background_white),
            darkScrim = 0.32f
        )

        is ChatBackground.CustomImage -> BackgroundImage(
            painter = rememberAsyncImagePainter(background.uri),
            darkScrim = 0.42f
        )
    }
}

@Composable
private fun BackgroundImage(
    painter: androidx.compose.ui.graphics.painter.Painter,
    darkScrim: Float
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = darkScrim))
        )
    }
}

@Composable
private fun ChatHeader(
    persona: PersonaUiState,
    groupMembers: List<GroupMember>,
    sessionHeaderName: String,
    sessionHeaderAvatarUri: android.net.Uri?,
    sessionHeaderAvatarScale: Float,
    sessionHeaderAvatarOffsetX: Float,
    sessionHeaderAvatarOffsetY: Float,
    isOnline: Boolean,
    onOpenSessions: () -> Unit,
    onOpenPersona: () -> Unit,
    onOpenVoiceCall: () -> Unit,
    isRoleplayMode: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onOpenSessions) {
            Icon(
                imageVector = if (isRoleplayMode) Icons.AutoMirrored.Rounded.ArrowBack else Icons.Rounded.Menu,
                contentDescription = if (isRoleplayMode) "Back" else "Sessions",
                tint = AppAccentSoft
            )
        }
        SessionHeaderAvatar(
            fallbackPersona = persona,
            avatarUri = sessionHeaderAvatarUri,
            avatarScale = sessionHeaderAvatarScale,
            avatarOffsetX = sessionHeaderAvatarOffsetX,
            avatarOffsetY = sessionHeaderAvatarOffsetY,
            size = 48,
            modifier = Modifier.clickable(onClick = onOpenPersona)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = sessionHeaderName.ifBlank {
                    groupMembers.joinToString(" + ") { it.persona.displayName.ifBlank { "AI" } }
                },
                style = MaterialTheme.typography.titleMedium,
                color = AppTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isOnline) Color(0xFF35D07F) else Color(0xFF8B8898))
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = if (isOnline) "Online" else "Offline",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTextSecondary,
                    maxLines = 1
                )
            }
        }
        IconButton(onClick = onOpenVoiceCall) {
            Icon(
                imageVector = Icons.Rounded.Call,
                contentDescription = "Start voice call",
                tint = AppTextPrimary
            )
        }
        IconButton(onClick = onOpenPersona) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Persona settings",
                tint = AppTextPrimary
            )
        }
    }
}

@Composable
private fun SessionDrawer(
    sessions: List<ChatSession>,
    projects: List<ProjectUiState>,
    activeSessionId: String,
    activeProject: ProjectUiState?,
    persona: PersonaUiState,
    appVersionLabel: String,
    onNewSession: () -> Unit,
    onNewProject: () -> String,
    onSelectSession: (String) -> Unit,
    onNewSessionInProject: (String) -> Unit,
    onMoveSessionToProject: (String, String?) -> Unit,
    onUpdateProjectName: (String, String) -> Unit,
    onUpdateProjectDescription: (String, String) -> Unit,
    onUpdateProjectInstruction: (String, String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onRenameSession: (String, String) -> Unit,
    onCloneSession: (String) -> Unit,
    onDuplicateSessionSettings: (String) -> Unit,
    onImportSession: () -> Unit,
    onImportConfig: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onClose: () -> Unit,
    onExportSession: (ChatSession) -> Unit,
    onExportConfigSession: (ChatSession) -> Unit,
    onLongPressSession: (ChatSession) -> Unit
) {
    var deleteTarget by remember { mutableStateOf<ChatSession?>(null) }
    var actionTarget by remember { mutableStateOf<ChatSession?>(null) }
    var renameTarget by remember { mutableStateOf<ChatSession?>(null) }
    var renameDraft by remember { mutableStateOf("") }
    var projectDeleteTarget by remember { mutableStateOf<ProjectUiState?>(null) }
    var projectAssignmentTarget by remember { mutableStateOf<ChatSession?>(null) }
    var addToProjectId by remember { mutableStateOf<String?>(null) }
    var sessionSearch by remember { mutableStateOf("") }
    var projectSearch by remember { mutableStateOf("") }
    var drawerSection by remember { mutableStateOf(DrawerSection.Chats) }
    var selectedProjectId by remember { mutableStateOf<String?>(activeProject?.id) }
    val selectedProject = selectedProjectId?.let { id -> projects.firstOrNull { it.id == id } }
    val sessionQuery = sessionSearch.trim()
    val filteredSessions = if (sessionQuery.isBlank()) {
        sessions
    } else {
        sessions.filter { session -> session.matchesSessionQuery(sessionQuery) }
    }
    val projectQuery = projectSearch.trim()
    val filteredProjects = if (projectQuery.isBlank()) {
        projects
    } else {
        projects.filter { project -> project.matchesProjectQuery(projectQuery) }
    }
    val activeProjectSessions = sessions.filter { it.projectId == selectedProjectId }

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.82f),
        color = AppBackground,
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_icon_minimalist),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = appVersionLabel.substringBefore(" v"),
                    color = AppTextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = "App settings",
                        tint = AppTextSecondary
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Close sessions",
                        tint = AppTextPrimary
                    )
                }
            }

            DrawerActionRow(
                iconRes = R.drawable.asset_chat,
                label = "New chat",
                highlighted = true,
                onClick = onNewSession
            )
            DrawerActionRow(
                iconRes = R.drawable.asset_import,
                label = "Import Session",
                onClick = onImportSession
            )
            DrawerActionRow(
                iconRes = R.drawable.asset_import,
                label = "Import Companion Config",
                onClick = onImportConfig
            )
            DrawerActionRow(
                iconRes = R.drawable.asset_session,
                label = "Chats",
                selected = drawerSection == DrawerSection.Chats,
                onClick = {
                    drawerSection = DrawerSection.Chats
                    selectedProjectId = null
                }
            )
            DrawerActionRow(
                iconRes = R.drawable.asset_project,
                label = "Projects",
                selected = drawerSection == DrawerSection.Projects,
                onClick = { drawerSection = DrawerSection.Projects }
            )

            when (drawerSection) {
                DrawerSection.Chats -> {
                    DrawerSearchField(
                        value = sessionSearch,
                        onValueChange = { sessionSearch = it },
                        placeholder = "Search chats",
                        modifier = Modifier.padding(top = 18.dp)
                    )
                    SectionHeader("Recents")
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredSessions, key = { it.id }) { session ->
                            SessionRow(
                                session = session,
                                project = session.projectId?.let { id -> projects.firstOrNull { it.id == id } },
                                selected = session.id == activeSessionId,
                                onClick = { onSelectSession(session.id) },
                                onLongPress = {
                                    actionTarget = session
                                    onLongPressSession(session)
                                }
                            )
                        }
                    }
                }

                DrawerSection.Projects -> {
                    if (selectedProject == null) {
                        DrawerSearchField(
                            value = projectSearch,
                            onValueChange = { projectSearch = it },
                            placeholder = "Search projects",
                            modifier = Modifier.padding(top = 14.dp)
                        )
                        SectionHeader("Projects")
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredProjects, key = { it.id }) { project ->
                                ProjectRow(
                                    project = project,
                                    chatCount = sessions.count { it.projectId == project.id },
                                    selected = project.id == activeProject?.id,
                                    onClick = {
                                        selectedProjectId = project.id
                                    }
                                )
                            }
                        }
                        Button(
                            onClick = {
                                selectedProjectId = onNewProject()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AppAccent),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "New project",
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    } else {
                        ProjectDetailPanel(
                            project = selectedProject,
                            sessions = activeProjectSessions,
                            activeSessionId = activeSessionId,
                            modifier = Modifier.weight(1f),
                            onBack = { selectedProjectId = null },
                            onNameChange = { onUpdateProjectName(selectedProject.id, it) },
                            onDescriptionChange = { onUpdateProjectDescription(selectedProject.id, it) },
                            onInstructionChange = { onUpdateProjectInstruction(selectedProject.id, it) },
                            onDeleteProject = { projectDeleteTarget = selectedProject },
                            onNewChat = { onNewSessionInProject(selectedProject.id) },
                            onAddExistingChat = { addToProjectId = selectedProject.id },
                            onSelectSession = onSelectSession,
                            onSessionLongPress = { actionTarget = it }
                        )
                    }
                }
            }
            TextButton(
                onClick = onOpenAbout,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 2.dp)
            ) {
                Text(
                    text = appVersionLabel,
                    color = AppTextMuted,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }

    deleteTarget?.let { session ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor = AppSurface,
            title = {
                Text(
                    text = "Delete chat?",
                    color = AppTextPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = session.displayTitle(),
                    color = AppTextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteSession(session.id)
                        deleteTarget = null
                    }
                ) {
                    Text(text = "Delete", color = Color(0xFFFF7E8B))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(text = "Cancel", color = AppAccentSoft)
                }
            }
        )
    }

    projectDeleteTarget?.let { project ->
        val chatCount = sessions.count { it.projectId == project.id }
        AlertDialog(
            onDismissRequest = { projectDeleteTarget = null },
            containerColor = AppSurface,
            title = {
                Text(
                    text = "Delete project?",
                    color = AppTextPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = if (chatCount == 0) {
                        "This removes ${project.name.ifBlank { "New project" }}."
                    } else {
                        "This removes ${project.name.ifBlank { "New project" }} and moves $chatCount chat${if (chatCount == 1) "" else "s"} back to Recents."
                    },
                    color = AppTextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteProject(project.id)
                        if (selectedProjectId == project.id) {
                            selectedProjectId = null
                        }
                        projectDeleteTarget = null
                    }
                ) {
                    Text(text = "Delete", color = Color(0xFFFF7E8B))
                }
            },
            dismissButton = {
                TextButton(onClick = { projectDeleteTarget = null }) {
                    Text(text = "Cancel", color = AppAccentSoft)
                }
            }
        )
    }

    actionTarget?.let { session ->
        SessionActionSheet(
            session = session,
            onDismiss = { actionTarget = null },
            onDuplicate = {
                onDuplicateSessionSettings(session.id)
                actionTarget = null
            },
            onClone = {
                onCloneSession(session.id)
                actionTarget = null
            },
            onRename = {
                renameTarget = session
                renameDraft = session.displayTitle()
                actionTarget = null
            },
            onMove = {
                projectAssignmentTarget = session
                actionTarget = null
            },
            onDelete = {
                deleteTarget = session
                actionTarget = null
            },
            onExport = {
                onExportSession(session)
                actionTarget = null
            },
            onExportConfig = {
                onExportConfigSession(session)
                actionTarget = null
            }
        )
    }

    projectAssignmentTarget?.let { session ->
        ProjectAssignmentSheet(
            session = session,
            projects = projects,
            onDismiss = { projectAssignmentTarget = null },
            onSelectProject = { projectId ->
                onMoveSessionToProject(session.id, projectId)
                projectAssignmentTarget = null
            }
        )
    }

    addToProjectId?.let { projectId ->
        projects.firstOrNull { it.id == projectId }?.let { project ->
            AddChatsToProjectSheet(
                project = project,
                sessions = sessions.filter { it.projectId != project.id },
                onDismiss = { addToProjectId = null },
                onAdd = { sessionId ->
                    onMoveSessionToProject(sessionId, project.id)
                }
            )
        }
    }

    renameTarget?.let { session ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            containerColor = AppSurface,
            title = {
                Text(
                    text = "Rename chat",
                    color = AppTextPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it.take(72) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTextPrimary,
                        unfocusedTextColor = AppTextPrimary,
                        focusedBorderColor = AppAccent.copy(alpha = 0.7f),
                        unfocusedBorderColor = AppStroke,
                        cursorColor = AppAccent,
                        focusedContainerColor = AppSurface2,
                        unfocusedContainerColor = AppSurface2
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRenameSession(session.id, renameDraft)
                        renameTarget = null
                    }
                ) {
                    Text(text = "Rename", color = AppAccentSoft)
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text(text = "Cancel", color = AppTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SessionActionSheet(
    session: ChatSession,
    onDismiss: () -> Unit,
    onDuplicate: () -> Unit,
    onClone: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    onExportConfig: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.46f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = AppSurface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            border = BorderStroke(1.dp, AppStroke),
            shadowElevation = 18.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(42.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(AppStroke)
                )
                Text(
                    text = session.displayTitle(),
                    color = AppTextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 18.dp, bottom = 10.dp)
                )
                ActionSheetRow(
                    icon = Icons.Rounded.ContentCopy,
                    label = "Copy settings only",
                    onClick = onDuplicate
                )
                ActionSheetRow(
                    icon = Icons.Rounded.ChatBubbleOutline,
                    label = "Clone session",
                    onClick = onClone
                )
                ActionSheetRow(
                    icon = Icons.Rounded.Edit,
                    label = "Rename",
                    onClick = onRename
                )
                ActionSheetRow(
                    icon = Icons.Rounded.InsertDriveFile,
                    label = if (session.projectId == null) "Move to project" else "Change project",
                    onClick = onMove
                )
                ActionSheetRow(
                    icon = Icons.Rounded.Upload,
                    label = "Export session",
                    onClick = onExport
                )
                ActionSheetRow(
                    icon = Icons.Rounded.Share,
                    label = "Export config only",
                    onClick = onExportConfig
                )
                ActionSheetRow(
                    icon = Icons.Rounded.DeleteOutline,
                    label = "Delete",
                    onClick = onDelete
                )
            }
        }
    }
}

@Composable
private fun ProjectAssignmentSheet(
    session: ChatSession,
    projects: List<ProjectUiState>,
    onDismiss: () -> Unit,
    onSelectProject: (String?) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = AppSurface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            border = BorderStroke(1.dp, AppStroke),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(18.dp)
            ) {
                Text(
                    text = "Move ${session.displayTitle()}",
                    color = AppTextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Choose where this chat belongs.",
                    color = AppTextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
                ProjectChoiceRow(
                    label = "No project",
                    selected = session.projectId == null,
                    onClick = { onSelectProject(null) }
                )
                projects.forEach { project ->
                    ProjectChoiceRow(
                        label = project.name.ifBlank { "New project" },
                        selected = session.projectId == project.id,
                        onClick = { onSelectProject(project.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) AppAccentDim else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.InsertDriveFile,
                contentDescription = null,
                tint = if (selected) AppAccentSoft else AppTextSecondary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                color = AppTextPrimary,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.DoneAll,
                    contentDescription = null,
                    tint = AppAccentSoft
                )
            }
        }
    }
}

@Composable
private fun AddChatsToProjectSheet(
    project: ProjectUiState,
    sessions: List<ChatSession>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var addedIds by remember(project.id) { mutableStateOf(emptySet<String>()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = AppSurface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            border = BorderStroke(1.dp, AppStroke),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.72f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Add chats",
                            color = AppTextPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = project.name,
                            color = AppTextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.btn_done), color = AppAccentSoft)
                    }
                }
                if (sessions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Every chat is already in this project.",
                            color = AppTextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sessions, key = { it.id }) { session ->
                            val added = session.id in addedIds
                            Surface(
                                color = if (added) AppAccentDim else AppSurface2,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, AppStroke),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !added) {
                                        onAdd(session.id)
                                        addedIds = addedIds + session.id
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = session.displayTitle(),
                                            color = AppTextPrimary,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                        Text(
                                            text = session.preview,
                                            color = AppTextSecondary,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = if (added) Icons.Rounded.DoneAll else Icons.Rounded.Add,
                                        contentDescription = null,
                                        tint = AppAccentSoft
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionRow(
    session: ChatSession,
    project: ProjectUiState?,
    selected: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Surface(
        color = if (selected) AppAccentDim.copy(alpha = 0.82f) else AppSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (selected) AppAccent.copy(alpha = 0.6f) else AppStroke),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (selected) AppAccent else AppSurface2),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.asset_session),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.displayTitle(),
                    color = AppTextPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = session.preview,
                    color = AppTextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (project != null) {
                    Text(
                        text = project.name,
                        color = AppAccentSoft,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = session.updatedAt,
                color = AppTextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Session actions",
                tint = AppTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun EmptyChatState(
    persona: PersonaUiState,
    onCreatePersona: () -> Unit,
    onSuggestion: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 58.dp, start = 12.dp, end = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Avatar(persona = persona, size = 112)
        Spacer(modifier = Modifier.height(22.dp))
        Text(
            text = buildAnnotatedString {
                append("Hi, I'm ")
                withStyle(SpanStyle(color = AppAccentSoft, fontWeight = FontWeight.Bold)) {
                    append(persona.displayName.ifBlank { "New Persona" })
                }
            },
            color = AppTextPrimary,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = "How can I help you today?",
            color = AppTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )

        val suggestions = listOf(
            "Tell me a story" to Icons.Rounded.InsertDriveFile,
            "Help me write code" to Icons.Rounded.ContentCopy,
            "Brainstorm ideas" to Icons.Rounded.StarBorder,
            "Explain something" to Icons.Rounded.Info
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            suggestions.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { (label, icon) ->
                        SuggestionChip(
                            label = label,
                            icon = icon,
                            onClick = { onSuggestion(label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        TextButton(
            onClick = onCreatePersona,
            modifier = Modifier.padding(top = 10.dp)
        ) {
            Text(
                text = "Edit persona",
                color = AppAccentSoft,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun SuggestionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = AppSurface.copy(alpha = 0.72f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AppStroke),
        modifier = modifier
            .height(56.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppTextSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                color = AppTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ChatErrorLine(message: String, onDismiss: () -> Unit) {
    Text(
        text = message,
        color = Color(0xFFFFA0AA),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDismiss() }
            .padding(horizontal = 24.dp, vertical = 4.dp)
    )
}

@Composable
private fun TypingBubble(persona: PersonaUiState) {
    val transition = rememberInfiniteTransition(label = "typingDots")
    val dot1 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0.35f at 0
                1f at 180
                0.35f at 420
                0.35f at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot1"
    )
    val dot2 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0.35f at 0
                0.35f at 180
                1f at 400
                0.35f at 660
                0.35f at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot2"
    )
    val dot3 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0.35f at 0
                0.35f at 400
                1f at 620
                0.35f at 880
                0.35f at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "dot3"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        Avatar(persona = persona, size = 40)
        Spacer(modifier = Modifier.width(10.dp))
        Surface(
            shape = RoundedCornerShape(
                topStart = 22.dp,
                topEnd = 22.dp,
                bottomStart = 6.dp,
                bottomEnd = 22.dp
            ),
            color = AppSurface2,
            border = BorderStroke(1.dp, AppStroke.copy(alpha = 0.7f)),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TypingDot(alpha = dot1)
                TypingDot(alpha = dot2)
                TypingDot(alpha = dot3)
            }
        }
    }
}

@Composable
private fun TypingDot(alpha: Float) {
    Box(
        modifier = Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(AppAccentSoft.copy(alpha = alpha))
    )
}

private fun ChatMessage.sameBubbleActor(other: ChatMessage): Boolean {
    if (role != other.role) return false
    if (role == "user") return true
    return speakerId == other.speakerId && speakerName == other.speakerName
}

@Composable
private fun AnimatedMessageBubble(
    message: ChatMessage,
    persona: PersonaUiState,
    groupMembers: List<GroupMember>,
    showAvatar: Boolean,
    showSpeakerLabel: Boolean,
    compactTop: Boolean,
    compactBottom: Boolean,
    actionsVisible: Boolean,
    animateText: Boolean,
    onTextAnimationStart: (String) -> Unit,
    onClick: () -> Unit,
    onOpenImage: (android.net.Uri) -> Unit,
    onMessageLongPress: (ChatMessage) -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onRetry: () -> Unit,
    onSpeak: () -> Unit
) {
    var visible by remember(message.id) { mutableStateOf(false) }
    var displayedContent by remember(message.id) {
        mutableStateOf(if (animateText) "" else message.content)
    }

    LaunchedEffect(message.id) {
        visible = true
    }

    LaunchedEffect(message.id, message.content, animateText) {
        if (!animateText) {
            displayedContent = message.content
            return@LaunchedEffect
        }
        onTextAnimationStart(message.id)
        if (displayedContent.length > message.content.length) {
            displayedContent = ""
        }
        val chunkSize = (message.content.length / 90).coerceIn(2, 10)
        var index = displayedContent.length
        while (index < message.content.length) {
            index = (index + chunkSize).coerceAtMost(message.content.length)
            displayedContent = message.content.take(index)
            delay(12)
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(220)) +
            slideInVertically(animationSpec = tween(260), initialOffsetY = { it / 3 }) +
            scaleIn(animationSpec = tween(260), initialScale = 0.96f)
    ) {
        MessageBubble(
            message = message,
            persona = persona,
            groupMembers = groupMembers,
            displayContent = displayedContent,
            showAvatar = showAvatar,
            showSpeakerLabel = showSpeakerLabel,
            compactTop = compactTop,
            compactBottom = compactBottom,
            actionsVisible = actionsVisible,
            onClick = onClick,
            onOpenImage = onOpenImage,
            onMessageLongPress = onMessageLongPress,
            onCopy = onCopy,
            onEdit = onEdit,
            onRetry = onRetry,
            onSpeak = onSpeak
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: ChatMessage,
    persona: PersonaUiState,
    groupMembers: List<GroupMember>,
    displayContent: String = message.content,
    showAvatar: Boolean,
    showSpeakerLabel: Boolean,
    compactTop: Boolean,
    compactBottom: Boolean,
    actionsVisible: Boolean,
    onClick: () -> Unit,
    onOpenImage: (android.net.Uri) -> Unit,
    onMessageLongPress: (ChatMessage) -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onRetry: () -> Unit,
    onSpeak: () -> Unit
) {
    val isUser = message.role == "user"
    val speakerPersona = if (isUser) persona else groupMembers.firstOrNull { it.id == message.speakerId }?.persona ?: persona
    val topRadius = if (compactTop) 12.dp else 22.dp
    val bottomRadius = if (compactBottom) 12.dp else 22.dp

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            if (!isUser) {
                if (showAvatar) {
                    Avatar(persona = speakerPersona, size = 36)
                } else {
                    Spacer(modifier = Modifier.size(36.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = if (isUser) topRadius else if (compactTop) 12.dp else 22.dp,
                    topEnd = if (isUser) if (compactTop) 12.dp else 22.dp else topRadius,
                    bottomStart = if (isUser) bottomRadius else if (compactBottom) 12.dp else 6.dp,
                    bottomEnd = if (isUser) if (compactBottom) 12.dp else 6.dp else bottomRadius
                ),
                color = if (isUser) AppAccentDim.copy(alpha = 0.95f) else AppSurface2.copy(alpha = 0.96f),
                border = if (isUser) {
                    BorderStroke(1.dp, AppAccent.copy(alpha = 0.2f))
                } else {
                    BorderStroke(1.dp, AppStroke.copy(alpha = 0.62f))
                },
                shadowElevation = if (compactTop || compactBottom) 1.dp else 3.dp,
                modifier = Modifier
                    .fillMaxWidth(if (isUser) 0.82f else 0.9f)
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = { onMessageLongPress(message) }
                    )
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    if (!isUser && groupMembers.size > 1 && showSpeakerLabel) {
                        Text(
                            text = message.speakerName ?: speakerPersona.displayName,
                            color = AppAccentSoft,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    if (message.isImageLoading) {
                        AnimeImageLoadingCard()
                    }
                    message.remoteImageUrl?.let { imageUrl ->
                        val imageUri = android.net.Uri.parse(imageUrl)
                        Image(
                            painter = rememberAsyncImagePainter(imageUrl),
                            contentDescription = "Fetched image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onOpenImage(imageUri) }
                        )
                    }
                    if (message.imageUris.isNotEmpty()) {
                        message.imageUris.forEachIndexed { index, uri ->
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = "Message image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (message.imageUris.size == 1) 180.dp else 118.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onOpenImage(uri) }
                            )
                            if (index != message.imageUris.lastIndex) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                        if (displayContent.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                    val showText = displayContent.isNotBlank() && !message.isImageLoading && message.remoteImageUrl == null
                    if (showText) {
                        MarkdownContent(
                            input = displayContent,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = message.time,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppTextSecondary.copy(alpha = 0.86f)
                        )
                        if (isUser) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Rounded.DoneAll,
                                contentDescription = "Sent",
                                tint = AppAccentSoft,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = actionsVisible,
            enter = fadeIn(animationSpec = tween(120)) + slideInVertically(initialOffsetY = { it / 3 }),
            exit = fadeOut(animationSpec = tween(100))
        ) {
            MessageInlineActions(
                isUser = isUser,
                canSpeak = !isUser && message.content.isNotBlank(),
                modifier = Modifier
                    .align(if (isUser) Alignment.End else Alignment.Start)
                    .padding(
                        start = if (isUser) 0.dp else 44.dp,
                        top = 6.dp,
                        end = if (isUser) 4.dp else 0.dp
                    ),
                onCopy = onCopy,
                onEdit = onEdit,
                onRetry = onRetry,
                onSpeak = onSpeak
            )
        }
    }
}

@Composable
private fun MessageInlineActions(
    isUser: Boolean,
    canSpeak: Boolean,
    modifier: Modifier = Modifier,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onRetry: () -> Unit,
    onSpeak: () -> Unit
) {
    Surface(
        color = AppSurface.copy(alpha = 0.94f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, AppStroke.copy(alpha = 0.75f)),
        shadowElevation = 4.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InlineActionIcon(Icons.Rounded.ContentCopy, "Copy", onCopy)
            if (isUser) {
                InlineActionIcon(Icons.Rounded.Edit, "Edit", onEdit)
            }
            InlineActionIcon(Icons.Rounded.Refresh, if (isUser) "Retry from here" else "Retry response", onRetry)
            if (canSpeak) {
                InlineActionIcon(Icons.Rounded.VolumeUp, "Speak", onSpeak)
            }
        }
    }
}

@Composable
private fun InlineActionIcon(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(34.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = AppTextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun DrawerActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconRes: Int? = null,
    label: String,
    selected: Boolean = false,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    val contentColor = if (highlighted || selected) AppAccentSoft else AppTextPrimary
    Surface(
        color = if (selected) AppSurface2 else Color.Transparent,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconRes != null) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                color = contentColor,
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

@Composable
private fun DrawerSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = AppTextSecondary
            )
        },
        placeholder = {
            Text(text = placeholder, color = AppTextSecondary)
        },
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = AppTextPrimary,
            unfocusedTextColor = AppTextPrimary,
            focusedBorderColor = AppAccent,
            unfocusedBorderColor = AppStroke,
            cursorColor = AppAccent,
            focusedContainerColor = AppSurface2,
            unfocusedContainerColor = AppSurface2
        ),
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label,
        color = AppTextSecondary,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
    )
}

@Composable
private fun ProjectRow(
    project: ProjectUiState,
    chatCount: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) AppSurface2 else Color.Transparent,
        shape = RoundedCornerShape(14.dp),
        border = if (selected) BorderStroke(1.dp, AppStroke) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.asset_project),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = project.name.ifBlank { "New project" },
                    color = AppTextPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.StarBorder,
                        contentDescription = null,
                        tint = AppAccentSoft,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = "$chatCount chats · Edited ${project.updatedAt}",
                color = AppTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

@Composable
private fun ProjectDetailPanel(
    project: ProjectUiState,
    sessions: List<ChatSession>,
    activeSessionId: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onInstructionChange: (String) -> Unit,
    onDeleteProject: () -> Unit,
    onNewChat: () -> Unit,
    onAddExistingChat: () -> Unit,
    onSelectSession: (String) -> Unit,
    onSessionLongPress: (ChatSession) -> Unit
) {
    var editVisible by remember(project.id) { mutableStateOf(false) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back to projects",
                    tint = AppTextPrimary
                )
            }
            Text(
                text = "Project",
                color = AppTextSecondary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDeleteProject) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete project",
                    tint = Color(0xFFFF7E8B)
                )
            }
        }
        Text(
            text = project.name.ifBlank { "New project" },
            color = AppTextPrimary,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
        if (project.description.isNotBlank()) {
            Text(
                text = project.description,
                color = AppTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        Surface(
            color = AppSurface,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, AppStroke),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp)
                .clickable { editVisible = true }
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Project instruction",
                        color = AppTextPrimary,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit project",
                        tint = AppAccentSoft,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Text(
                    text = project.instruction.ifBlank {
                        "Add shared context and instructions for every chat in this project."
                    },
                    color = if (project.instruction.isBlank()) AppTextMuted else AppTextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onNewChat,
                colors = ButtonDefaults.buttonColors(containerColor = AppAccent),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.new_chat), color = Color.White)
            }
            Button(
                onClick = onAddExistingChat,
                colors = ButtonDefaults.buttonColors(containerColor = AppSurface2),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Icon(Icons.Rounded.InsertDriveFile, contentDescription = null, tint = AppAccentSoft)
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.add_chat), color = AppTextPrimary)
            }
        }
        SectionHeader("Recent chats")
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sessions, key = { it.id }) { session ->
                SessionRow(
                    session = session,
                    project = project,
                    selected = session.id == activeSessionId,
                    onClick = { onSelectSession(session.id) },
                    onLongPress = { onSessionLongPress(session) }
                )
            }
        }
    }

    if (editVisible) {
        AlertDialog(
            onDismissRequest = { editVisible = false },
            containerColor = AppSurface,
            title = {
                Text(stringResource(R.string.edit_project_title), color = AppTextPrimary)
            },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 520.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = project.name,
                        onValueChange = onNameChange,
                        label = { Text(stringResource(R.string.project_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = project.description,
                        onValueChange = onDescriptionChange,
                        label = { Text(stringResource(R.string.project_description_label)) },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = project.instruction,
                        onValueChange = onInstructionChange,
                        label = { Text(stringResource(R.string.shared_instruction_label)) },
                        minLines = 5,
                        maxLines = 10,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { editVisible = false }) {
                    Text(stringResource(R.string.btn_done), color = AppAccentSoft)
                }
            }
        )
    }
}

@Composable
private fun AnimeImageLoadingCard() {
    val transition = rememberInfiniteTransition(label = "animeImageLoading")
    val alpha by transition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.68f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "animeImageLoadingAlpha"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AppStroke.copy(alpha = alpha))
    ) {
        Surface(
            color = AppSurface.copy(alpha = 0.72f),
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    color = AppAccentSoft,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Fetching image",
                    color = AppTextPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun MessageActionSheet(
    message: ChatMessage,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onRetry: () -> Unit,
    onSpeak: () -> Unit
) {
    val isUser = message.role == "user"
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.46f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = AppSurface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            border = BorderStroke(1.dp, AppStroke),
            shadowElevation = 18.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(42.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(AppStroke)
                )
                Text(
                    text = if (isUser) "Your message" else "AI response",
                    color = AppTextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 18.dp, bottom = 10.dp)
                )
                ActionSheetRow(
                    icon = Icons.Rounded.ContentCopy,
                    label = if (message.content.isBlank() && message.remoteImageUrl != null) "Copy image link" else "Copy",
                    onClick = onCopy
                )
                if (isUser) {
                    ActionSheetRow(
                        icon = Icons.Rounded.Edit,
                        label = "Edit",
                        onClick = onEdit
                    )
                }
                ActionSheetRow(
                    icon = Icons.Rounded.Refresh,
                    label = if (isUser) "Retry from here" else "Retry response",
                    onClick = onRetry
                )
                if (!isUser && message.content.isNotBlank()) {
                    ActionSheetRow(
                        icon = Icons.Rounded.VolumeUp,
                        label = "Speak",
                        onClick = onSpeak
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeechPreviewSheet(
    state: TtsPreviewState,
    onTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onGenerate: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.46f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = AppSurface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            border = BorderStroke(1.dp, AppStroke),
            shadowElevation = 18.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(42.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(AppStroke)
                )
                Text(
                    text = "Speech preview",
                    color = AppTextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 18.dp)
                )
                Text(
                    text = "Edit before ElevenLabs uses credits.",
                    color = AppTextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                if (state.isPreparing) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = AppAccentSoft,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Preparing Japanese speech",
                            color = AppTextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = state.text,
                        onValueChange = onTextChange,
                        enabled = !state.isGenerating,
                        minLines = 4,
                        maxLines = 8,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = AppTextPrimary,
                            unfocusedTextColor = AppTextPrimary,
                            disabledTextColor = AppTextSecondary,
                            focusedBorderColor = AppAccent.copy(alpha = 0.7f),
                            unfocusedBorderColor = AppStroke,
                            disabledBorderColor = AppStroke,
                            cursorColor = AppAccent,
                            focusedContainerColor = AppSurface2,
                            unfocusedContainerColor = AppSurface2,
                            disabledContainerColor = AppSurface2
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 148.dp)
                    )
                    Text(
                        text = "Estimated ElevenLabs usage: ${state.characterCount} chars",
                        color = AppTextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !state.isGenerating
                    ) {
                        Text(
                            text = "Cancel",
                            color = AppTextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onGenerate,
                        enabled = !state.isPreparing && !state.isGenerating && state.text.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = AppAccent),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (state.isGenerating) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (state.isGenerating) "Generating" else "Generate & play",
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportPreviewSheet(
    state: ImportPreviewState,
    onDismiss: () -> Unit,
    onImport: () -> Unit,
    onImportAsCopy: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.48f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = AppSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            border = BorderStroke(1.dp, AppStroke),
            shadowElevation = 18.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(42.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(AppStroke)
                )
                Row(
                    modifier = Modifier.padding(top = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(persona = state.session.persona, size = 64)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Import ${state.sourceKind}",
                            color = AppTextSecondary,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = state.session.persona.displayName.ifBlank { state.session.displayTitle() },
                            color = AppTextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${state.session.persona.traits.take(3).joinToString(", ").ifBlank { "No tags" }} · ${state.messageCount} messages",
                            color = AppTextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (state.duplicateWarning != null) {
                    Surface(
                        color = Color(0xFFFFB74D).copy(alpha = 0.14f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFFFB74D).copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                    ) {
                        Text(
                            text = state.duplicateWarning,
                            color = Color(0xFFFFCC80),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, AppStroke),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = AppTextSecondary)
                    }
                    OutlinedButton(
                        onClick = onImportAsCopy,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, AppAccentSoft),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Import as Copy", color = AppAccentSoft)
                    }
                }
                Button(
                    onClick = onImport,
                    colors = ButtonDefaults.buttonColors(containerColor = AppAccent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .height(50.dp)
                ) {
                    Text("Import", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ActionSheetRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppAccentSoft,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                color = AppTextPrimary,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun ChatToolsSheet(
    onDismiss: () -> Unit,
    onScrollTop: () -> Unit,
    onScrollBottom: () -> Unit,
    onCamera: () -> Unit,
    onPhotos: () -> Unit,
    onFiles: () -> Unit,
    messages: List<ChatMessage>,
    onJumpToMessage: (Int) -> Unit,
    webSearchEnabled: Boolean,
    webSearchAvailable: Boolean,
    onWebSearchChange: (Boolean) -> Unit,
    animeImageModeEnabled: Boolean,
    animeImagePreset: AnimeImagePreset,
    onAnimeImageModeChange: (Boolean) -> Unit,
    onAnimeImagePresetChange: (AnimeImagePreset) -> Unit
) {
    var keywordSearch by remember { mutableStateOf("") }
    val keywordResults = remember(messages, keywordSearch) {
        val query = keywordSearch.trim()
        if (query.isBlank()) {
            emptyList()
        } else {
            messages.mapIndexedNotNull { index, message ->
                if (
                    message.content.contains(query, ignoreCase = true) ||
                    message.speakerName.orEmpty().contains(query, ignoreCase = true)
                ) {
                    index to message
                } else {
                    null
                }
            }.take(8)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.52f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = AppSurface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            border = BorderStroke(1.dp, AppStroke),
            shadowElevation = 18.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = AppTextPrimary
                        )
                    }
                    Text(
                        text = "Chat tools",
                        color = AppTextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.size(48.dp))
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    AttachmentOptionCard(
                        imageRes = R.drawable.tool_camera,
                        label = "Camera",
                        onClick = onCamera,
                        modifier = Modifier.weight(1f)
                    )
                    AttachmentOptionCard(
                        imageRes = R.drawable.tool_photos,
                        label = "Photos",
                        onClick = onPhotos,
                        modifier = Modifier.weight(1f)
                    )
                    AttachmentOptionCard(
                        imageRes = R.drawable.tool_files,
                        label = "Files",
                        onClick = onFiles,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                ) {
                    ToolActionButton(
                        icon = Icons.Rounded.KeyboardArrowDown,
                        label = "Top",
                        iconRotation = 180f,
                        onClick = onScrollTop,
                        modifier = Modifier.weight(1f)
                    )
                    ToolActionButton(
                        icon = Icons.Rounded.KeyboardArrowDown,
                        label = "Bottom",
                        onClick = onScrollBottom,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = keywordSearch,
                    onValueChange = { keywordSearch = it },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = AppTextSecondary
                        )
                    },
                    placeholder = {
                        Text(text = "Search this chat", color = AppTextSecondary)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTextPrimary,
                        unfocusedTextColor = AppTextPrimary,
                        focusedBorderColor = AppAccent,
                        unfocusedBorderColor = AppStroke,
                        cursorColor = AppAccent,
                        focusedContainerColor = AppSurface2,
                        unfocusedContainerColor = AppSurface2
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                AnimatedVisibility(visible = keywordSearch.isNotBlank()) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        if (keywordResults.isEmpty()) {
                            Text(
                                text = "No matches",
                                color = AppTextSecondary,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                            )
                        } else {
                            keywordResults.forEach { (index, message) ->
                                SearchResultRow(
                                    message = message,
                                    onClick = { onJumpToMessage(index) }
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(AppStroke)
                        .padding(top = 18.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 22.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Public,
                        contentDescription = null,
                        tint = AppTextPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Web search",
                        color = AppTextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = webSearchEnabled,
                        onCheckedChange = onWebSearchChange,
                        enabled = webSearchAvailable
                    )
                }
                if (!webSearchAvailable) {
                    Text(
                        text = "Add a Tavily key to enable web search.",
                        color = AppTextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 44.dp, bottom = 4.dp)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ImageSearch,
                        contentDescription = null,
                        tint = AppTextPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Anime image",
                        color = AppTextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = animeImageModeEnabled,
                        onCheckedChange = onAnimeImageModeChange
                    )
                }
                AnimatedVisibility(visible = animeImageModeEnabled) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 44.dp, top = 6.dp)
                    ) {
                        items(AnimeImagePreset.entries) { preset ->
                            PresetChoiceChip(
                                label = preset.label,
                                selected = animeImagePreset == preset,
                                onClick = { onAnimeImagePresetChange(preset) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    iconRotation: Float = 0f,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = AppSurface2,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AppStroke),
        modifier = modifier
            .height(46.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppAccentSoft,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = iconRotation }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = AppTextPrimary,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun SearchResultRow(
    message: ChatMessage,
    onClick: () -> Unit
) {
    val label = if (message.role == "user") {
        "You"
    } else {
        message.speakerName ?: "AI"
    }
    val snippet = message.content.ifBlank {
        message.remoteImageUrl ?: if (message.imageUris.isNotEmpty()) "[image]" else "(empty)"
    }
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
            Text(
                text = label,
                color = AppAccentSoft,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = snippet,
                color = AppTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PresetChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (selected) AppAccentDim else AppSurface2,
        border = BorderStroke(1.dp, if (selected) AppAccent else AppStroke),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = if (selected) AppAccentSoft else AppTextPrimary,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun AttachmentOptionCard(
    imageRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = AppSurface2,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, AppStroke),
        modifier = modifier
            .height(112.dp)
            .clickable(onClick = onClick)
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = label,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun ChatInputBar(
    placeholder: String,
    draft: String,
    isSending: Boolean,
    attachedImageUris: List<android.net.Uri>,
    webSearchEnabled: Boolean,
    animeImageModeEnabled: Boolean,
    animeImagePreset: AnimeImagePreset,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onOpenTools: () -> Unit,
    onVoiceInput: () -> Unit,
    onRemoveAttachedImage: (android.net.Uri) -> Unit,
    onOpenImage: (android.net.Uri) -> Unit
) {
    val sendScale by animateFloatAsState(
        targetValue = if ((draft.isBlank() && attachedImageUris.isEmpty()) || isSending) 0.94f else 1f,
        label = "sendScale"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp),
        color = AppSurface2,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, AppAccent.copy(alpha = 0.42f)),
        shadowElevation = 6.dp
    ) {
        Column {
            if (webSearchEnabled || animeImageModeEnabled) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(start = 16.dp, top = 10.dp, end = 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (webSearchEnabled) {
                        item {
                            ActiveToolChip(
                                icon = Icons.Rounded.Public,
                                label = "Web search"
                            )
                        }
                    }
                    if (animeImageModeEnabled) {
                        item {
                            ActiveToolChip(
                                icon = Icons.Rounded.ImageSearch,
                                label = "Anime image: ${animeImagePreset.label}"
                            )
                        }
                    }
                }
            }
            if (attachedImageUris.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(start = 16.dp, top = 10.dp, end = 16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(attachedImageUris, key = { it.toString() }) { uri ->
                        AttachedImageChip(
                            uri = uri,
                            onOpen = { onOpenImage(uri) },
                            onRemove = { onRemoveAttachedImage(uri) }
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .heightIn(min = 60.dp, max = 164.dp)
                    .padding(start = 8.dp, top = 7.dp, end = 8.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = AppSurface,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, AppStroke),
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(onClick = onOpenTools)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Open chat tools",
                            tint = AppTextPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    singleLine = false,
                    minLines = 1,
                    maxLines = 6,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp, max = 144.dp),
                    placeholder = {
                        Text(
                            text = placeholder,
                            color = AppTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTextPrimary,
                        unfocusedTextColor = AppTextPrimary,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = AppAccent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
                Surface(
                    color = AppSurface,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, AppStroke),
                    modifier = Modifier
                        .size(42.dp)
                        .clickable(onClick = onVoiceInput)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = "Voice input",
                            tint = AppTextSecondary,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onSend,
                    enabled = (draft.isNotBlank() || attachedImageUris.isNotEmpty()) && !isSending,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppAccent,
                        disabledContainerColor = AppAccent.copy(alpha = 0.35f)
                    ),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(48.dp)
                        .scale(sendScale)
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Send",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveToolChip(
    icon: ImageVector,
    label: String
) {
    Surface(
        color = AppAccent.copy(alpha = 0.16f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, AppAccent.copy(alpha = 0.34f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppAccentSoft,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                color = AppAccentSoft,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AttachedImageChip(
    uri: android.net.Uri,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    Box {
        Image(
            painter = rememberAsyncImagePainter(uri),
            contentDescription = "Attached image",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onOpen)
        )
        Surface(
            color = Color.Black.copy(alpha = 0.72f),
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 5.dp, y = (-5).dp)
                .size(24.dp)
                .clickable(onClick = onRemove)
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Remove image",
                tint = Color.White,
                modifier = Modifier.padding(5.dp)
            )
        }
    }
}

@Composable
private fun MarkdownContent(
    input: String,
    modifier: Modifier = Modifier
) {
    val blocks = remember(input) { parseMarkdownBlocks(input) }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = formatMarkdownLite(block.text),
                        style = MaterialTheme.typography.bodyLarge,
                        color = AppTextPrimary,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                    )
                }

                is MarkdownBlock.Table -> {
                    MarkdownTable(block.rows)
                }
            }
        }
    }
}

@Composable
private fun MarkdownTable(rows: List<List<String>>) {
    if (rows.isEmpty()) return
    val columnCount = rows.maxOf { it.size }.coerceAtLeast(1)
    val widths = remember(rows) {
        List(columnCount) { column ->
            val longest = rows.maxOf { row -> row.getOrNull(column)?.length ?: 0 }
            when {
                longest >= 26 -> 168.dp
                longest >= 14 -> 122.dp
                else -> 88.dp
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, AppStroke.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
    ) {
        rows.forEachIndexed { rowIndex, row ->
            Row {
                for (column in 0 until columnCount) {
                    val isHeader = rowIndex == 0
                    Box(
                        modifier = Modifier
                            .width(widths[column])
                            .heightIn(min = 42.dp)
                            .background(if (isHeader) AppSurface.copy(alpha = 0.92f) else AppBackground.copy(alpha = 0.34f))
                            .border(0.5.dp, AppStroke.copy(alpha = 0.58f))
                            .padding(horizontal = 10.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = formatMarkdownLite(row.getOrNull(column).orEmpty()),
                            color = AppTextPrimary,
                            style = if (isHeader) {
                                MaterialTheme.typography.labelLarge
                            } else {
                                MaterialTheme.typography.bodyMedium
                            },
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private sealed class MarkdownBlock {
    data class Paragraph(val text: String) : MarkdownBlock()
    data class Table(val rows: List<List<String>>) : MarkdownBlock()
}

private fun parseMarkdownBlocks(input: String): List<MarkdownBlock> {
    val lines = input.trim().lines()
    val blocks = mutableListOf<MarkdownBlock>()
    var index = 0

    while (index < lines.size) {
        if (lines[index].isBlank()) {
            index++
            continue
        }

        if (index + 1 < lines.size && isMarkdownTableRow(lines[index]) && isMarkdownTableSeparator(lines[index + 1])) {
            val rows = mutableListOf(parseMarkdownTableRow(lines[index]))
            index += 2
            while (index < lines.size && isMarkdownTableRow(lines[index])) {
                rows += parseMarkdownTableRow(lines[index])
                index++
            }
            blocks += MarkdownBlock.Table(rows)
            continue
        }

        val paragraphLines = mutableListOf<String>()
        while (index < lines.size && lines[index].isNotBlank()) {
            if (index + 1 < lines.size && isMarkdownTableRow(lines[index]) && isMarkdownTableSeparator(lines[index + 1])) {
                break
            }
            paragraphLines += lines[index]
            index++
        }
        if (paragraphLines.isNotEmpty()) {
            blocks += MarkdownBlock.Paragraph(paragraphLines.joinToString("\n"))
        }
    }

    return blocks.ifEmpty { listOf(MarkdownBlock.Paragraph(input)) }
}

private fun isMarkdownTableRow(line: String): Boolean {
    val trimmed = line.trim()
    return trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.count { it == '|' } >= 2
}

private fun isMarkdownTableSeparator(line: String): Boolean {
    if (!isMarkdownTableRow(line)) return false
    return parseMarkdownTableRow(line).all { cell ->
        cell.matches(Regex(":?-{3,}:?"))
    }
}

private fun parseMarkdownTableRow(line: String): List<String> {
    return line.trim()
        .removePrefix("|")
        .removeSuffix("|")
        .split("|")
        .map { it.trim() }
}

private fun formatMarkdownLite(input: String): AnnotatedString {
    return buildAnnotatedString {
        var index = 0
        while (index < input.length) {
            when {
                input.startsWith("$$", index) -> {
                    val end = input.indexOf("$$", index + 2)
                    if (end > index) {
                        withStyle(SpanStyle(fontFamily = FontFamily.Monospace, color = AppAccentSoft)) {
                            append(input.substring(index + 2, end))
                        }
                        index = end + 2
                    } else {
                        append(input[index])
                        index++
                    }
                }

                input.startsWith("***", index) -> {
                    index = appendDelimited(input, index, "***", SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic))
                }

                input.startsWith("**", index) -> {
                    index = appendDelimited(input, index, "**", SpanStyle(fontWeight = FontWeight.Bold))
                }

                input[index] == '*' -> {
                    index = appendDelimited(input, index, "*", SpanStyle(fontStyle = FontStyle.Italic))
                }

                input[index] == '`' -> {
                    index = appendDelimited(input, index, "`", SpanStyle(fontFamily = FontFamily.Monospace, color = AppAccentSoft))
                }

                input[index] == '$' -> {
                    index = appendDelimited(input, index, "$", SpanStyle(fontFamily = FontFamily.Monospace, color = AppAccentSoft))
                }

                else -> {
                    append(input[index])
                    index++
                }
            }
        }
    }
}

private fun AnnotatedString.Builder.appendDelimited(
    input: String,
    start: Int,
    delimiter: String,
    style: SpanStyle
): Int {
    val contentStart = start + delimiter.length
    val end = input.indexOf(delimiter, contentStart)
    return if (end > contentStart) {
        withStyle(style) {
            append(input.substring(contentStart, end))
        }
        end + delimiter.length
    } else {
        append(input[start])
        start + 1
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PersonaSettingsSheet(
    persona: PersonaUiState,
    groupMembers: List<GroupMember>,
    showSessionHeaderControls: Boolean,
    sessionHeaderName: String,
    sessionHeaderAvatarUri: android.net.Uri?,
    sessionHeaderAvatarScale: Float,
    sessionHeaderAvatarOffsetX: Float,
    sessionHeaderAvatarOffsetY: Float,
    activeMemberId: String,
    responseRounds: Int,
    memoryEnabled: Boolean,
    storyLore: String,
    levelSystemEnabled: Boolean,
    levelXp: Int,
    background: ChatBackground,
    moreOptions: Boolean,
    activeApiKeyLabel: String?,
    tavilyApiKeyLabel: String?,
    rule34UserIdLabel: String?,
    rule34ApiKeyLabel: String?,
    elevenLabsApiKeyLabel: String?,
    elevenLabsVoiceIdLabel: String?,
    elevenLabsModelIdLabel: String?,
    summarizerUsesSeparateKey: Boolean,
    summarizerApiKeyLabel: String?,
    quotaUsage: QuotaUsageState,
    dailyRequestLimit: Int?,
    onBack: () -> Unit,
    onVendorChange: (ApiVendor) -> Unit,
    onSelectMember: (String) -> Unit,
    onAddMember: () -> Unit,
    onRemoveMember: (String) -> Unit,
    onResponseRoundsChange: (Int) -> Unit,
    onMemoryEnabledChange: (Boolean) -> Unit,
    onLevelSystemEnabledChange: (Boolean) -> Unit,
    onNameChange: (String) -> Unit,
    onAuthorChange: (String) -> Unit,
    onTaglineChange: (String) -> Unit,
    onTraitsChange: (List<String>) -> Unit,
    onSessionHeaderNameChange: (String) -> Unit,
    onInstructionModeChange: (InstructionMode) -> Unit,
    onBeginnerRoleChange: (String) -> Unit,
    onBeginnerStyleChange: (String) -> Unit,
    onBeginnerLimitsChange: (String) -> Unit,
    onStoryLoreChange: (String) -> Unit,
    onPromptChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onSafetyLevelChange: (SafetyLevel) -> Unit,
    onThinkingEffortChange: (GeminiThinkingEffort) -> Unit,
    onTemperatureChange: (Float) -> Unit,
    onAvatarChange: (android.net.Uri?) -> Unit,
    onAvatarTransform: (Float, Float, Float) -> Unit,
    onSessionHeaderAvatarChange: (android.net.Uri?) -> Unit,
    onSessionHeaderAvatarTransform: (Float, Float, Float) -> Unit,
    onBackgroundChange: (ChatBackground) -> Unit,
    onCustomBackgroundChange: (android.net.Uri?) -> Unit,
    onToggleMore: () -> Unit,
    onEditApiKey: () -> Unit,
    onClearApiKey: () -> Unit,
    onEditTavilyKey: () -> Unit,
    onClearTavilyKey: () -> Unit,
    onEditRule34UserId: () -> Unit,
    onClearRule34UserId: () -> Unit,
    onEditRule34ApiKey: () -> Unit,
    onClearRule34ApiKey: () -> Unit,
    onEditElevenLabsKey: () -> Unit,
    onClearElevenLabsKey: () -> Unit,
    onEditElevenLabsVoiceId: () -> Unit,
    onClearElevenLabsVoiceId: () -> Unit,
    onEditElevenLabsModelId: () -> Unit,
    onClearElevenLabsModelId: () -> Unit,
    onSummarizerKeyModeChange: (Boolean) -> Unit,
    onEditSummarizerKey: () -> Unit,
    onClearSummarizerKey: () -> Unit,
    onTriggerAutoFill: () -> Unit,
    onDeleteSession: () -> Unit,
    onShareConfig: () -> Unit,
    onSave: () -> Unit
) {
    var instructionPromptExpanded by remember { mutableStateOf(true) }
    var selectedSection by remember { mutableStateOf(PersonaSettingsSection.Context) }
    var showTagPicker by remember { mutableStateOf(false) }
    val imagePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
        onResult = onAvatarChange
    )
    val sessionHeaderImagePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
        onResult = onSessionHeaderAvatarChange
    )
    val backgroundPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
        onResult = onCustomBackgroundChange
    )

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.86f),
        color = AppBackground,
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Close settings",
                        tint = AppTextPrimary
                    )
                }
                Text(
                    text = "Persona Settings",
                    style = MaterialTheme.typography.titleMedium,
                    color = AppTextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onSave) {
                    Text(
                        text = "Save",
                        color = AppAccentSoft,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            PersonaSettingsSectionTabs(
                selected = selectedSection,
                onSelect = { selectedSection = it }
            )

            when (selectedSection) {
                PersonaSettingsSection.Context -> {
                    GroupEditor(
                        members = groupMembers,
                        activeMemberId = activeMemberId,
                        responseRounds = responseRounds,
                        onSelectMember = onSelectMember,
                        onAddMember = onAddMember,
                        onRemoveMember = onRemoveMember,
                        onResponseRoundsChange = onResponseRoundsChange
                    )

                    InstructionSection(
                        persona = persona,
                        storyLore = storyLore,
                        advancedExpanded = instructionPromptExpanded,
                        onAdvancedExpandedChange = { instructionPromptExpanded = it },
                        onModeChange = onInstructionModeChange,
                        onBeginnerRoleChange = onBeginnerRoleChange,
                        onBeginnerStyleChange = onBeginnerStyleChange,
                        onBeginnerLimitsChange = onBeginnerLimitsChange,
                        onStoryLoreChange = onStoryLoreChange,
                        onAdvancedPromptChange = onPromptChange,
                        onTriggerAutoFill = onTriggerAutoFill
                    )

                    SessionMemoryToggle(
                        enabled = memoryEnabled,
                        onEnabledChange = onMemoryEnabledChange
                    )

                    SessionLevelSystem(
                        enabled = levelSystemEnabled,
                        xp = levelXp,
                        onEnabledChange = onLevelSystemEnabledChange
                    )
                }

                PersonaSettingsSection.Ui -> {
                    if (showSessionHeaderControls) {
                        Text(
                            text = "Session header",
                            color = AppTextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                        Text(
                            text = "Changes the group name and top avatar without editing any AI member.",
                            color = AppTextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        SessionHeaderAvatarEditor(
                            fallbackPersona = persona,
                            avatarUri = sessionHeaderAvatarUri,
                            avatarScale = sessionHeaderAvatarScale,
                            avatarOffsetX = sessionHeaderAvatarOffsetX,
                            avatarOffsetY = sessionHeaderAvatarOffsetY,
                            onUpload = { sessionHeaderImagePicker.launch(arrayOf("image/*")) },
                            onTransform = onSessionHeaderAvatarTransform
                        )

                        PersonaTextField(
                            label = "Session name",
                            value = sessionHeaderName,
                            onValueChange = onSessionHeaderNameChange,
                            helper = "This only changes the group header.",
                            placeholder = groupMembers.joinToString(" + ") {
                                it.persona.displayName.ifBlank { "AI" }
                            },
                            counter = "${sessionHeaderName.length}/72",
                            singleLine = true
                        )

                        Text(
                            text = "Active AI",
                            color = AppTextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    AvatarEditor(
                        persona = persona,
                        onUpload = { imagePicker.launch(arrayOf("image/*")) },
                        onTransform = onAvatarTransform
                    )

                    PersonaTextField(
                        label = "Display name",
                        value = persona.displayName,
                        onValueChange = onNameChange,
                        helper = "This is how your AI will appear in chats.",
                        counter = "${persona.displayName.length}/32",
                        singleLine = true
                    )

                    PersonaTextField(
                        label = "Creator",
                        value = persona.author,
                        onValueChange = onAuthorChange,
                        helper = "Who made this character? (e.g. @Aprilisalie)",
                        placeholder = "@You",
                        counter = "${persona.author.length}/32",
                        singleLine = true
                    )

                    PersonaTextField(
                        label = "Description",
                        value = persona.tagline,
                        onValueChange = onTaglineChange,
                        helper = "Short description shown under the character name in Roleplay UI.",
                        placeholder = "e.g. Your teasing roommate",
                        counter = "${persona.tagline.length}/80",
                        singleLine = true
                    )

                    Column(modifier = Modifier.padding(bottom = 18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.tags_label), color = AppTextPrimary, style = MaterialTheme.typography.labelLarge)
                            TextButton(onClick = { showTagPicker = true }) {
                                Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.edit_tags))
                            }
                        }
                        if (persona.traits.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                persona.traits.forEach { tag ->
                                    Surface(
                                        color = Color(0xFFFF5D8F).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = tag,
                                            color = Color(0xFFFF8FA3),
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(stringResource(R.string.no_tags_set), color = AppTextMuted, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    BackgroundOptions(
                        selected = background,
                        onBackgroundChange = onBackgroundChange,
                        onPickCustomBackground = { backgroundPicker.launch(arrayOf("image/*")) }
                    )
                }

                PersonaSettingsSection.ApiVault -> {
                    MoreOptions(
                        expanded = true,
                        showHeader = false,
                        showBackground = false,
                        persona = persona,
                        background = background,
                        activeApiKeyLabel = activeApiKeyLabel,
                        tavilyApiKeyLabel = tavilyApiKeyLabel,
                        rule34UserIdLabel = rule34UserIdLabel,
                        rule34ApiKeyLabel = rule34ApiKeyLabel,
                        elevenLabsApiKeyLabel = elevenLabsApiKeyLabel,
                        elevenLabsVoiceIdLabel = elevenLabsVoiceIdLabel,
                        elevenLabsModelIdLabel = elevenLabsModelIdLabel,
                        summarizerUsesSeparateKey = summarizerUsesSeparateKey,
                        summarizerApiKeyLabel = summarizerApiKeyLabel,
                        quotaUsage = quotaUsage,
                        dailyRequestLimit = dailyRequestLimit,
                        onToggle = onToggleMore,
                        onVendorChange = onVendorChange,
                        onModelChange = onModelChange,
                        onSafetyLevelChange = onSafetyLevelChange,
                        onThinkingEffortChange = onThinkingEffortChange,
                        onTemperatureChange = onTemperatureChange,
                        onBackgroundChange = onBackgroundChange,
                        onPickCustomBackground = { backgroundPicker.launch(arrayOf("image/*")) },
                        onEditApiKey = onEditApiKey,
                        onClearApiKey = onClearApiKey,
                        onEditTavilyKey = onEditTavilyKey,
                        onClearTavilyKey = onClearTavilyKey,
                        onEditRule34UserId = onEditRule34UserId,
                        onClearRule34UserId = onClearRule34UserId,
                        onEditRule34ApiKey = onEditRule34ApiKey,
                        onClearRule34ApiKey = onClearRule34ApiKey,
                        onEditElevenLabsKey = onEditElevenLabsKey,
                        onClearElevenLabsKey = onClearElevenLabsKey,
                        onEditElevenLabsVoiceId = onEditElevenLabsVoiceId,
                        onClearElevenLabsVoiceId = onClearElevenLabsVoiceId,
                        onEditElevenLabsModelId = onEditElevenLabsModelId,
                        onClearElevenLabsModelId = onClearElevenLabsModelId,
                        onSummarizerKeyModeChange = onSummarizerKeyModeChange,
                        onEditSummarizerKey = onEditSummarizerKey,
                        onClearSummarizerKey = onClearSummarizerKey
                    )
                }
            }

            TextButton(
                onClick = onShareConfig,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
            ) {
                Text(
                    text = "Share Companion Config",
                    color = Color(0xFF6B8BFF)
                )
            }

            TextButton(
                onClick = onDeleteSession,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 18.dp)
            ) {
                Text(
                    text = "Delete this session",
                    color = Color(0xFFFF7E8B)
                )
            }
        }
    }

    TagPickerDialog(
        visible = showTagPicker,
        selectedTags = persona.traits,
        onDismiss = { showTagPicker = false },
        onConfirm = { tags ->
            onTraitsChange(tags)
            showTagPicker = false
        }
    )
}

@Composable
private fun PersonaSettingsSectionTabs(
    selected: PersonaSettingsSection,
    onSelect: (PersonaSettingsSection) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 12.dp)
    ) {
        PersonaSettingsSection.entries.forEach { section ->
            val isSelected = selected == section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(section) }
                    .padding(top = 8.dp)
            ) {
                Text(
                    text = section.label,
                    color = if (isSelected) AppAccentSoft else AppTextPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(if (isSelected) AppAccent else AppStroke.copy(alpha = 0.45f))
                )
            }
        }
    }
}

@Composable
private fun GroupEditor(
    members: List<GroupMember>,
    activeMemberId: String,
    responseRounds: Int,
    onSelectMember: (String) -> Unit,
    onAddMember: () -> Unit,
    onRemoveMember: (String) -> Unit,
    onResponseRoundsChange: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(top = 10.dp, bottom = 18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AI MEMBERS (${members.size}/4)",
                color = AppTextPrimary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f)
            )
        }
        Surface(
            color = AppSurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, AppStroke),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
        ) {
            Column {
                members.forEachIndexed { index, member ->
                    GroupMemberRow(
                        member = member,
                        selected = member.id == activeMemberId,
                        canRemove = members.size > 1,
                        onSelect = { onSelectMember(member.id) },
                        onRemove = { onRemoveMember(member.id) }
                    )
                    if (index != members.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(AppStroke.copy(alpha = 0.48f))
                        )
                    }
                }
                if (members.size < 4) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(AppStroke.copy(alpha = 0.48f))
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onAddMember)
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = AppAccentSoft,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add AI member",
                            color = AppAccentSoft,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        Text(
            text = "AI TURNS",
            color = AppTextPrimary,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 18.dp)
        )
        Slider(
            value = responseRounds.toFloat(),
            onValueChange = { onResponseRoundsChange(it.toInt().coerceIn(1, 3)) },
            valueRange = 1f..3f,
            steps = 1,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Each turn picks one AI to answer. Multi-AI chats rotate speakers before your next turn.",
            color = AppTextSecondary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun GroupMemberRow(
    member: GroupMember,
    selected: Boolean,
    canRemove: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(persona = member.persona, size = 38)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = member.persona.displayName,
                color = if (selected) AppAccentSoft else AppTextPrimary,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2ECC71))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Online",
                    color = AppTextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        if (canRemove) {
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Remove AI",
                    tint = Color(0xFFFFA0AA),
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = null,
                tint = AppTextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun SessionHeaderAvatarEditor(
    fallbackPersona: PersonaUiState,
    avatarUri: android.net.Uri?,
    avatarScale: Float,
    avatarOffsetX: Float,
    avatarOffsetY: Float,
    onUpload: () -> Unit,
    onTransform: (Float, Float, Float) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 20.dp)
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .pointerInput(avatarUri) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            onTransform(zoom, pan.x, pan.y)
                        }
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(AppAccentSoft.copy(alpha = 0.45f), AppSurface2)
                        )
                    )
                    .border(3.dp, AppAccentSoft, CircleShape)
                    .padding(4.dp)
            ) {
                SessionHeaderAvatar(
                    fallbackPersona = fallbackPersona,
                    avatarUri = avatarUri,
                    avatarScale = avatarScale,
                    avatarOffsetX = avatarOffsetX,
                    avatarOffsetY = avatarOffsetY,
                    size = 142,
                    modifier = Modifier.fillMaxSize()
                )
            }
            IconButton(
                onClick = onUpload,
                modifier = Modifier
                    .offset(x = 2.dp, y = 2.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AppSurface2)
                    .border(1.dp, AppStroke, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Upload,
                    contentDescription = "Upload session avatar",
                    tint = AppAccentSoft
                )
            }
        }

        Button(
            onClick = onUpload,
            colors = ButtonDefaults.buttonColors(containerColor = AppSurface),
            border = BorderStroke(1.dp, AppStroke),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp)
                .height(54.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Upload,
                contentDescription = null,
                tint = AppAccentSoft
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Upload session avatar",
                color = AppAccentSoft,
                style = MaterialTheme.typography.labelLarge
            )
        }
        Text(
            text = "JPG, PNG or WebP. Pinch and drag to crop.",
            color = AppTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun AvatarEditor(
    persona: PersonaUiState,
    onUpload: () -> Unit,
    onTransform: (Float, Float, Float) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 20.dp)
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .pointerInput(persona.avatarUri) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            onTransform(zoom, pan.x, pan.y)
                        }
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(AppAccentSoft.copy(alpha = 0.45f), AppSurface2)
                        )
                    )
                    .border(3.dp, AppAccentSoft, CircleShape)
                    .padding(4.dp)
            ) {
                Avatar(
                    persona = persona,
                    size = 142,
                    modifier = Modifier.fillMaxSize()
                )
            }
            IconButton(
                onClick = onUpload,
                modifier = Modifier
                    .offset(x = 2.dp, y = 2.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AppSurface2)
                    .border(1.dp, AppStroke, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Upload,
                    contentDescription = "Upload avatar",
                    tint = AppAccentSoft
                )
            }
        }

        Button(
            onClick = onUpload,
            colors = ButtonDefaults.buttonColors(containerColor = AppSurface),
            border = BorderStroke(1.dp, AppStroke),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp)
                .height(54.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Upload,
                contentDescription = null,
                tint = AppAccentSoft
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Upload avatar",
                color = AppAccentSoft,
                style = MaterialTheme.typography.labelLarge
            )
        }
        Text(
            text = "JPG, PNG or WebP. Pinch and drag to crop.",
            color = AppTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun PersonaTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    helper: String,
    placeholder: String? = null,
    counter: String? = null,
    singleLine: Boolean,
    minLines: Int = 1
) {
    Column(modifier = Modifier.padding(bottom = 18.dp)) {
        val hasHeader = label.isNotBlank() || counter != null
        if (hasHeader) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    color = AppTextPrimary,
                    style = MaterialTheme.typography.labelLarge
                )
                counter?.let {
                    Text(
                        text = it,
                        color = AppTextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = minLines,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (hasHeader) 8.dp else 0.dp),
            placeholder = placeholder?.let { placeholderText ->
                {
                    Text(
                        text = placeholderText,
                        color = AppTextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = AppTextPrimary,
                unfocusedTextColor = AppTextPrimary,
                focusedBorderColor = AppAccent.copy(alpha = 0.7f),
                unfocusedBorderColor = AppStroke,
                cursorColor = AppAccent,
                focusedContainerColor = AppSurface,
                unfocusedContainerColor = AppSurface
            )
        )
        Text(
            text = helper,
            color = AppTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun AutoFillPromptDialog(
    visible: Boolean,
    isGenerating: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    if (!visible) return
    var promptText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        title = { Text(stringResource(R.string.autofill_dialog_title)) },
        text = {
            Column {
                Text(
                    text = "Describe your scenario and characters. Gemini will automatically fill in the story lore, role, style, and limits.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                if (isGenerating) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AppAccent)
                    }
                } else {
                    OutlinedTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        placeholder = { Text(stringResource(R.string.autofill_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(promptText) },
                enabled = !isGenerating && promptText.isNotBlank()
            ) {
                Text(stringResource(R.string.btn_generate), color = AppAccent)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isGenerating
            ) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Composable
private fun InstructionSection(
    persona: PersonaUiState,
    storyLore: String,
    advancedExpanded: Boolean,
    onAdvancedExpandedChange: (Boolean) -> Unit,
    onModeChange: (InstructionMode) -> Unit,
    onBeginnerRoleChange: (String) -> Unit,
    onBeginnerStyleChange: (String) -> Unit,
    onBeginnerLimitsChange: (String) -> Unit,
    onStoryLoreChange: (String) -> Unit,
    onAdvancedPromptChange: (String) -> Unit,
    onTriggerAutoFill: () -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Instruction mode",
                color = AppTextPrimary,
                style = MaterialTheme.typography.labelLarge
            )

            TextButton(
                onClick = onTriggerAutoFill,
                colors = ButtonDefaults.textButtonColors(contentColor = AppAccent)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = "AI Auto-fill",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.ai_autofill), style = MaterialTheme.typography.labelMedium)
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            InstructionMode.entries.forEach { mode ->
                ModeChoiceChip(
                    label = mode.label,
                    selected = persona.instructionMode == mode,
                    onClick = { onModeChange(mode) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        PersonaTextField(
            label = "Story lore",
            value = storyLore,
            onValueChange = onStoryLoreChange,
            helper = "Shared by every AI in this session: its canon, rules, locations, factions, and how the world works.",
            placeholder = "Describe the world and the rules that govern it.",
            counter = "${storyLore.length}/16000",
            singleLine = false,
            minLines = 4
        )

        AnimatedVisibility(visible = persona.instructionMode == InstructionMode.Beginner) {
            Column(modifier = Modifier.padding(top = 14.dp)) {
                PersonaTextField(
                    label = "Role",
                    value = persona.beginnerRole,
                    onValueChange = onBeginnerRoleChange,
                    helper = "Who this persona is, what they know, and how they relate to you.",
                    placeholder = "Who this persona is, what they know, and how they relate to you.",
                    singleLine = false,
                    minLines = 2
                )
                PersonaTextField(
                    label = "Style",
                    value = persona.beginnerStyle,
                    onValueChange = onBeginnerStyleChange,
                    helper = "Tone, message length, speaking quirks, and roleplay texture.",
                    placeholder = "Tone, message length, speaking quirks, and roleplay texture.",
                    singleLine = false,
                    minLines = 2
                )
                PersonaTextField(
                    label = "Limits",
                    value = persona.beginnerLimits,
                    onValueChange = onBeginnerLimitsChange,
                    helper = "Boundaries, canon rules, things to avoid, or must-follow details.",
                    singleLine = false,
                    minLines = 2
                )
            }
        }

        AnimatedVisibility(visible = persona.instructionMode == InstructionMode.Advanced) {
            Column(modifier = Modifier.padding(top = 14.dp)) {
                CollapsibleInstructionPrompt(
                    value = persona.instructionPrompt,
                    expanded = advancedExpanded,
                    onExpandedChange = onAdvancedExpandedChange,
                    onValueChange = onAdvancedPromptChange
                )
            }
        }
    }
}

@Composable
private fun ModeChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) AppAccentDim else AppSurface,
        border = BorderStroke(1.dp, if (selected) AppAccent else AppStroke),
        modifier = modifier
            .height(46.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (selected) AppAccentSoft else AppTextPrimary,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CollapsibleInstructionPrompt(
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 18.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onExpandedChange(!expanded) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Instruction Prompt",
                color = AppTextPrimary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (expanded) "Minimize" else "Expand",
                color = AppAccentSoft,
                style = MaterialTheme.typography.labelLarge
            )
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = AppTextSecondary,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .graphicsLayer(rotationZ = if (expanded) 180f else 0f)
            )
        }

        AnimatedVisibility(visible = expanded) {
            PersonaTextField(
                label = "",
                value = value,
                onValueChange = onValueChange,
                helper = "Shape the tone, boundaries, and role for this persona.",
                singleLine = false,
                minLines = 5
            )
        }

        AnimatedVisibility(visible = !expanded) {
            Surface(
                color = AppSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, AppStroke),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clickable { onExpandedChange(true) }
            ) {
                Text(
                    text = value.ifBlank { "No instruction prompt yet." },
                    color = if (value.isBlank()) AppTextSecondary else AppTextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

@Composable
private fun SessionMemoryToggle(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    Surface(
        color = AppSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AppStroke),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Read global memory",
                    color = AppTextPrimary,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "Let this chat use the shared memory block.",
                    color = AppTextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange
            )
        }
    }
}

@Composable
private fun SessionLevelSystem(
    enabled: Boolean,
    xp: Int,
    onEnabledChange: (Boolean) -> Unit
) {
    val state = sessionLevelState(xp)
    Surface(
        color = AppSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AppStroke),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Relationship level",
                        color = AppTextPrimary,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "10 XP per text message. Progress is unique to this session.",
                        color = AppTextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }

            AnimatedVisibility(visible = enabled) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Level ${state.level}",
                            color = AppAccentSoft,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = "${state.xp}/1500 XP",
                            color = AppTextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = state.label,
                        color = AppTextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(AppSurface2)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(state.progress)
                                .fillMaxHeight()
                                .background(AppAccent)
                        )
                    }
                    Text(
                        text = if (state.level >= 10) {
                            "Maximum level reached"
                        } else {
                            "${state.nextLevelXp - state.xp} XP to Level ${state.level + 1}"
                        },
                        color = AppTextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 7.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TraitChips(traits: List<String>) {
    val icons = listOf(
        Icons.Rounded.FavoriteBorder,
        Icons.Rounded.StarBorder,
        Icons.Rounded.Search,
        Icons.Rounded.Shield
    )
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 14.dp)
    ) {
        items(traits.zip(icons)) { (trait, icon) ->
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = AppSurface,
                border = BorderStroke(1.dp, AppStroke)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AppAccentSoft,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = trait,
                        color = AppAccentSoft,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreOptions(
    expanded: Boolean,
    showHeader: Boolean = true,
    showBackground: Boolean = true,
    persona: PersonaUiState,
    background: ChatBackground,
    activeApiKeyLabel: String?,
    tavilyApiKeyLabel: String?,
    rule34UserIdLabel: String?,
    rule34ApiKeyLabel: String?,
    elevenLabsApiKeyLabel: String?,
    elevenLabsVoiceIdLabel: String?,
    elevenLabsModelIdLabel: String?,
    summarizerUsesSeparateKey: Boolean,
    summarizerApiKeyLabel: String?,
    quotaUsage: QuotaUsageState,
    dailyRequestLimit: Int?,
    onToggle: () -> Unit,
    onVendorChange: (ApiVendor) -> Unit,
    onModelChange: (String) -> Unit,
    onSafetyLevelChange: (SafetyLevel) -> Unit,
    onThinkingEffortChange: (GeminiThinkingEffort) -> Unit,
    onTemperatureChange: (Float) -> Unit,
    onBackgroundChange: (ChatBackground) -> Unit,
    onPickCustomBackground: () -> Unit,
    onEditApiKey: () -> Unit,
    onClearApiKey: () -> Unit,
    onEditTavilyKey: () -> Unit,
    onClearTavilyKey: () -> Unit,
    onEditRule34UserId: () -> Unit,
    onClearRule34UserId: () -> Unit,
    onEditRule34ApiKey: () -> Unit,
    onClearRule34ApiKey: () -> Unit,
    onEditElevenLabsKey: () -> Unit,
    onClearElevenLabsKey: () -> Unit,
    onEditElevenLabsVoiceId: () -> Unit,
    onClearElevenLabsVoiceId: () -> Unit,
    onEditElevenLabsModelId: () -> Unit,
    onClearElevenLabsModelId: () -> Unit,
    onSummarizerKeyModeChange: (Boolean) -> Unit,
    onEditSummarizerKey: () -> Unit,
    onClearSummarizerKey: () -> Unit
) {
    Surface(
        color = AppSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AppStroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (showHeader) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onToggle),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "More options",
                        color = AppTextPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = AppTextSecondary,
                        modifier = Modifier.alpha(if (expanded) 1f else 0.82f)
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = if (showHeader) 16.dp else 0.dp)) {
                    VendorDropdown(
                        selected = persona.vendor,
                        onVendorChange = onVendorChange
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ModelDropdown(
                        vendor = persona.vendor,
                        selected = persona.model,
                        onModelChange = onModelChange
                    )
                    if (persona.vendor == ApiVendor.Google) {
                        Spacer(modifier = Modifier.height(12.dp))
                        ThinkingEffortDropdown(
                            selected = persona.thinkingEffort,
                            onThinkingEffortChange = onThinkingEffortChange
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    SafetyDropdown(
                        selected = persona.safetyLevel,
                        enabled = persona.vendor == ApiVendor.Google,
                        onSafetyLevelChange = onSafetyLevelChange
                    )
                    Text(
                        text = "Temperature ${"%.1f".format(persona.temperature)}",
                        color = AppTextPrimary,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Slider(
                        value = persona.temperature,
                        onValueChange = onTemperatureChange,
                        valueRange = 0f..2f,
                        steps = 19,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (showBackground) {
                        BackgroundOptions(
                            selected = background,
                            onBackgroundChange = onBackgroundChange,
                            onPickCustomBackground = onPickCustomBackground
                        )
                    }
                    ApiKeySummary(
                        activeApiKeyLabel = activeApiKeyLabel,
                        tavilyApiKeyLabel = tavilyApiKeyLabel,
                        rule34UserIdLabel = rule34UserIdLabel,
                        rule34ApiKeyLabel = rule34ApiKeyLabel,
                        elevenLabsApiKeyLabel = elevenLabsApiKeyLabel,
                        elevenLabsVoiceIdLabel = elevenLabsVoiceIdLabel,
                        elevenLabsModelIdLabel = elevenLabsModelIdLabel,
                        summarizerUsesSeparateKey = summarizerUsesSeparateKey,
                        summarizerApiKeyLabel = summarizerApiKeyLabel,
                        quotaUsage = quotaUsage,
                        dailyRequestLimit = dailyRequestLimit,
                        onEditApiKey = onEditApiKey,
                        onClearApiKey = onClearApiKey,
                        onEditTavilyKey = onEditTavilyKey,
                        onClearTavilyKey = onClearTavilyKey,
                        onEditRule34UserId = onEditRule34UserId,
                        onClearRule34UserId = onClearRule34UserId,
                        onEditRule34ApiKey = onEditRule34ApiKey,
                        onClearRule34ApiKey = onClearRule34ApiKey,
                        onEditElevenLabsKey = onEditElevenLabsKey,
                        onClearElevenLabsKey = onClearElevenLabsKey,
                        onEditElevenLabsVoiceId = onEditElevenLabsVoiceId,
                        onClearElevenLabsVoiceId = onClearElevenLabsVoiceId,
                        onEditElevenLabsModelId = onEditElevenLabsModelId,
                        onClearElevenLabsModelId = onClearElevenLabsModelId,
                        onSummarizerKeyModeChange = onSummarizerKeyModeChange,
                        onEditSummarizerKey = onEditSummarizerKey,
                        onClearSummarizerKey = onClearSummarizerKey
                    )
                }
            }
        }
    }
}

@Composable
private fun BackgroundOptions(
    selected: ChatBackground,
    onBackgroundChange: (ChatBackground) -> Unit,
    onPickCustomBackground: () -> Unit
) {
    Text(
        text = "Background",
        color = AppTextPrimary,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )

    BackgroundPreview(background = selected)

    val presets = listOf(
        "Darkmode" to ChatBackground.DarkMode,
        "Black" to ChatBackground.PureBlack,
        "Background white" to ChatBackground.PresetWhite,
        "Background black" to ChatBackground.PresetBlack
    )

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 10.dp)
    ) {
        items(presets) { (label, background) ->
            BackgroundChoice(
                label = label,
                selected = selected.sameChoice(background),
                onClick = { onBackgroundChange(background) }
            )
        }
        item {
            BackgroundChoice(
                label = "Upload photo",
                selected = selected is ChatBackground.CustomImage,
                onClick = onPickCustomBackground
            )
        }
    }
}

@Composable
private fun BackgroundPreview(background: ChatBackground) {
    Surface(
        color = AppSurface2,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AppStroke),
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            ChatBackgroundLayer(background = background)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.48f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Crop preview",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun BackgroundChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (selected) AppAccentDim else AppSurface2,
        border = BorderStroke(1.dp, if (selected) AppAccent else AppStroke),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = if (selected) AppAccentSoft else AppTextPrimary,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

private fun ChatBackground.sameChoice(other: ChatBackground): Boolean {
    return this::class == other::class
}

private fun ChatSession.groupTitle(): String {
    val members = members.ifEmpty { listOf(GroupMember(persona = persona)) }
    return members.joinToString(" + ") { it.persona.displayName.ifBlank { "AI" } }
}

private fun ChatSession.displayTitle(): String {
    return title.ifBlank { groupTitle().ifBlank { "Unnamed chat" } }
}

private fun ChatSession.matchesSessionQuery(query: String): Boolean {
    val normalized = query.lowercase(Locale.getDefault())
    if (title.lowercase(Locale.getDefault()).contains(normalized)) return true
    if (groupTitle().lowercase(Locale.getDefault()).contains(normalized)) return true
    if (preview.lowercase(Locale.getDefault()).contains(normalized)) return true
    return messages.any { message ->
        message.content.lowercase(Locale.getDefault()).contains(normalized) ||
            message.speakerName.orEmpty().lowercase(Locale.getDefault()).contains(normalized)
    }
}

private fun ProjectUiState.matchesProjectQuery(query: String): Boolean {
    val normalized = query.lowercase(Locale.getDefault())
    return name.lowercase(Locale.getDefault()).contains(normalized) ||
        description.lowercase(Locale.getDefault()).contains(normalized) ||
        instruction.lowercase(Locale.getDefault()).contains(normalized)
}

@Composable
private fun ApiKeySummary(
    activeApiKeyLabel: String?,
    tavilyApiKeyLabel: String?,
    rule34UserIdLabel: String?,
    rule34ApiKeyLabel: String?,
    elevenLabsApiKeyLabel: String?,
    elevenLabsVoiceIdLabel: String?,
    elevenLabsModelIdLabel: String?,
    summarizerUsesSeparateKey: Boolean,
    summarizerApiKeyLabel: String?,
    quotaUsage: QuotaUsageState,
    dailyRequestLimit: Int?,
    onEditApiKey: () -> Unit,
    onClearApiKey: () -> Unit,
    onEditTavilyKey: () -> Unit,
    onClearTavilyKey: () -> Unit,
    onEditRule34UserId: () -> Unit,
    onClearRule34UserId: () -> Unit,
    onEditRule34ApiKey: () -> Unit,
    onClearRule34ApiKey: () -> Unit,
    onEditElevenLabsKey: () -> Unit,
    onClearElevenLabsKey: () -> Unit,
    onEditElevenLabsVoiceId: () -> Unit,
    onClearElevenLabsVoiceId: () -> Unit,
    onEditElevenLabsModelId: () -> Unit,
    onClearElevenLabsModelId: () -> Unit,
    onSummarizerKeyModeChange: (Boolean) -> Unit,
    onEditSummarizerKey: () -> Unit,
    onClearSummarizerKey: () -> Unit
) {
    Text(
        text = "API keys",
        color = AppTextPrimary,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)
    )
    Surface(
        color = AppSurface2,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AppStroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            ApiKeySlot(
                label = "Model key",
                keyLabel = activeApiKeyLabel,
                emptyText = "No key saved",
                onEdit = onEditApiKey,
                onClear = onClearApiKey
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = AppBackground.copy(alpha = 0.32f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, AppStroke),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Separate summarizer key",
                            color = AppTextPrimary,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = if (summarizerUsesSeparateKey) {
                                "Archive calls use their own Gemini key."
                            } else {
                                "Archive calls use the saved Google key."
                            },
                            color = AppTextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = summarizerUsesSeparateKey,
                        onCheckedChange = onSummarizerKeyModeChange
                    )
                }
            }
            if (summarizerUsesSeparateKey) {
                Spacer(modifier = Modifier.height(8.dp))
                ApiKeySlot(
                    label = "Gemini summarizer key",
                    keyLabel = summarizerApiKeyLabel,
                    emptyText = "No summarizer key saved",
                    onEdit = onEditSummarizerKey,
                    onClear = onClearSummarizerKey
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            ApiKeySlot(
                label = "Tavily search key",
                keyLabel = tavilyApiKeyLabel,
                emptyText = "No Tavily key saved",
                onEdit = onEditTavilyKey,
                onClear = onClearTavilyKey
            )
            Spacer(modifier = Modifier.height(8.dp))
            ApiKeySlot(
                label = "Rule34 User ID",
                keyLabel = rule34UserIdLabel,
                emptyText = "No Rule34 User ID saved",
                onEdit = onEditRule34UserId,
                onClear = onClearRule34UserId
            )
            Spacer(modifier = Modifier.height(8.dp))
            ApiKeySlot(
                label = "Rule34 API Key",
                keyLabel = rule34ApiKeyLabel,
                emptyText = "No Rule34 API key saved",
                onEdit = onEditRule34ApiKey,
                onClear = onClearRule34ApiKey
            )
            Spacer(modifier = Modifier.height(8.dp))
            ApiKeySlot(
                label = "ElevenLabs API key",
                keyLabel = elevenLabsApiKeyLabel,
                emptyText = "No ElevenLabs key saved",
                onEdit = onEditElevenLabsKey,
                onClear = onClearElevenLabsKey
            )
            Spacer(modifier = Modifier.height(8.dp))
            ApiKeySlot(
                label = "ElevenLabs voice ID",
                keyLabel = elevenLabsVoiceIdLabel,
                emptyText = "No ElevenLabs voice ID saved",
                onEdit = onEditElevenLabsVoiceId,
                onClear = onClearElevenLabsVoiceId
            )
            Spacer(modifier = Modifier.height(8.dp))
            ApiKeySlot(
                label = "ElevenLabs model",
                keyLabel = elevenLabsModelIdLabel,
                emptyText = "eleven_multilingual_v2",
                onEdit = onEditElevenLabsModelId,
                onClear = onClearElevenLabsModelId
            )
            QuotaMonitor(
                usage = quotaUsage,
                dailyRequestLimit = dailyRequestLimit
            )
        }
    }
}

@Composable
private fun ApiKeySlot(
    label: String,
    keyLabel: String?,
    emptyText: String,
    onEdit: () -> Unit,
    onClear: () -> Unit
) {
    Surface(
        color = AppBackground.copy(alpha = 0.32f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AppStroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                color = AppTextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = keyLabel ?: emptyText,
                    color = AppTextPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                if (keyLabel != null) {
                    Surface(
                        color = AppAccentDim,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "Saved",
                            color = AppAccentSoft,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Row(modifier = Modifier.padding(top = 4.dp)) {
                TextButton(onClick = onEdit) {
                    Text(
                        text = if (keyLabel == null) "Add key" else "Replace key",
                        color = AppAccentSoft
                    )
                }
                if (keyLabel != null) {
                    TextButton(onClick = onClear) {
                        Text(
                            text = "Clear",
                            color = Color(0xFFFFA0AA)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuotaMonitor(
    usage: QuotaUsageState,
    dailyRequestLimit: Int?
) {
    var resetText by remember { mutableStateOf(formatPacificResetCountdown()) }
    LaunchedEffect(Unit) {
        while (true) {
            resetText = formatPacificResetCountdown()
            delay(1_000)
        }
    }
    val requestText = if (dailyRequestLimit == null) {
        "${usage.requestsToday} requests today"
    } else {
        "${usage.requestsToday} / $dailyRequestLimit RPD"
    }
    Surface(
        color = AppBackground.copy(alpha = 0.5f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AppStroke),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Local quota monitor",
                color = AppTextPrimary,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = requestText,
                color = AppAccentSoft,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Tokens today ${usage.totalTokensToday} · Last ${usage.lastTotalTokens}",
                color = AppTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = resetText,
                color = AppTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

private fun formatPacificResetCountdown(): String {
    val localZone = ZoneId.systemDefault()
    val pacificZone = ZoneId.of("America/Los_Angeles")
    val nowLocal = ZonedDateTime.now(localZone)
    val nextPacificReset = nowLocal
        .withZoneSameInstant(pacificZone)
        .toLocalDate()
        .plusDays(1)
        .atStartOfDay(pacificZone)
    val localReset = nextPacificReset.withZoneSameInstant(localZone)
    val duration = Duration.between(nowLocal, localReset).coerceAtLeast(Duration.ZERO)
    val hours = duration.toHours()
    val minutes = duration.minusHours(hours).toMinutes()
    val seconds = duration.minusHours(hours).minusMinutes(minutes).seconds
    val time = localReset.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
    return "RPD reset $time local · %02d:%02d:%02d".format(hours, minutes, seconds)
}

@Composable
private fun LanguagePickerSection(
    languageCode: String,
    onLanguageChange: (String) -> Unit,
    surfaceColor: Color = AppSurface,
    surface2Color: Color = AppSurface2,
    strokeColor: Color = AppStroke,
    textPrimaryColor: Color = AppTextPrimary,
    textSecondaryColor: Color = AppTextSecondary,
    accentColor: Color = Color(0xFFFF5D8F)
) {
    Surface(
        color = surfaceColor,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, strokeColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.language_label),
                color = textPrimaryColor,
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val isEn = languageCode != "vi"
                Surface(
                    color = if (isEn) accentColor else surface2Color,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onLanguageChange("en") }
                ) {
                    Text(
                        text = stringResource(R.string.lang_english),
                        textAlign = TextAlign.Center,
                        color = if (isEn) Color.White else textSecondaryColor,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Surface(
                    color = if (!isEn) accentColor else surface2Color,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onLanguageChange("vi") }
                ) {
                    Text(
                        text = stringResource(R.string.lang_vietnamese),
                        textAlign = TextAlign.Center,
                        color = if (!isEn) Color.White else textSecondaryColor,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppSettingsDialog(
    selectedName: AppNameChoice,
    selectedIcon: AppIconChoice,
    nsfwModeEnabled: Boolean,
    roleplayUiModeEnabled: Boolean,
    useLightColors: Boolean,
    languageCode: String,
    onNameChange: (AppNameChoice) -> Unit,
    onIconChange: (AppIconChoice) -> Unit,
    onNsfwModeChange: (Boolean) -> Unit,
    onRoleplayUiModeChange: (Boolean) -> Unit,
    onLanguageChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val surfaceColor = if (useLightColors) Color.White else AppSurface
    val surface2Color = if (useLightColors) Color(0xFFF0F0F0) else AppSurface2
    val strokeColor = if (useLightColors) Color(0xFFE0E0E0) else AppStroke
    val textPrimaryColor = if (useLightColors) Color.Black else AppTextPrimary
    val textSecondaryColor = if (useLightColors) Color(0xFF555555) else AppTextSecondary
    val accentColor = if (useLightColors) Color(0xFF57DDF7) else AppAccent
    val accentSoftColor = if (useLightColors) Color(0xFF57DDF7) else AppAccentSoft
    val accentDimColor = if (useLightColors) Color(0xFF57DDF7).copy(alpha = 0.18f) else AppAccentDim
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor,
        titleContentColor = textPrimaryColor,
        textContentColor = textSecondaryColor,
        title = {
            Text(
                text = "App settings",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                LanguagePickerSection(
                    languageCode = languageCode,
                    onLanguageChange = onLanguageChange,
                    surfaceColor = surfaceColor,
                    surface2Color = surface2Color,
                    strokeColor = strokeColor,
                    textPrimaryColor = textPrimaryColor,
                    textSecondaryColor = textSecondaryColor,
                    accentColor = accentColor
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Roleplay UI Mode",
                            color = textPrimaryColor,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = if (roleplayUiModeEnabled) stringResource(R.string.roleplay_ui_mode_active) else stringResource(R.string.roleplay_ui_mode_inactive),
                            color = textSecondaryColor,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                    Switch(
                        checked = roleplayUiModeEnabled,
                        onCheckedChange = onRoleplayUiModeChange
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "NSFW mode",
                            color = textPrimaryColor,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = if (nsfwModeEnabled) {
                                stringResource(R.string.nsfw_mode_active)
                            } else {
                                stringResource(R.string.nsfw_mode_inactive)
                            },
                            color = textSecondaryColor,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                    Switch(
                        checked = nsfwModeEnabled,
                        onCheckedChange = onNsfwModeChange
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "App name",
                    color = textPrimaryColor,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                AppNameChoiceRow(
                    choice = AppNameChoice.Zora,
                    selected = selectedName == AppNameChoice.Zora,
                    onClick = { onNameChange(AppNameChoice.Zora) },
                    surface2Color = surface2Color,
                    strokeColor = strokeColor,
                    textPrimaryColor = textPrimaryColor,
                    accentColor = accentColor,
                    accentDimColor = accentDimColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                AppNameChoiceRow(
                    choice = AppNameChoice.SanLoVerse,
                    selected = selectedName == AppNameChoice.SanLoVerse,
                    onClick = { onNameChange(AppNameChoice.SanLoVerse) },
                    surface2Color = surface2Color,
                    strokeColor = strokeColor,
                    textPrimaryColor = textPrimaryColor,
                    accentColor = accentColor,
                    accentDimColor = accentDimColor
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "App icon",
                    color = textPrimaryColor,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                AppIconChoiceRow(
                    choice = AppIconChoice.Minimalist,
                    selected = selectedIcon == AppIconChoice.Minimalist,
                    onClick = { onIconChange(AppIconChoice.Minimalist) },
                    surface2Color = surface2Color,
                    strokeColor = strokeColor,
                    textPrimaryColor = textPrimaryColor,
                    accentColor = accentColor,
                    accentDimColor = accentDimColor
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Done", color = accentSoftColor)
            }
        }
    )
}

@Composable
private fun AboutAppDialog(
    appVersionLabel: String,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        titleContentColor = AppTextPrimary,
        textContentColor = AppTextSecondary,
        title = {
            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = appVersionLabel,
                    color = AppTextPrimary,
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Created by Phan Chí Vỹ.",
                    color = AppTextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "API key setup",
                    color = AppTextPrimary,
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                AboutStep("1. Open the provider dashboard for the model you want to use.")
                AboutStep("2. Create or reveal an API key in that provider account.")
                AboutStep("3. Copy the key, then open Persona settings in this app.")
                AboutStep("4. Pick the matching API vendor and model.")
                AboutStep("5. Tap API keys > Add key, paste it, and save.")
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Provider dashboards",
                    color = AppTextPrimary,
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                AboutLinkRow(
                    label = "Google AI Studio",
                    detail = "Gemini API key",
                    url = "https://aistudio.google.com/app/apikey",
                    onOpen = uriHandler::openUri
                )
                AboutLinkRow(
                    label = "OpenAI Platform",
                    detail = "GPT API keys",
                    url = "https://platform.openai.com/api-keys",
                    onOpen = uriHandler::openUri
                )
                AboutLinkRow(
                    label = "Anthropic Console",
                    detail = "Claude API keys",
                    url = "https://console.anthropic.com/settings/keys",
                    onOpen = uriHandler::openUri
                )
                AboutLinkRow(
                    label = "xAI Console",
                    detail = "Grok API keys",
                    url = "https://console.x.ai/team/default/api-keys",
                    onOpen = uriHandler::openUri
                )
                AboutLinkRow(
                    label = "Mistral Console",
                    detail = "Mistral/Mixtral API keys",
                    url = "https://console.mistral.ai/api-keys",
                    onOpen = uriHandler::openUri
                )
                AboutLinkRow(
                    label = "Tavily Dashboard",
                    detail = "Web search API key",
                    url = "https://app.tavily.com/home",
                    onOpen = uriHandler::openUri
                )
                AboutLinkRow(
                    label = "ElevenLabs",
                    detail = "TTS API key, voice ID, and model ID",
                    url = "https://elevenlabs.io/app/settings/api-keys",
                    onOpen = uriHandler::openUri
                )
                AboutLinkRow(
                    label = "Rule34 account options",
                    detail = "Rule34 User ID and API key",
                    url = "https://rule34.xxx/index.php?page=account&s=options",
                    onOpen = uriHandler::openUri
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Rule34 setup: sign in to Rule34, open account options, copy the numeric User ID and API key, then paste them into Persona settings > API keys. Rule34 image mode is adult-only.",
                    color = AppTextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "ElevenLabs setup: create an API key, copy a Voice ID from your voices, then paste both into Persona settings > API keys. Some voices return 402 unless your ElevenLabs account has the required paid plan or subscription.",
                    color = AppTextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Keep API keys private. Do not post them online or commit them to GitHub.",
                    color = Color(0xFFFFA0AA),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Done", color = AppAccentSoft)
            }
        }
    )
}

@Composable
private fun AboutStep(text: String) {
    Text(
        text = text,
        color = AppTextSecondary,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 3.dp)
    )
}

@Composable
private fun AboutLinkRow(
    label: String,
    detail: String,
    url: String,
    onOpen: (String) -> Unit
) {
    Surface(
        color = AppSurface2,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AppStroke),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onOpen(url) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = AppTextPrimary,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = detail,
                    color = AppTextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = "Open",
                color = AppAccentSoft,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun AppNameChoiceRow(
    choice: AppNameChoice,
    selected: Boolean,
    onClick: () -> Unit,
    surface2Color: Color = AppSurface2,
    strokeColor: Color = AppStroke,
    textPrimaryColor: Color = AppTextPrimary,
    accentColor: Color = AppAccent,
    accentDimColor: Color = AppAccentDim
) {
    SettingsChoiceRow(
        selected = selected,
        onClick = onClick,
        leading = null,
        label = choice.label,
        surface2Color = surface2Color,
        strokeColor = strokeColor,
        textPrimaryColor = textPrimaryColor,
        accentColor = accentColor,
        accentDimColor = accentDimColor
    )
}

@Composable
private fun AppIconChoiceRow(
    choice: AppIconChoice,
    selected: Boolean,
    onClick: () -> Unit,
    surface2Color: Color = AppSurface2,
    strokeColor: Color = AppStroke,
    textPrimaryColor: Color = AppTextPrimary,
    accentColor: Color = AppAccent,
    accentDimColor: Color = AppAccentDim
) {
    SettingsChoiceRow(
        selected = selected,
        onClick = onClick,
        leading = {
            Image(
                painter = painterResource(id = choice.iconResource()),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        },
        label = choice.label,
        surface2Color = surface2Color,
        strokeColor = strokeColor,
        textPrimaryColor = textPrimaryColor,
        accentColor = accentColor,
        accentDimColor = accentDimColor
    )
}

@Composable
private fun SettingsChoiceRow(
    selected: Boolean,
    onClick: () -> Unit,
    leading: (@Composable () -> Unit)?,
    label: String,
    surface2Color: Color = AppSurface2,
    strokeColor: Color = AppStroke,
    textPrimaryColor: Color = AppTextPrimary,
    accentColor: Color = AppAccent,
    accentDimColor: Color = AppAccentDim
) {
    Surface(
        color = if (selected) accentDimColor else surface2Color,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (selected) accentColor else strokeColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leading?.invoke()
            if (leading != null) {
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = label,
                color = textPrimaryColor,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Surface(
                    color = accentColor,
                    shape = CircleShape,
                    modifier = Modifier.size(10.dp)
                ) {}
            }
        }
    }
}

private fun AppIconChoice.iconResource(): Int {
    return when (this) {
        AppIconChoice.Minimalist -> R.drawable.app_icon_minimalist
    }
}

private fun createCameraImageUri(context: android.content.Context): android.net.Uri {
    val directory = File(context.cacheDir, "camera").apply { mkdirs() }
    val imageFile = File.createTempFile("zora_camera_", ".jpg", directory)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}

@Composable
private fun ApiKeyDialog(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    secureInput: Boolean = true
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = {
            Text(
                text = title,
                color = AppTextPrimary,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    visualTransformation = if (secureInput) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (secureInput) KeyboardType.Password else KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { onSave() }),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTextPrimary,
                        unfocusedTextColor = AppTextPrimary,
                        focusedBorderColor = AppAccent.copy(alpha = 0.7f),
                        unfocusedBorderColor = AppStroke,
                        cursorColor = AppAccent,
                        focusedContainerColor = AppSurface2,
                        unfocusedContainerColor = AppSurface2
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Saved only on this device.",
                    color = AppTextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = value.isNotBlank()
            ) {
                Text(
                    text = "Save",
                    color = AppAccentSoft
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = AppTextSecondary
                )
            }
        }
    )
}

@Composable
private fun RateLimitRetryDialog(
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = {
            Text(
                text = "Model limit hit",
                color = AppTextPrimary,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Text(
                text = "The model returned a rate-limit or quota error. Retry the last message?",
                color = AppTextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text(text = "Retry", color = AppAccentSoft)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = AppTextSecondary)
            }
        }
    )
}

@Composable
private fun FullscreenImageViewer(
    uri: android.net.Uri,
    onDismiss: () -> Unit
) {
    var scale by remember(uri) { mutableStateOf(1f) }
    var offsetX by remember(uri) { mutableStateOf(0f) }
    var offsetY by remember(uri) { mutableStateOf(0f) }

    fun resetZoom() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.94f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = rememberAsyncImagePainter(uri),
            contentDescription = "Full image",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                }
                .pointerInput(uri) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val nextScale = (scale * zoom).coerceIn(1f, 5f)
                        scale = nextScale
                        if (nextScale <= 1.01f) {
                            resetZoom()
                        } else {
                            offsetX = (offsetX + pan.x)
                                .coerceIn(-size.width * nextScale, size.width * nextScale)
                            offsetY = (offsetY + pan.y)
                                .coerceIn(-size.height * nextScale, size.height * nextScale)
                        }
                    }
                }
                .pointerInput(uri) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1.01f) {
                                resetZoom()
                            } else {
                                scale = 2f
                            }
                        }
                    )
                }
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 12.dp, end = 12.dp)
                .background(Color.Black.copy(alpha = 0.42f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close image",
                tint = Color.White
            )
        }
        Text(
            text = "Pinch or double-tap to zoom",
            color = Color.White.copy(alpha = 0.74f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 22.dp)
        )
        Text(
            text = if (scale > 1.01f) "Double-tap to reset" else "Use X to close",
            color = Color.White.copy(alpha = 0.72f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        )
    }
}

@Composable
private fun VendorDropdown(
    selected: ApiVendor,
    onVendorChange: (ApiVendor) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    LabeledMenuBox(
        label = "API vendor",
        value = selected.label,
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        ApiVendor.entries.forEach { vendor ->
            DropdownMenuItem(
                text = { Text(text = vendor.label, color = AppTextPrimary) },
                onClick = {
                    onVendorChange(vendor)
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun SafetyDropdown(
    selected: SafetyLevel,
    enabled: Boolean,
    onSafetyLevelChange: (SafetyLevel) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    LabeledMenuBox(
        label = "Safety level",
        value = if (enabled) selected.label else "Provider default",
        expanded = expanded && enabled,
        enabled = enabled,
        onExpandedChange = { expanded = it }
    ) {
        SafetyLevel.entries.forEach { level ->
            DropdownMenuItem(
                text = { Text(text = level.label, color = AppTextPrimary) },
                onClick = {
                    onSafetyLevelChange(level)
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun ThinkingEffortDropdown(
    selected: GeminiThinkingEffort,
    onThinkingEffortChange: (GeminiThinkingEffort) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    LabeledMenuBox(
        label = "Thinking level",
        value = selected.label,
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        GeminiThinkingEffort.entries.forEach { effort ->
            DropdownMenuItem(
                text = { Text(text = effort.label, color = AppTextPrimary) },
                onClick = {
                    onThinkingEffortChange(effort)
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun ModelDropdown(
    vendor: ApiVendor,
    selected: String,
    onModelChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val models = vendor.modelOptions
    LabeledMenuBox(
        label = "Model",
        value = selected,
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        models.forEach { model ->
            DropdownMenuItem(
                text = { Text(text = model, color = AppTextPrimary) },
                onClick = {
                    onModelChange(model)
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun LabeledMenuBox(
    label: String,
    value: String,
    expanded: Boolean,
    enabled: Boolean = true,
    onExpandedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Text(
        text = label,
        color = AppTextPrimary,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Box {
        Surface(
            color = if (enabled) AppSurface2 else AppSurface2.copy(alpha = 0.55f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, AppStroke),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onExpandedChange(true) }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    color = AppTextPrimary,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = "Choose model",
                    tint = AppTextSecondary
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.background(AppSurface2)
        ) {
            content()
        }
    }
}

@Composable
private fun SessionHeaderAvatar(
    fallbackPersona: PersonaUiState,
    avatarUri: android.net.Uri?,
    avatarScale: Float,
    avatarOffsetX: Float,
    avatarOffsetY: Float,
    size: Int,
    modifier: Modifier = Modifier
) {
    if (avatarUri == null) {
        Avatar(
            persona = fallbackPersona,
            size = size,
            modifier = modifier
        )
        return
    }

    Box(
        modifier = modifier
            .size(size.dp)
            .aspectRatio(1f)
            .clip(CircleShape)
    ) {
        Image(
            painter = rememberAsyncImagePainter(avatarUri),
            contentDescription = "Session avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = avatarScale,
                    scaleY = avatarScale,
                    translationX = avatarOffsetX,
                    translationY = avatarOffsetY
                )
        )
    }
}

@Composable
private fun Avatar(
    persona: PersonaUiState,
    size: Int,
    modifier: Modifier = Modifier
) {
    val painter = if (persona.avatarUri != null) {
        rememberAsyncImagePainter(persona.avatarUri)
    } else {
        painterResource(id = R.drawable.default_avatar)
    }

    Box(
        modifier = modifier
            .size(size.dp)
            .aspectRatio(1f)
            .clip(CircleShape)
    ) {
        Image(
            painter = painter,
            contentDescription = "${persona.displayName} avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = persona.avatarScale,
                    scaleY = persona.avatarScale,
                    translationX = persona.avatarOffsetX,
                    translationY = persona.avatarOffsetY
                )
        )
    }
}

// ==========================================
// ROLEPLAY HUB UI MODE (DOKICHAT LAYOUT)
// ==========================================

private enum class RoleplayTab {
    Discover,
    Chats,
    Create,
    Settings
}

private data class PresetCharacter(
    val sessionId: String? = null,
    val name: String,
    val author: String,
    val tagline: String,
    val greeting: String,
    val prompt: String,
    val tags: List<String>,
    val avatarUri: android.net.Uri? = null,
    val isFavorite: Boolean = false,
    val pinnedAtMillis: Long? = null
)

private val PresetCharacters = emptyList<PresetCharacter>()

private data class RoleplayColors(
    val background: Color,
    val surface: Color,
    val surface2: Color,
    val stroke: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val actionAccent: Color,
    val accentSoft: Color,
    val buttonCyan: Color,
    val isLight: Boolean
)

private val RoleplayDarkColors = RoleplayColors(
    background = AppBackground,
    surface = AppSurface,
    surface2 = AppSurface2,
    stroke = AppStroke,
    textPrimary = AppTextPrimary,
    textSecondary = AppTextSecondary,
    accent = Color(0xFFFF5D8F),
    actionAccent = AppAccentSoft,
    accentSoft = AppAccentSoft,
    buttonCyan = Color(0xFF80DBEE),
    isLight = false
)

private val RoleplayLightColors = RoleplayColors(
    background = Color(0xFFF8F8F8),
    surface = Color.White,
    surface2 = Color(0xFFF0F0F0),
    stroke = Color(0xFFE0E0E0),
    textPrimary = Color.Black,
    textSecondary = Color(0xFF555555),
    accent = Color(0xFFE60060),
    actionAccent = Color(0xFFE60060),
    accentSoft = Color(0xFF57DDF7),
    buttonCyan = Color(0xFF80DBEE),
    isLight = true
)

private val LocalRoleplayColors = staticCompositionLocalOf { RoleplayDarkColors }

@Composable
private fun RoleplayHubLayout(
    viewModel: ChatViewModel,
    activeTab: RoleplayTab,
    onTabChange: (RoleplayTab) -> Unit,
    onOpenChat: () -> Unit,
    onImportSession: () -> Unit,
    onImportConfig: () -> Unit
) {
    val genderAccent = when (viewModel.userProfileGender) {
        "female" -> Color(0xFFF858A3)
        "male" -> Color(0xFF57DDF7)
        else -> Color(0xFF80DBEE)
    }
    val baseColors = if (viewModel.roleplayLightModeEnabled) RoleplayLightColors else RoleplayDarkColors
    val colors = if (baseColors.isLight) baseColors.copy(accent = genderAccent) else baseColors

    CompositionLocalProvider(LocalRoleplayColors provides colors) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LocalRoleplayColors.current.background)
                .statusBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (activeTab) {
                    RoleplayTab.Discover -> {
                        DiscoverScreen(
                            viewModel = viewModel,
                            onSelectCharacter = { preset ->
                                val existing = if (preset.sessionId != null) {
                                    viewModel.sessions.firstOrNull { it.id == preset.sessionId }
                                } else {
                                    viewModel.sessions.firstOrNull { it.persona.displayName.equals(preset.name, ignoreCase = true) }
                                }
                                if (existing != null) {
                                    viewModel.selectSession(existing.id)
                                    onOpenChat()
                                } else {
                                    viewModel.createSessionWithPersona(
                                        name = preset.name,
                                        tagline = preset.tagline,
                                        prompt = preset.prompt,
                                        tags = preset.tags,
                                        greeting = preset.greeting
                                    )
                                    onOpenChat()
                                }
                            }
                        )
                    }
                    RoleplayTab.Chats -> {
                        ChatsScreen(
                            viewModel = viewModel,
                            onSelectSession = { sessionId ->
                                viewModel.selectSession(sessionId)
                                onOpenChat()
                            }
                        )
                    }
                    RoleplayTab.Create -> {
                        CreateScreen(
                            viewModel = viewModel,
                            onCreated = onOpenChat,
                            onImportSession = onImportSession,
                            onImportConfig = onImportConfig
                        )
                    }
                    RoleplayTab.Settings -> {
                        RoleplaySettingsScreen(viewModel = viewModel)
                    }
                }
            }
            RoleplayBottomBar(activeTab = activeTab, onTabChange = onTabChange)
        }
    }
}

@Composable
private fun RoleplayBottomBar(
    activeTab: RoleplayTab,
    onTabChange: (RoleplayTab) -> Unit
) {
    val items = listOf(
        Triple(RoleplayTab.Discover, stringResource(R.string.nav_discover), Icons.Rounded.Public),
        Triple(RoleplayTab.Chats, stringResource(R.string.nav_chats), Icons.Rounded.ChatBubbleOutline),
        Triple(RoleplayTab.Create, stringResource(R.string.nav_create), Icons.Rounded.Add),
        Triple(RoleplayTab.Settings, stringResource(R.string.nav_settings), Icons.Rounded.Settings)
    )

    Surface(
        color = LocalRoleplayColors.current.surface,
        border = BorderStroke(1.dp, LocalRoleplayColors.current.stroke.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (tab, label, icon) ->
                val selected = activeTab == tab
                val tint = if (selected) LocalRoleplayColors.current.accent else LocalRoleplayColors.current.textSecondary

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabChange(tab) }
                        )
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        color = tint,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun DiscoverScreen(
    viewModel: ChatViewModel,
    onSelectCharacter: (PresetCharacter) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf("All Tags") }
    var actionTarget by remember { mutableStateOf<ChatSession?>(null) }
    var detailTarget by remember { mutableStateOf<PresetCharacter?>(null) }
    var communitySource by remember { mutableStateOf(false) }

    val customCharacters = viewModel.sessions.filter { session ->
        session.persona.displayName.isNotBlank()
    }.sortedWith(
        compareByDescending<ChatSession> { it.pinnedAtMillis ?: 0L }
            .thenByDescending { it.isFavorite }
    ).map { session ->
        PresetCharacter(
            sessionId = session.id,
            name = session.persona.displayName,
            author = session.persona.author.ifBlank { "@You" },
            tagline = session.persona.tagline.ifBlank { "Custom character" },
            greeting = session.preview.takeIf { it != "No messages yet" && it.isNotBlank() } ?: "",
            prompt = session.persona.instructionPrompt,
            tags = session.persona.traits,
            avatarUri = session.persona.avatarUri,
            isFavorite = session.isFavorite,
            pinnedAtMillis = session.pinnedAtMillis
        )
    }

    val allCharacters = PresetCharacters + customCharacters
    val tagsList = remember(customCharacters) {
        val userTags = customCharacters
            .flatMap { it.tags }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key.lowercase() })
            .map { it.key }
        listOf("All Tags", "Pinned", "Favorites") + userTags
    }
    LaunchedEffect(tagsList) {
        if (selectedTag !in tagsList) {
            selectedTag = "All Tags"
        }
    }

    val filteredCharacters = allCharacters.filter { char ->
        val matchesSearch = char.name.contains(searchQuery, ignoreCase = true) ||
                            char.tagline.contains(searchQuery, ignoreCase = true)
        val matchesTag = if (selectedTag == "All Tags") {
            true
        } else if (selectedTag == "Pinned") {
            char.pinnedAtMillis != null
        } else if (selectedTag == "Favorites") {
            char.isFavorite
        } else {
            char.tags.any { it.equals(selectedTag, ignoreCase = true) }
        }
        matchesSearch && matchesTag
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        // Branding Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Zora.AI",
                style = TextStyle(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF8B5CF6), LocalRoleplayColors.current.accent)
                    ),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(stringResource(R.string.search_characters), color = LocalRoleplayColors.current.textSecondary) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = LocalRoleplayColors.current.textSecondary) },
            textStyle = TextStyle(color = LocalRoleplayColors.current.textPrimary),
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = LocalRoleplayColors.current.surface,
                unfocusedContainerColor = LocalRoleplayColors.current.surface,
                focusedBorderColor = LocalRoleplayColors.current.accent,
                unfocusedBorderColor = LocalRoleplayColors.current.stroke
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(false to "Your AI", true to "Community").forEach { (community, label) ->
                val selected = communitySource == community
                Surface(
                    color = if (selected) LocalRoleplayColors.current.accent else LocalRoleplayColors.current.surface,
                    shape = RoundedCornerShape(8.dp),
                    border = if (selected) null else BorderStroke(1.dp, LocalRoleplayColors.current.stroke),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { communitySource = community }
                ) {
                    Text(
                        label,
                        color = if (selected) Color.White else LocalRoleplayColors.current.textSecondary,
                        textAlign = TextAlign.Center,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(vertical = 9.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (communitySource) {
            CommunityDiscoverPane(
                viewModel = viewModel,
                searchQuery = searchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {

        // Tags List
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(tagsList) { tag ->
                val selected = selectedTag == tag
                val chipColor = if (selected) LocalRoleplayColors.current.accent else LocalRoleplayColors.current.surface
                val textColor = if (selected) Color.White else LocalRoleplayColors.current.textSecondary
                val borderStroke = if (selected) null else BorderStroke(1.dp, LocalRoleplayColors.current.stroke)

                Surface(
                    color = chipColor,
                    shape = RoundedCornerShape(16.dp),
                    border = borderStroke,
                    modifier = Modifier.clickable { selectedTag = tag }
                ) {
                    Text(
                        text = when {
                            tag == "All Tags" -> stringResource(R.string.tag_all)
                            tag == "Pinned" -> "Pinned"
                            tag == "Favorites" -> "Favorites"
                            else -> tag
                        },
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filteredCharacters.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = LocalRoleplayColors.current.surface,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, LocalRoleplayColors.current.stroke),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.no_characters_found),
                            color = LocalRoleplayColors.current.textPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.no_characters_found_hint),
                            color = LocalRoleplayColors.current.textSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            val pagerState = rememberPagerState(pageCount = { filteredCharacters.size })
            LaunchedEffect(filteredCharacters.size, searchQuery, selectedTag) {
                if (filteredCharacters.isNotEmpty()) {
                    pagerState.scrollToPage(pagerState.currentPage.coerceIn(0, filteredCharacters.lastIndex))
                }
            }

            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 28.dp),
                pageSpacing = 14.dp,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                page ->
                val character = filteredCharacters[page]
                val pageOffset = (
                    (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                ).let(::abs).coerceIn(0f, 1f)
                val cardScale = 0.92f + ((1f - pageOffset) * 0.08f)
                val cardAlpha = 0.74f + ((1f - pageOffset) * 0.26f)

                CharacterCarouselCard(
                    char = character,
                    pageCount = filteredCharacters.size,
                    pageIndex = page,
                    onChat = { onSelectCharacter(character) },
                    onOpenDetail = { detailTarget = character },
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        actionTarget = character.sessionId?.let { id ->
                            viewModel.sessions.firstOrNull { it.id == id }
                        }
                    },
                    modifier = Modifier
                        .fillMaxHeight()
                        .graphicsLayer {
                            scaleX = cardScale
                            scaleY = cardScale
                            alpha = cardAlpha
                        }
                )
            }
        }
        }
    }

    actionTarget?.let { session ->
        RoleplayActionSheet(
            session = session,
            onDismiss = { actionTarget = null },
            onExport = {
                viewModel.autoExportSession(
                    sessionId = session.id,
                    onSuccess = { path ->
                        android.widget.Toast.makeText(context, "Saved: $path", android.widget.Toast.LENGTH_LONG).show()
                    },
                    onFailure = { err ->
                        android.widget.Toast.makeText(context, "Export failed: $err", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
                actionTarget = null
            },
            onCloneConfig = {
                viewModel.duplicateSessionSettings(session.id)
                actionTarget = null
            },
            onCloneSession = {
                viewModel.cloneSession(session.id)
                actionTarget = null
            },
            onFavorite = {
                viewModel.toggleSessionFavorite(session.id)
                actionTarget = null
            },
            onPin = {
                viewModel.toggleSessionPinned(session.id)
                actionTarget = null
            },
            onPublish = {
                viewModel.publishSessionToHub(session.id)
                actionTarget = null
            },
            onDelete = {
                viewModel.deleteSession(session.id)
                actionTarget = null
            }
        )
    }

    detailTarget?.let { character ->
        val session = character.sessionId?.let { id -> viewModel.sessions.firstOrNull { it.id == id } }
        CharacterDetailSheet(
            character = character,
            session = session,
            onDismiss = { detailTarget = null },
            onChat = {
                detailTarget = null
                onSelectCharacter(character)
            },
            onFavorite = session?.let { currentSession ->
                {
                    viewModel.toggleSessionFavorite(currentSession.id)
                    detailTarget = detailTarget?.copy(isFavorite = !currentSession.isFavorite)
                }
            },
            onPin = session?.let { currentSession ->
                {
                    viewModel.toggleSessionPinned(currentSession.id)
                    detailTarget = detailTarget?.copy(
                        pinnedAtMillis = if (currentSession.pinnedAtMillis == null) {
                            System.currentTimeMillis()
                        } else {
                            null
                        }
                    )
                }
            },
            onExport = session?.let { currentSession ->
                {
                    viewModel.autoExportSession(
                        sessionId = currentSession.id,
                        onSuccess = { path ->
                            android.widget.Toast.makeText(context, "Saved: $path", android.widget.Toast.LENGTH_LONG).show()
                        },
                        onFailure = { err ->
                            android.widget.Toast.makeText(context, "Export failed: $err", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                    detailTarget = null
                }
            },
            onCloneConfig = session?.let { currentSession ->
                {
                    viewModel.duplicateSessionSettings(currentSession.id)
                    detailTarget = null
                }
            },
            onCloneSession = session?.let { currentSession ->
                {
                    viewModel.cloneSession(currentSession.id)
                    detailTarget = null
                }
            },
            onDelete = session?.let { currentSession ->
                {
                    viewModel.deleteSession(currentSession.id)
                    detailTarget = null
                }
            }
        )
    }
}

@Composable
private fun CommunityDiscoverPane(
    viewModel: ChatViewModel,
    searchQuery: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var signInVisible by remember { mutableStateOf(false) }

    fun beginGoogleSignIn() {
        val clientId = context.getString(R.string.google_web_client_id)
        if (clientId.isBlank() || clientId == "CONFIGURE_ME") {
            android.widget.Toast.makeText(context, "Google Sign-In is not configured in this build.", android.widget.Toast.LENGTH_LONG).show()
            return
        }
        coroutineScope.launch {
            runCatching {
                val option = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(clientId)
                    .setAutoSelectEnabled(false)
                    .build()
                val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                val credential = credentialManager.getCredential(context, request).credential
                if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    error("Google did not return a usable account credential.")
                }
                GoogleIdTokenCredential.createFrom(credential.data).idToken
            }.onSuccess { token ->
                signInVisible = false
                viewModel.signInHubWithGoogle(token)
            }.onFailure { cause ->
                android.widget.Toast.makeText(
                    context,
                    cause.message?.take(140) ?: "Google sign-in was cancelled.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    LaunchedEffect(searchQuery, viewModel.hubUsername) {
        delay(400)
        viewModel.refreshHubCharacters(searchQuery)
    }
    LaunchedEffect(viewModel.hubMessage) {
        viewModel.hubMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearHubMessage()
        }
    }

    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                viewModel.hubUsername?.let { "Publishing as @$it" } ?: "Browse and download without an account",
                color = LocalRoleplayColors.current.textSecondary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            if (viewModel.hubUsername == null) {
                TextButton(onClick = { signInVisible = true }) {
                    Text("Sign in to publish", color = LocalRoleplayColors.current.accent, fontWeight = FontWeight.Bold)
                }
            } else {
                TextButton(onClick = viewModel::signOutHub) {
                    Text("Sign out", color = LocalRoleplayColors.current.textSecondary)
                }
                IconButton(onClick = { viewModel.refreshHubCharacters(searchQuery) }) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = LocalRoleplayColors.current.accent)
                }
            }
        }

        if (viewModel.hubLoading && viewModel.hubCharacters.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = LocalRoleplayColors.current.accent)
            }
        } else if (viewModel.hubCharacters.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No shared characters found.", color = LocalRoleplayColors.current.textSecondary)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(viewModel.hubCharacters, key = { it.id }) { character ->
                    CommunityCharacterRow(character = character, onImport = { viewModel.importHubCharacter(character.id) })
                }
            }
        }
    }

    if (signInVisible) {
        AlertDialog(
            onDismissRequest = { if (!viewModel.hubLoading) signInVisible = false },
            title = { Text("Sign in to publish") },
            text = {
                Text(
                    "Google Sign-In is used only to identify who owns a Community upload. " +
                        "Your chats, API keys, sessions, and local characters stay on this device. " +
                        "Browsing and importing never require an account."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !viewModel.hubLoading,
                    onClick = ::beginGoogleSignIn
                ) { Text("Continue with Google") }
            },
            dismissButton = { TextButton(onClick = { signInVisible = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun CommunityCharacterRow(character: HubCharacter, onImport: () -> Unit) {
    Surface(
        color = LocalRoleplayColors.current.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, LocalRoleplayColors.current.stroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(6.dp), color = LocalRoleplayColors.current.surface2, modifier = Modifier.size(72.dp)) {
                if (character.avatarUrl != null) {
                    Image(
                        painter = rememberAsyncImagePainter("https://zora-hub.pages.dev${character.avatarUrl}"),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) { Text(character.displayName.take(1).uppercase(), color = LocalRoleplayColors.current.accent, fontWeight = FontWeight.Bold) }
                }
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(character.displayName, color = LocalRoleplayColors.current.textPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("@${character.author}", color = LocalRoleplayColors.current.textSecondary, style = MaterialTheme.typography.bodySmall)
                Text(character.tagline, color = LocalRoleplayColors.current.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onImport) {
                Icon(Icons.Rounded.FileOpen, contentDescription = "Preview import", tint = LocalRoleplayColors.current.accent)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CharacterCarouselCard(
    char: PresetCharacter,
    pageCount: Int,
    pageIndex: Int,
    onChat: () -> Unit,
    onOpenDetail: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black,
        shape = RoundedCornerShape(30.dp),
        border = BorderStroke(1.dp, LocalRoleplayColors.current.stroke.copy(alpha = 0.75f)),
        shadowElevation = 12.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp)
            .combinedClickable(
                onClick = onOpenDetail,
                onLongClick = onLongPress
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (char.avatarUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(char.avatarUri),
                    contentDescription = "${char.name} avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    LocalRoleplayColors.current.accent.copy(alpha = 0.5f),
                                    Color(0xFF8B5CF6).copy(alpha = 0.35f),
                                    LocalRoleplayColors.current.surface
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = char.name.take(1).uppercase(),
                        style = TextStyle(
                            brush = Brush.horizontalGradient(
                                listOf(LocalRoleplayColors.current.accent, Color(0xFF8B5CF6))
                            ),
                            fontSize = 116.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.06f),
                            0.42f to Color.Black.copy(alpha = 0.08f),
                            0.76f to Color.Black.copy(alpha = 0.58f),
                            1f to Color.Black.copy(alpha = 0.9f)
                        )
                    )
            )

            if (pageCount > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 18.dp, end = 18.dp)
                ) {
                    repeat(pageCount.coerceAtMost(5)) { index ->
                        val selected = if (pageCount <= 5) {
                            index == pageIndex
                        } else {
                            index == (pageIndex * 5 / pageCount).coerceIn(0, 4)
                        }
                        Box(
                            modifier = Modifier
                                .width(if (selected) 16.dp else 7.dp)
                                .height(7.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.White.copy(alpha = if (selected) 0.86f else 0.34f))
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 22.dp)
            ) {
                Text(
                    text = char.name,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CarouselInfoChip(
                        text = char.author,
                        emphasize = true,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (char.tags.isNotEmpty()) {
                        CarouselInfoChip(
                            text = char.tags.first(),
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }

                if (char.tags.size > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        char.tags.drop(1).take(2).forEach { tag ->
                            CarouselInfoChip(
                                text = tag,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = char.greeting.ifBlank { char.tagline },
                    color = Color.White.copy(alpha = 0.86f),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(18.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(LocalRoleplayColors.current.buttonCyan, Color(0xFFB685FF))
                            )
                        )
                        .clickable(onClick = onChat),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chat",
                        color = Color.Black,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun CarouselInfoChip(
    text: String,
    modifier: Modifier = Modifier,
    emphasize: Boolean = false
) {
    Surface(
        color = Color.White.copy(alpha = if (emphasize) 0.22f else 0.18f),
        shape = RoundedCornerShape(999.dp),
        modifier = modifier.height(34.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = text,
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CharacterDetailSheet(
    character: PresetCharacter,
    session: ChatSession?,
    onDismiss: () -> Unit,
    onChat: () -> Unit,
    onFavorite: (() -> Unit)?,
    onPin: (() -> Unit)?,
    onExport: (() -> Unit)?,
    onCloneConfig: (() -> Unit)?,
    onCloneSession: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = LocalRoleplayColors.current.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            border = BorderStroke(1.dp, LocalRoleplayColors.current.stroke),
            shadowElevation = 18.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(42.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(LocalRoleplayColors.current.stroke)
                )
                Row(
                    modifier = Modifier.padding(top = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = LocalRoleplayColors.current.surface2,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, LocalRoleplayColors.current.stroke),
                        modifier = Modifier.size(92.dp)
                    ) {
                        if (character.avatarUri != null) {
                            Image(
                                painter = rememberAsyncImagePainter(character.avatarUri),
                                contentDescription = "${character.name} avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = character.name.take(1).uppercase(),
                                    color = LocalRoleplayColors.current.accent,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = character.name,
                            color = LocalRoleplayColors.current.textPrimary,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = character.author,
                            color = LocalRoleplayColors.current.textSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (character.tags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        character.tags.take(12).forEach { tag ->
                            Surface(
                                color = LocalRoleplayColors.current.accent.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(999.dp),
                                border = BorderStroke(1.dp, LocalRoleplayColors.current.accent.copy(alpha = 0.28f))
                            ) {
                                Text(
                                    text = tag,
                                    color = LocalRoleplayColors.current.accent,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
                Text(
                    text = character.tagline,
                    color = LocalRoleplayColors.current.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    text = character.greeting.ifBlank { character.prompt.ifBlank { "No opening preview yet." } },
                    color = LocalRoleplayColors.current.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Button(
                    onClick = onChat,
                    colors = ButtonDefaults.buttonColors(containerColor = LocalRoleplayColors.current.accent),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp)
                        .height(52.dp)
                ) {
                    Text("Chat", color = Color.White, fontWeight = FontWeight.Bold)
                }
                if (session != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onFavorite?.invoke() },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, LocalRoleplayColors.current.stroke),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (session.isFavorite) "Unfavorite" else "Favorite")
                        }
                        OutlinedButton(
                            onClick = { onPin?.invoke() },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, LocalRoleplayColors.current.stroke),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (session.pinnedAtMillis != null) "Unpin" else "Pin")
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        OutlinedButton(onClick = { onExport?.invoke() }, modifier = Modifier.weight(1f)) {
                            Text("Export")
                        }
                        OutlinedButton(onClick = { onCloneConfig?.invoke() }, modifier = Modifier.weight(1f)) {
                            Text("Clone config")
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        OutlinedButton(onClick = { onCloneSession?.invoke() }, modifier = Modifier.weight(1f)) {
                            Text("Clone session")
                        }
                        OutlinedButton(onClick = { onDelete?.invoke() }, modifier = Modifier.weight(1f)) {
                            Text("Delete", color = Color(0xFFFF7E8B))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CharacterCard(
    char: PresetCharacter,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null
) {
    Surface(
        color = LocalRoleplayColors.current.surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, LocalRoleplayColors.current.stroke.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(LocalRoleplayColors.current.accent.copy(alpha = 0.15f), Color(0xFF8B5CF6).copy(alpha = 0.15f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (char.avatarUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(char.avatarUri),
                        contentDescription = "${char.name} avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = char.name.take(1).uppercase(),
                        style = TextStyle(
                            brush = Brush.horizontalGradient(
                                colors = listOf(LocalRoleplayColors.current.accent, Color(0xFF8B5CF6))
                            ),
                            fontSize = 58.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(10.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = char.name,
                        color = LocalRoleplayColors.current.textPrimary,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = char.author,
                        color = LocalRoleplayColors.current.textSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = char.tagline,
                        color = Color(0xFFFF8FA3),
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    char.tags.take(3).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .background(LocalRoleplayColors.current.surface2, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tag,
                                color = LocalRoleplayColors.current.textSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleplayActionSheet(
    session: ChatSession,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
    onCloneConfig: () -> Unit,
    onCloneSession: () -> Unit,
    onFavorite: () -> Unit,
    onPin: () -> Unit,
    onPublish: (() -> Unit)? = null,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.46f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = LocalRoleplayColors.current.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            border = BorderStroke(1.dp, LocalRoleplayColors.current.stroke),
            shadowElevation = 18.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(42.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(LocalRoleplayColors.current.stroke)
                )
                Text(
                    text = session.displayTitle(),
                    color = LocalRoleplayColors.current.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 18.dp, bottom = 10.dp)
                )
                RoleplayActionSheetRow(
                    icon = Icons.Rounded.Upload,
                    label = stringResource(R.string.action_export_session),
                    onClick = onExport
                )
                RoleplayActionSheetRow(
                    icon = Icons.Rounded.ContentCopy,
                    label = stringResource(R.string.action_clone_config),
                    onClick = onCloneConfig
                )
                RoleplayActionSheetRow(
                    icon = Icons.Rounded.ContentCopy,
                    label = stringResource(R.string.action_clone_session),
                    onClick = onCloneSession
                )
                onPublish?.let {
                    RoleplayActionSheetRow(
                        icon = Icons.Rounded.Public,
                        label = "Publish to Community",
                        onClick = it
                    )
                }
                RoleplayActionSheetRow(
                    icon = Icons.Rounded.FavoriteBorder,
                    label = if (session.isFavorite) "Unfavorite" else "Favorite",
                    onClick = onFavorite
                )
                RoleplayActionSheetRow(
                    icon = Icons.Rounded.StarBorder,
                    label = if (session.pinnedAtMillis != null) "Unpin" else "Pin",
                    onClick = onPin
                )
                RoleplayActionSheetRow(
                    icon = Icons.Rounded.DeleteOutline,
                    label = stringResource(R.string.action_delete),
                    onClick = onDelete
                )
            }
        }
    }
}

@Composable
private fun RoleplayActionSheetRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = LocalRoleplayColors.current.actionAccent,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            color = LocalRoleplayColors.current.textPrimary,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatsScreen(
    viewModel: ChatViewModel,
    onSelectSession: (String) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var actionTarget by remember { mutableStateOf<ChatSession?>(null) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.active_chats),
            style = MaterialTheme.typography.titleLarge,
            color = LocalRoleplayColors.current.textPrimary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (viewModel.sessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_active_chats), color = LocalRoleplayColors.current.textSecondary)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(viewModel.sessions) { session ->
                    val selected = session.id == viewModel.activeSessionId
                    Surface(
                        color = if (selected) LocalRoleplayColors.current.accentSoft.copy(alpha = 0.6f) else LocalRoleplayColors.current.surface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (selected) LocalRoleplayColors.current.accent.copy(alpha = 0.6f) else LocalRoleplayColors.current.stroke),
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onSelectSession(session.id) },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    actionTarget = session
                                }
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(LocalRoleplayColors.current.accent, Color(0xFF8B5CF6)))),
                                contentAlignment = Alignment.Center
                            ) {
                                if (session.persona.avatarUri != null) {
                                    Image(
                                        painter = rememberAsyncImagePainter(session.persona.avatarUri),
                                        contentDescription = "${session.persona.displayName} avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = session.persona.displayName.take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = session.persona.displayName.ifBlank { "New Persona" },
                                        color = LocalRoleplayColors.current.textPrimary,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = session.updatedAt,
                                        color = LocalRoleplayColors.current.textSecondary,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = session.persona.tagline.ifBlank { "Teasing companion" },
                                    color = Color(0xFFFF8FA3),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = session.preview,
                                    color = LocalRoleplayColors.current.textSecondary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    actionTarget?.let { session ->
        RoleplayActionSheet(
            session = session,
            onDismiss = { actionTarget = null },
            onExport = {
                viewModel.autoExportSession(
                    sessionId = session.id,
                    onSuccess = { path ->
                        android.widget.Toast.makeText(context, "Saved: $path", android.widget.Toast.LENGTH_LONG).show()
                    },
                    onFailure = { err ->
                        android.widget.Toast.makeText(context, "Export failed: $err", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
                actionTarget = null
            },
            onCloneConfig = {
                viewModel.duplicateSessionSettings(session.id)
                actionTarget = null
            },
            onCloneSession = {
                viewModel.cloneSession(session.id)
                actionTarget = null
            },
            onFavorite = {
                viewModel.toggleSessionFavorite(session.id)
                actionTarget = null
            },
            onPin = {
                viewModel.toggleSessionPinned(session.id)
                actionTarget = null
            },
            onDelete = {
                viewModel.deleteSession(session.id)
                actionTarget = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateScreen(
    viewModel: ChatViewModel,
    onCreated: () -> Unit,
    onImportSession: () -> Unit,
    onImportConfig: () -> Unit
) {
    val context = LocalContext.current
    val restoredDraft = viewModel.createDraftState
    var draftApplied by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(restoredDraft.name) }
    var tagline by remember { mutableStateOf(restoredDraft.tagline) }
    var selectedTags by remember { mutableStateOf(restoredDraft.selectedTags) }
    var selectedGender by remember { mutableStateOf(restoredDraft.selectedGender) }
    var showTagPicker by remember { mutableStateOf(false) }
    var showAutoFill by remember { mutableStateOf(false) }
    var prompt by remember { mutableStateOf(restoredDraft.prompt) }
    var greeting by remember { mutableStateOf(restoredDraft.greeting) }
    var storyLore by remember { mutableStateOf(restoredDraft.storyLore) }
    var avatarUri by remember { mutableStateOf<android.net.Uri?>(restoredDraft.avatarUri) }
    var background by remember { mutableStateOf<ChatBackground>(restoredDraft.background) }
    var createAttempted by remember { mutableStateOf(false) }

    fun persistPickedImage(uri: android.net.Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    fun clearCreateForm() {
        name = ""
        tagline = ""
        selectedTags = emptyList()
        selectedGender = null
        prompt = ""
        greeting = ""
        storyLore = ""
        avatarUri = null
        background = ChatBackground.DarkMode
        createAttempted = false
        viewModel.clearCreateDraft()
    }

    fun selectGender(tag: String) {
        selectedGender = tag
        selectedTags = (selectedTags.filterNot { it in CreateGenderTags } + tag).distinct()
    }

    val avatarPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                persistPickedImage(uri)
                avatarUri = uri
            }
        }
    )
    val backgroundPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                persistPickedImage(uri)
                background = ChatBackground.CustomImage(uri)
            }
        }
    )

    LaunchedEffect(restoredDraft) {
        if (!draftApplied && restoredDraft.hasContent) {
            name = restoredDraft.name
            tagline = restoredDraft.tagline
            selectedTags = restoredDraft.selectedTags
            selectedGender = restoredDraft.selectedGender
            prompt = restoredDraft.prompt
            greeting = restoredDraft.greeting
            storyLore = restoredDraft.storyLore
            avatarUri = restoredDraft.avatarUri
            background = restoredDraft.background
        }
        draftApplied = true
    }

    LaunchedEffect(name, tagline, selectedTags, selectedGender, prompt, greeting, storyLore, avatarUri, background, draftApplied) {
        if (draftApplied) {
            delay(350)
            viewModel.updateCreateDraft(
                CreateDraftState(
                    name = name,
                    tagline = tagline,
                    selectedTags = selectedTags,
                    selectedGender = selectedGender,
                    prompt = prompt,
                    greeting = greeting,
                    storyLore = storyLore,
                    avatarUri = avatarUri,
                    background = background
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalRoleplayColors.current.background)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = ::clearCreateForm,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(LocalRoleplayColors.current.surface)
                    .border(1.dp, LocalRoleplayColors.current.stroke, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.create_reset_form),
                    tint = LocalRoleplayColors.current.textPrimary
                )
            }
            Text(
                text = stringResource(R.string.create_character_title),
                style = MaterialTheme.typography.headlineSmall,
                color = LocalRoleplayColors.current.textPrimary,
                fontWeight = FontWeight.Bold
            )
            Surface(
                color = LocalRoleplayColors.current.surface,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, LocalRoleplayColors.current.stroke),
                onClick = { showAutoFill = true },
                modifier = Modifier.height(44.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = LocalRoleplayColors.current.accent,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = stringResource(R.string.ai_autofill),
                        color = LocalRoleplayColors.current.textPrimary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        CreateLivePreviewCard(
            name = name,
            tagline = tagline,
            greeting = greeting,
            tags = selectedTags,
            avatarUri = avatarUri
        )

        CreateFormCard(title = stringResource(R.string.character_image_section)) {
            Surface(
                color = LocalRoleplayColors.current.surface2,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, LocalRoleplayColors.current.stroke),
                modifier = Modifier
                    .width(156.dp)
                    .height(196.dp)
                    .clickable { avatarPicker.launch(arrayOf("image/*")) }
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (avatarUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(avatarUri),
                            contentDescription = stringResource(R.string.character_image_section),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Upload,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = LocalRoleplayColors.current.textSecondary,
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.upload_avatar),
                                color = LocalRoleplayColors.current.textSecondary,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        CreateTextFieldCard(
            title = stringResource(R.string.character_name_required),
            value = name,
            onValueChange = { name = it.take(32) },
            placeholder = stringResource(R.string.character_name_placeholder),
            singleLine = true,
            footer = if (createAttempted && name.isBlank()) "Name is required." else null
        )

        CreateFormCard(title = stringResource(R.string.gender_label)) {
            CreateGenderSelector(
                selected = selectedGender,
                onSelect = ::selectGender
            )
        }

        CreateTextFieldCard(
            title = stringResource(R.string.short_tagline_label),
            value = tagline,
            onValueChange = { tagline = it.take(80) },
            placeholder = stringResource(R.string.short_tagline_placeholder),
            singleLine = true
        )

        CreateFormCard(title = stringResource(R.string.tags_traits_label)) {
            if (selectedTags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    selectedTags.forEach { tag ->
                        CreateTagChip(
                            tag = tag,
                            onRemove = {
                                selectedTags = selectedTags - tag
                                if (selectedGender == tag) selectedGender = null
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            Surface(
                color = LocalRoleplayColors.current.surface2,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, LocalRoleplayColors.current.stroke),
                onClick = { showTagPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = null,
                        tint = LocalRoleplayColors.current.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.add_tags),
                        color = LocalRoleplayColors.current.textSecondary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        CreateTextFieldCard(
            title = stringResource(R.string.greeting_label),
            value = greeting,
            onValueChange = { greeting = it },
            placeholder = stringResource(R.string.opening_message_placeholder),
            minLines = 4,
            maxLines = 7
        )

        CreateTextFieldCard(
            title = stringResource(R.string.long_description_label),
            value = prompt,
            onValueChange = { prompt = it },
            placeholder = stringResource(R.string.long_description_placeholder),
            minLines = 7,
            maxLines = 12
        )

        CreateTextFieldCard(
            title = stringResource(R.string.lorebook_label),
            value = storyLore,
            onValueChange = { storyLore = it.take(16000) },
            placeholder = stringResource(R.string.lorebook_placeholder),
            minLines = 4,
            maxLines = 8,
            footer = stringResource(R.string.lorebook_counter, storyLore.length)
        )

        BackgroundOptions(
            selected = background,
            onBackgroundChange = { background = it },
            onPickCustomBackground = { backgroundPicker.launch(arrayOf("image/*")) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        GradientCreateButton(
            enabled = name.isNotBlank(),
            onClick = {
                createAttempted = true
                if (name.isNotBlank()) {
                    viewModel.createSessionWithPersona(
                        name = name,
                        tagline = tagline,
                        prompt = prompt,
                        tags = selectedTags,
                        greeting = greeting,
                        storyLore = storyLore,
                        avatarUri = avatarUri,
                        background = background
                    )
                    viewModel.clearCreateDraft()
                    onCreated()
                }
            },
            text = stringResource(R.string.create_and_chat)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = LocalRoleplayColors.current.stroke)
            Text(stringResource(R.string.or_import), color = LocalRoleplayColors.current.textSecondary, style = MaterialTheme.typography.bodySmall)
            HorizontalDivider(modifier = Modifier.weight(1f), color = LocalRoleplayColors.current.stroke)
        }

        Spacer(modifier = Modifier.height(16.dp))

        CreateImportButton(
            text = stringResource(R.string.import_session_btn),
            onClick = onImportSession,
        )

        Spacer(modifier = Modifier.height(8.dp))

        CreateImportButton(
            text = stringResource(R.string.import_config_btn),
            onClick = onImportConfig
        )

        Spacer(modifier = Modifier.height(24.dp))
    }

    TagPickerDialog(
        visible = showTagPicker,
        selectedTags = selectedTags,
        onDismiss = { showTagPicker = false },
        onConfirm = { tags ->
            val normalizedGender = tags.firstOrNull { it in CreateGenderTags }
            selectedGender = normalizedGender
            selectedTags = (tags.filterNot { it in CreateGenderTags } + listOfNotNull(normalizedGender)).distinct()
            showTagPicker = false
        }
    )

    AutoFillPromptDialog(
        visible = showAutoFill,
        isGenerating = viewModel.isAutoFilling,
        onDismiss = { showAutoFill = false },
        onConfirm = { userPrompt ->
            viewModel.autoFillWithAIForCreate(userPrompt) { generatedPrompt, generatedLore ->
                if (generatedPrompt.isNotBlank()) {
                    prompt = generatedPrompt
                }
                if (generatedLore.isNotBlank()) {
                    storyLore = generatedLore.take(16000)
                }
                showAutoFill = false
            }
        }
    )
}

private val CreateGenderTags = listOf("Male", "Female", "Other")

@Composable
private fun CreateLivePreviewCard(
    name: String,
    tagline: String,
    greeting: String,
    tags: List<String>,
    avatarUri: android.net.Uri?
) {
    Surface(
        color = LocalRoleplayColors.current.surface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, LocalRoleplayColors.current.stroke),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = LocalRoleplayColors.current.surface2,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, LocalRoleplayColors.current.stroke),
                modifier = Modifier.size(86.dp)
            ) {
                if (avatarUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(avatarUri),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = name.take(1).ifBlank { "Z" }.uppercase(),
                            color = LocalRoleplayColors.current.accent,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name.ifBlank { stringResource(R.string.character_name_placeholder) },
                    color = LocalRoleplayColors.current.textPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = tagline.ifBlank { greeting.ifBlank { stringResource(R.string.short_tagline_placeholder) } },
                    color = LocalRoleplayColors.current.textSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
                if (tags.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        items(tags.take(6)) { tag ->
                            Surface(
                                color = LocalRoleplayColors.current.accent.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(999.dp)
                            ) {
                                Text(
                                    text = tag,
                                    color = LocalRoleplayColors.current.accent,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateFormCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = LocalRoleplayColors.current.surface,
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, LocalRoleplayColors.current.stroke),
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                color = LocalRoleplayColors.current.textPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}

@Composable
private fun CreateTextFieldCard(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    footer: String? = null
) {
    CreateFormCard(title = title, modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = LocalRoleplayColors.current.textSecondary.copy(alpha = 0.72f)) },
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            textStyle = TextStyle(color = LocalRoleplayColors.current.textPrimary),
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = LocalRoleplayColors.current.textPrimary,
                unfocusedTextColor = LocalRoleplayColors.current.textPrimary,
                focusedContainerColor = LocalRoleplayColors.current.surface2,
                unfocusedContainerColor = LocalRoleplayColors.current.surface2,
                focusedBorderColor = LocalRoleplayColors.current.accent,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = LocalRoleplayColors.current.accent
            ),
            modifier = Modifier.fillMaxWidth()
        )
        if (footer != null) {
            Text(
                text = footer,
                color = LocalRoleplayColors.current.textSecondary,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CreateGenderSelector(
    selected: String?,
    onSelect: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        CreateGenderTags.forEach { gender ->
            val isSelected = selected == gender
            val label = when (gender) {
                "Male" -> stringResource(R.string.gender_male)
                "Female" -> stringResource(R.string.gender_female)
                else -> stringResource(R.string.gender_other)
            }
            Surface(
                color = if (isSelected) LocalRoleplayColors.current.accent.copy(alpha = 0.16f) else LocalRoleplayColors.current.surface2,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) LocalRoleplayColors.current.accent else LocalRoleplayColors.current.stroke
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clickable { onSelect(gender) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        color = if (isSelected) LocalRoleplayColors.current.accent else LocalRoleplayColors.current.textPrimary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateTagChip(
    tag: String,
    onRemove: () -> Unit
) {
    Surface(
        color = LocalRoleplayColors.current.accent.copy(alpha = 0.14f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, LocalRoleplayColors.current.accent.copy(alpha = 0.32f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = tag,
                color = LocalRoleplayColors.current.accent,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1
            )
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Remove tag",
                tint = LocalRoleplayColors.current.accent,
                modifier = Modifier
                    .size(14.dp)
                    .clickable(onClick = onRemove)
            )
        }
    }
}

@Composable
private fun GradientCreateButton(
    enabled: Boolean,
    onClick: () -> Unit,
    text: String
) {
    val alpha = if (enabled) 1f else 0.45f
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .alpha(alpha)
            .clip(RoundedCornerShape(999.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF67E8F9), Color(0xFF8B5CF6), Color(0xFFFF5D8F))
                )
            )
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CreateImportButton(
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, LocalRoleplayColors.current.stroke),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = LocalRoleplayColors.current.surface,
            contentColor = LocalRoleplayColors.current.textPrimary
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
    ) {
        Icon(Icons.Rounded.FileOpen, contentDescription = null, tint = LocalRoleplayColors.current.accentSoft)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = LocalRoleplayColors.current.textPrimary, fontWeight = FontWeight.Bold)
    }
}

private val TAG_CATEGORIES = linkedMapOf(
    "Genre & Setting" to listOf(
        "Fantasy", "Sci-Fi", "Modern", "Slice-of-Life",
        "Adventure", "Isekai", "Post-Apocalyptic", "Cyberpunk",
        "Steampunk", "Medieval", "Historical", "Victorian",
        "High-School", "College-Life", "Office-Setting", "Supernatural",
        "Mystery", "Horror", "Thriller", "Dystopian",
        "Space-Opera", "Military", "Survival", "Mafia/Underworld",
        "Gothic", "Fairy-Tale", "Apocalypse", "Cyber-Noir",
        "Urban-Fantasy", "Contemporary", "Historical-Romance", "Dark-Fantasy"
    ),
    "Personality Archetypes" to listOf(
        "Tsundere", "Yandere", "Kuudere", "Dandere",
        "Dominant", "Submissive", "Teasing", "Stoic",
        "Shy", "Arrogant", "Protective", "Possessive",
        "Caring", "Cold", "Mischievous", "Playful",
        "Sassy", "Gentle", "Rebellious", "Introverted",
        "Extroverted", "Manipulative", "Loyal", "Obsessive",
        "Flirty", "Sarcastic", "Aloof", "Affectionate",
        "Clingy", "Devoted", "Innocent", "Seductive",
        "Bratty", "Nurturing", "Reserved"
    ),
    "Species & Identity" to listOf(
        "Human", "Elf", "Demon", "Angel",
        "Vampire", "Werewolf", "Neko/Kemonomimi", "Furry/Anthromorph",
        "Android/Robot", "Monster", "Alien", "Ghost/Spirit",
        "Witch/Wizard", "Demi-Human", "Orc", "Dragon",
        "Male", "Female", "Non-Binary", "Genderfluid",
        "Futanari", "Succubus", "Incubus", "Hybrid"
    ),
    "Relationship Tropes" to listOf(
        "Enemies-to-Lovers", "Friends-to-Lovers", "Strangers-to-Lovers",
        "Arranged-Marriage", "Ex-Partner", "Roommate", "Step-Family",
        "Childhood-Friend", "Secret-Crush", "Forbidden-Love", "Office-Romance",
        "Fake-Dating", "Slow-Burn", "Love-Triangle", "Fated-Mates",
        "Mutual-Pining", "Accidental-Meeting", "Rescue-Mission", "Stalker",
        "Contracts/Agreements", "Marriage-of-Convenience", "Reunion",
        "One-Night-Stand", "Long-Distance"
    ),
    "Roles & Occupations" to listOf(
        "CEO/Boss", "Student", "Teacher/Professor", "Royalty (Prince/Queen)",
        "Villain", "Hero", "Knight/Guard", "Butler/Maid", "Detective",
        "Doctor/Nurse", "Idol/Celebrity", "Bodyguard", "Mafia-Boss",
        "Gladiator", "Pirate", "Assassin", "Adventurer", "Deity/God",
        "Priest/Nun", "Barista", "Secretary", "Model", "Athlete",
        "Artist/Musician"
    ),
    "Format & POV" to listOf(
        "AnyPOV", "MalePOV", "FemalePOV", "Scenario", "RPG-Game",
        "Interactive-Fiction", "First-Person", "Third-Person",
        "Long-Form/Novella", "Short-Form", "Chat/Casual", "Lorebook/Utility",
        "Multi-Character", "Script-Style", "Detailed-Descriptive"
    ),
    "Explicit & Hentai" to listOf(
        "Hentai", "Explicit", "NSFW", "Erotic",
        "Big-Breasts", "Ahegao", "Creampie", "Mind-Break",
        "Tentacles", "Futanari", "Monster-Girl", "Yuri",
        "Yaoi", "Netorare", "NTR", "Reverse-Harem",
        "Dubcon", "Noncon", "Consensual-Nonconsent", "Aftercare",
        "Vanilla", "Romantic-Sex", "Slow-Intimacy", "Passionate",
        "Breeding", "Public", "Size-Difference", "Lactation",
        "Oviposition", "Corruption", "Hypnosis", "Gangbang",
        "Cheating", "MILF", "Incest", "Rape",
        "Pregnant", "Slave", "Inflation", "Golden-Shower",
        "Chikan", "BDSM", "Femdom", "Orgasm-Denial",
        "Cum-Inflation", "Impregnation", "Double-Penetration", "Anal",
        "Blowjob", "Paizuri", "Footjob", "Handjob",
        "Stockings", "Lingerie", "Uniform", "Maid-Outfit",
        "Schoolgirl", "Glasses", "Glasses-Adjustment", "Glasses-Push",
        "Sweat", "Smell", "Armpit", "Hairy",
        "BBW", "Tomboy", "Trap", "Dark-Skin",
        "Monster", "Demon-Girl", "Elf-Girl", "Catgirl"
    ),
    "Vanilla & Romance" to listOf(
        "Fluff", "Cuddling", "Kissing", "Hand-Holding",
        "Date-Night", "Confession", "Domestic-Life", "Gentle-Romance",
        "Emotional-Intimacy", "Comfort", "Healing", "Sweet",
        "Wholesome", "First-Time", "Morning-After", "Proposal",
        "Family-Oriented", "Cozy", "Light-Hearted", "Pure-Love",
        "Mutual-Masturbation", "Foreplay", "Missionary", "Spooning",
        "Love-Making", "Affection", "Hugging", "Pillow-Talk"
    )
)

@Composable
private fun localizedTagCategory(category: String): String {
    return when (category) {
        "Genre & Setting" -> stringResource(R.string.tag_cat_genre)
        "Personality Archetypes" -> stringResource(R.string.tag_cat_personality)
        "Species & Identity" -> stringResource(R.string.tag_cat_species)
        "Relationship Tropes" -> stringResource(R.string.tag_cat_relationship)
        "Roles & Occupations" -> stringResource(R.string.tag_cat_roles)
        "Format & POV" -> stringResource(R.string.tag_cat_format)
        "Explicit & Hentai" -> stringResource(R.string.tag_cat_explicit)
        "Vanilla & Romance" -> stringResource(R.string.tag_cat_vanilla)
        else -> category
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagPickerDialog(
    visible: Boolean,
    selectedTags: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    if (!visible) return
    var draftTags by remember(selectedTags) { mutableStateOf(selectedTags) }
    var query by remember { mutableStateOf("") }
    val normalizedQuery = query.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LocalRoleplayColors.current.surface,
        title = { Text(stringResource(R.string.add_tags), color = LocalRoleplayColors.current.textPrimary) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.search_tags_hint), color = LocalRoleplayColors.current.textSecondary) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = LocalRoleplayColors.current.textSecondary) },
                    singleLine = true,
                    textStyle = TextStyle(color = LocalRoleplayColors.current.textPrimary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TAG_CATEGORIES.forEach { (category, tags) ->
                        val filteredTags = if (normalizedQuery.isBlank()) {
                            tags
                        } else {
                            tags.filter { it.contains(normalizedQuery, ignoreCase = true) }
                        }
                        if (filteredTags.isNotEmpty()) {
                            item(key = category) {
                                Text(
                                    text = localizedTagCategory(category),
                                    color = LocalRoleplayColors.current.textPrimary,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    filteredTags.forEach { tag ->
                                        val selected = tag in draftTags
                                        Surface(
                                            color = if (selected) LocalRoleplayColors.current.accent else LocalRoleplayColors.current.surface2,
                                            shape = RoundedCornerShape(14.dp),
                                            border = if (selected) null else BorderStroke(1.dp, LocalRoleplayColors.current.stroke),
                                            modifier = Modifier.clickable {
                                                draftTags = if (selected) draftTags - tag else draftTags + tag
                                            }
                                        ) {
                                            Text(
                                                text = tag,
                                                color = if (selected) Color.White else LocalRoleplayColors.current.textSecondary,
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(draftTags.distinct()) }) {
                Text(stringResource(R.string.btn_done), color = LocalRoleplayColors.current.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel), color = LocalRoleplayColors.current.textSecondary)
            }
        }
    )
}

@Composable
private fun RoleplayLanguagePickerSection(
    languageCode: String,
    onLanguageChange: (String) -> Unit
) {
    Surface(
        color = LocalRoleplayColors.current.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, LocalRoleplayColors.current.stroke),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.language_label),
                color = LocalRoleplayColors.current.textPrimary,
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val isEn = languageCode != "vi"
                Surface(
                    color = if (isEn) LocalRoleplayColors.current.accent else LocalRoleplayColors.current.surface2,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onLanguageChange("en") }
                ) {
                    Text(
                        text = stringResource(R.string.lang_english),
                        textAlign = TextAlign.Center,
                        color = if (isEn) Color.White else LocalRoleplayColors.current.textSecondary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Surface(
                    color = if (!isEn) LocalRoleplayColors.current.accent else LocalRoleplayColors.current.surface2,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onLanguageChange("vi") }
                ) {
                    Text(
                        text = stringResource(R.string.lang_vietnamese),
                        textAlign = TextAlign.Center,
                        color = if (!isEn) Color.White else LocalRoleplayColors.current.textSecondary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun UserProfileScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isVi = viewModel.languageCode == "vi"
    var name by remember(viewModel.userProfileName) { mutableStateOf(viewModel.userProfileName) }
    var gender by remember(viewModel.userProfileGender) { mutableStateOf(viewModel.userProfileGender) }
    var bio by remember(viewModel.globalMemoryBlock) { mutableStateOf(viewModel.globalMemoryBlock) }
    var avatarUri by remember(viewModel.userProfileAvatarUri) { mutableStateOf(viewModel.userProfileAvatarUri) }
    val title = if (isVi) "H\u1ed3 s\u01a1 c\u1ee7a t\u00f4i" else "My Profile"
    val nameLabel = if (isVi) "T\u00ean" else "Name"
    val genderLabel = if (isVi) "Gi\u1edbi t\u00ednh" else "Gender"
    val bioLabel = if (isVi) "Ti\u1ec3u s\u1eed" else "Bio"
    val avatarLabel = if (isVi) "T\u1ea3i \u1ea3nh \u0111\u1ea1i di\u1ec7n" else "Upload avatar"
    val saveLabel = if (isVi) "L\u01b0u" else "Save"
    val savedLabel = if (isVi) "\u0110\u00e3 l\u01b0u" else "Saved"
    val genderOptions = if (isVi) {
        listOf("female" to "N\u1eef", "male" to "Nam", "other" to "Kh\u00e1c")
    } else {
        listOf("female" to "Female", "male" to "Male", "other" to "Other")
    }
    val avatarPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                avatarUri = uri
                viewModel.updateUserProfileAvatarFromPicker(uri)
            }
        }
    )
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = LocalRoleplayColors.current.textPrimary,
        unfocusedTextColor = LocalRoleplayColors.current.textPrimary,
        focusedBorderColor = LocalRoleplayColors.current.accent,
        unfocusedBorderColor = LocalRoleplayColors.current.stroke,
        cursorColor = LocalRoleplayColors.current.accent,
        focusedContainerColor = LocalRoleplayColors.current.surface,
        unfocusedContainerColor = LocalRoleplayColors.current.surface
    )

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LocalRoleplayColors.current.background)
            .statusBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = LocalRoleplayColors.current.textPrimary
                )
            }
            Text(
                text = title,
                color = LocalRoleplayColors.current.textPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(116.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            LocalRoleplayColors.current.accentSoft.copy(alpha = 0.55f),
                            LocalRoleplayColors.current.surface2
                        )
                    )
                )
                .border(2.dp, LocalRoleplayColors.current.accent, CircleShape)
                .clickable { avatarPicker.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (avatarUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(avatarUri),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = LocalRoleplayColors.current.accent,
                    modifier = Modifier.size(54.dp)
                )
            }
        }
        TextButton(
            onClick = { avatarPicker.launch("image/*") },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(text = avatarLabel, color = LocalRoleplayColors.current.accent)
        }

        Spacer(modifier = Modifier.height(18.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(120) },
            label = { Text(nameLabel) },
            textStyle = TextStyle(color = LocalRoleplayColors.current.textPrimary),
            colors = fieldColors,
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = genderLabel,
            color = LocalRoleplayColors.current.textPrimary,
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            genderOptions.forEach { (id, label) ->
                val selected = gender == id
                Surface(
                    color = if (selected) LocalRoleplayColors.current.accent else LocalRoleplayColors.current.surface2,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, if (selected) LocalRoleplayColors.current.accent else LocalRoleplayColors.current.stroke),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clickable { gender = id }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            color = if (selected) Color.White else LocalRoleplayColors.current.textSecondary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it.take(64_000) },
            label = { Text(bioLabel) },
            supportingText = {
                Text(
                    text = "${bio.length} / 64,000",
                    color = LocalRoleplayColors.current.textSecondary
                )
            },
            textStyle = TextStyle(color = LocalRoleplayColors.current.textPrimary),
            colors = fieldColors,
            shape = RoundedCornerShape(14.dp),
            minLines = 7,
            maxLines = 12,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(LocalRoleplayColors.current.buttonCyan, LocalRoleplayColors.current.accentSoft)
                    )
                )
                .clickable {
                    viewModel.updateUserProfileName(name)
                    viewModel.updateUserProfileGender(gender)
                    viewModel.updateUserProfileAvatar(avatarUri)
                    viewModel.updateGlobalMemoryBlock(bio)
                    android.widget.Toast.makeText(context, savedLabel, android.widget.Toast.LENGTH_SHORT).show()
                    onBack()
                },
            contentAlignment = Alignment.Center
        ) {
            Text(saveLabel, color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}
@Composable
private fun RoleplaySettingsScreen(viewModel: ChatViewModel) {
    var showProfileScreen by remember { mutableStateOf(false) }

    if (showProfileScreen) {
        UserProfileScreen(
            viewModel = viewModel,
            onBack = { showProfileScreen = false }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.nav_settings),
            style = MaterialTheme.typography.titleLarge,
            color = LocalRoleplayColors.current.textPrimary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Surface(
            color = LocalRoleplayColors.current.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, LocalRoleplayColors.current.stroke),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .clickable { showProfileScreen = true }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = LocalRoleplayColors.current.accent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Hồ sơ của tôi / My Profile",
                        color = LocalRoleplayColors.current.textPrimary,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = viewModel.userProfileName.ifBlank { "Name, gender, and shared bio" },
                        color = LocalRoleplayColors.current.textSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
                Text(">", color = LocalRoleplayColors.current.textSecondary, fontWeight = FontWeight.Bold)
            }
        }

        RoleplayLanguagePickerSection(
            languageCode = viewModel.languageCode,
            onLanguageChange = viewModel::updateLanguage
        )

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            color = LocalRoleplayColors.current.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, LocalRoleplayColors.current.stroke),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Light Mode",
                            color = LocalRoleplayColors.current.textPrimary,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = "Soft pastel theme for RoleplayUI.",
                            color = LocalRoleplayColors.current.textSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                    Switch(
                        checked = viewModel.roleplayLightModeEnabled,
                        onCheckedChange = { viewModel.updateRoleplayLightModeEnabled(it) }
                    )
                }
            }
        }

        Surface(
            color = LocalRoleplayColors.current.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, LocalRoleplayColors.current.stroke),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.roleplay_ui_mode),
                            color = LocalRoleplayColors.current.textPrimary,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = if (viewModel.roleplayUiModeEnabled) stringResource(R.string.roleplay_ui_mode_active) else stringResource(R.string.roleplay_ui_mode_inactive),
                            color = LocalRoleplayColors.current.textSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                    Switch(
                        checked = viewModel.roleplayUiModeEnabled,
                        onCheckedChange = { viewModel.updateRoleplayUiModeEnabled(it) }
                    )
                }
            }
        }

        Surface(
            color = LocalRoleplayColors.current.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, LocalRoleplayColors.current.stroke),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.nsfw_mode),
                            color = LocalRoleplayColors.current.textPrimary,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = if (viewModel.nsfwModeEnabled) stringResource(R.string.nsfw_mode_active) else stringResource(R.string.nsfw_mode_inactive),
                            color = LocalRoleplayColors.current.textSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                    Switch(
                        checked = viewModel.nsfwModeEnabled,
                        onCheckedChange = { viewModel.updateNsfwModeEnabled(it) }
                    )
                }
            }
        }

        Surface(
            color = LocalRoleplayColors.current.surface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, LocalRoleplayColors.current.stroke),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Relationship XP",
                            color = LocalRoleplayColors.current.textPrimary,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = if (viewModel.levelSystemEnabled) stringResource(R.string.relationship_xp_active) else stringResource(R.string.relationship_xp_inactive),
                            color = LocalRoleplayColors.current.textSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                    Switch(
                        checked = viewModel.levelSystemEnabled,
                        onCheckedChange = { viewModel.updateLevelSystemEnabled(it) }
                    )
                }
            }
        }

        Button(
            onClick = { viewModel.openAppSettings() },
            colors = ButtonDefaults.buttonColors(containerColor = LocalRoleplayColors.current.accent),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(stringResource(R.string.open_app_settings), color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        val updateState = viewModel.appUpdateState
        Surface(
            color = LocalRoleplayColors.current.surface,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, LocalRoleplayColors.current.stroke),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        ) {
            Text(
                text = when (updateState) {
                    AppUpdateState.Unknown -> "Checking app version..."
                    AppUpdateState.Checking -> "Checking for updates..."
                    is AppUpdateState.UpToDate -> "App is updated - v${updateState.currentVersion} (${updateState.currentVersionCode})"
                    is AppUpdateState.UpdateAvailable -> {
                        val latestCode = updateState.latestVersionCode?.let { " ($it)" }.orEmpty()
                        "New version available - v${updateState.latestVersion}$latestCode"
                    }
                    is AppUpdateState.Downloading -> "Downloading v${updateState.latestVersion}"
                    is AppUpdateState.InstallerOpened -> "Installer opened. Return here after installing."
                    is AppUpdateState.PermissionNeeded -> "Install permission needed for v${updateState.latestVersion}"
                    is AppUpdateState.Error -> updateState.message.ifBlank { "Update check failed." }
                },
                color = if (updateState is AppUpdateState.Error) {
                    Color(0xFFFF8A80)
                } else {
                    LocalRoleplayColors.current.textSecondary
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(12.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        val updateButtonEnabled = updateState !is AppUpdateState.Checking &&
            updateState !is AppUpdateState.Downloading
        Button(
            onClick = {
                when (updateState) {
                    is AppUpdateState.UpdateAvailable -> viewModel.downloadAndInstallUpdate()
                    is AppUpdateState.PermissionNeeded -> viewModel.openInstallPermissionSettings()
                    else -> viewModel.checkForUpdates()
                }
            },
            enabled = updateButtonEnabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFEDAA00),
                contentColor = Color.Black,
                disabledContainerColor = Color(0xFFEDAA00).copy(alpha = 0.55f),
                disabledContentColor = Color.Black.copy(alpha = 0.75f)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (updateState is AppUpdateState.Checking || updateState is AppUpdateState.Downloading) {
                CircularProgressIndicator(
                    color = Color.Black,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = when (updateState) {
                    AppUpdateState.Unknown -> "Check for Updates"
                    AppUpdateState.Checking -> "Checking..."
                    is AppUpdateState.UpToDate -> "Check Again"
                    is AppUpdateState.InstallerOpened -> "Check Again"
                    is AppUpdateState.PermissionNeeded -> "Open Permission Settings"
                    is AppUpdateState.UpdateAvailable -> "Download & Install"
                    is AppUpdateState.Downloading -> "Downloading ${updateState.progressPercent}%"
                    is AppUpdateState.Error -> updateState.message.ifBlank { "Try Update Again" }
                },
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
