package com.yozora.aichat.ui.companion

import android.graphics.Color as AndroidColor
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.webkit.WebViewAssetLoader
import kotlinx.coroutines.delay
import java.io.File
import org.json.JSONObject

private const val COMPANION_PAGE = "https://appassets.androidplatform.net/assets/companion/index.html"
private const val INTERNAL_VRM_PREFIX = "https://appassets.androidplatform.net/internal/"

@Composable
fun CompanionStage(
    modifier: Modifier = Modifier,
    vrmUri: Uri?,
    mouthAa: Float,
    emotion: String = "neutral",
    emotionWeight: Float = 1f,
    onError: (String) -> Unit = {},
) {
    if (vrmUri == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "No VRM model selected",
                modifier = Modifier.fillMaxSize(0.55f),
                tint = Color(0xFF9A91B7),
            )
        }
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentMouth by rememberUpdatedState(mouthAa.coerceIn(0f, 1f))
    val currentEmotion by rememberUpdatedState(emotion)
    val currentEmotionWeight by rememberUpdatedState(emotionWeight.coerceIn(0f, 1f))
    val safeVrmUrl = remember(context, vrmUri) { internalVrmUrl(context.filesDir, vrmUri) }
    var pageReady by remember { mutableStateOf(false) }
    var loadedVrmUrl by remember { mutableStateOf<String?>(null) }
    var isResumed by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    val currentOnError by rememberUpdatedState(onError)

    val webView = remember(context) {
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler(
                "/assets/",
                WebViewAssetLoader.AssetsPathHandler(context),
            )
            .addPathHandler(
                "/internal/",
                WebViewAssetLoader.InternalStoragePathHandler(context, File(context.filesDir, "vrm")),
            )
            .build()

        WebView(context).apply {
            setBackgroundColor(AndroidColor.TRANSPARENT)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = false
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
            addJavascriptInterface(object {
                @JavascriptInterface
                fun onError(message: String) {
                    post { currentOnError(message) }
                }
            }, "AndroidBridge")
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: android.webkit.WebResourceRequest,
                ): android.webkit.WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)

                override fun onPageFinished(view: WebView, url: String) {
                    if (url.startsWith(COMPANION_PAGE)) pageReady = true
                }
            }
            loadUrl(COMPANION_PAGE)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { webView },
        update = { view ->
            view.setBackgroundColor(AndroidColor.TRANSPARENT)
        },
    )

    LaunchedEffect(webView, pageReady, safeVrmUrl) {
        if (pageReady) {
            if (loadedVrmUrl != safeVrmUrl) {
                safeVrmUrl?.let { url ->
                    webView.evaluateJavascript(
                        "window.companion && window.companion.loadVrm(${javascriptString(url)}).catch(function(){});",
                        null,
                    )
                }
                loadedVrmUrl = safeVrmUrl
            }
            webView.evaluateJavascript(
                "window.companion && window.companion.setMouth(${currentMouth},0,0,0,0);" +
                    "window.companion && window.companion.setEmotion(${javascriptString(currentEmotion)},${currentEmotionWeight});",
                null,
            )
        }
    }

    LaunchedEffect(webView) {
        while (true) {
            delay(33L)
            if (pageReady && isResumed) {
                webView.evaluateJavascript(
                    "window.companion && window.companion.setMouth(${currentMouth},0,0,0,0);" +
                        "window.companion && window.companion.setEmotion(${javascriptString(currentEmotion)},${currentEmotionWeight});",
                    null,
                )
            }
        }
    }

    DisposableEffect(webView, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    isResumed = true
                    webView.evaluateJavascript("window.companion && window.companion.resume();", null)
                }
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> {
                    isResumed = false
                    webView.evaluateJavascript("window.companion && window.companion.pause();", null)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            webView.evaluateJavascript("window.companion && window.companion.resume();", null)
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            webView.evaluateJavascript("window.companion && window.companion.dispose();", null)
            webView.stopLoading()
            webView.removeAllViews()
            webView.destroy()
        }
    }
}

private fun internalVrmUrl(filesDir: File, uri: Uri): String? {
    val path = when (uri.scheme) {
        "file" -> uri.path
        null -> uri.path
        else -> null
    } ?: return null
    val file = File(path).canonicalFile
    val root = File(filesDir, "vrm").canonicalFile
    if (!file.path.startsWith(root.path + File.separator) || !file.isFile) return null
    val relativePath = root.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/')
    return INTERNAL_VRM_PREFIX + relativePath.split('/').joinToString("/") { Uri.encode(it) }
}

private fun javascriptString(value: String): String = JSONObject.quote(value)
