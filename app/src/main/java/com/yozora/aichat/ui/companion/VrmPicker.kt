package com.yozora.aichat.ui.companion

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

private const val MAX_VRM_BYTES = 50L * 1024L * 1024L

@Composable
fun VrmPicker(
    onVrmSelected: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    onError: (String) -> Unit = {},
    content: @Composable () -> Unit = { Text("Choose VRM") },
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { sourceUri ->
        if (sourceUri != null) {
            scope.launch {
                runCatching { copyVrmIntoInternalStorage(context, sourceUri) }
                    .onSuccess(onVrmSelected)
                    .onFailure { error -> onError(error.message ?: "Unable to import VRM file") }
            }
        }
    }

    Button(
        onClick = { picker.launch(arrayOf("application/octet-stream", "model/gltf-binary")) },
        modifier = modifier,
    ) {
        content()
    }
}

private suspend fun copyVrmIntoInternalStorage(context: Context, sourceUri: Uri): Uri =
    withContext(Dispatchers.IO) {
        val displayName = context.contentResolver.query(
            sourceUri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        } ?: sourceUri.lastPathSegment.orEmpty()

        if (!displayName.endsWith(".vrm", ignoreCase = true)) {
            throw IOException("Choose a file with the .vrm extension")
        }

        val targetDirectory = File(context.filesDir, "vrm").apply {
            if (!exists() && !mkdirs()) throw IOException("Unable to create the VRM storage directory")
        }
        val target = File(targetDirectory, "${UUID.randomUUID()}.vrm")
        val temp = File(targetDirectory, "${target.name}.tmp")
        try {
            val input = context.contentResolver.openInputStream(sourceUri)
                ?: throw IOException("Unable to open the selected VRM file")
            input.use { source ->
                temp.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var totalBytes = 0L
                    while (true) {
                        val count = source.read(buffer)
                        if (count < 0) break
                        totalBytes += count
                        if (totalBytes > MAX_VRM_BYTES) {
                            throw IOException("VRM files must be 50 MB or smaller")
                        }
                        output.write(buffer, 0, count)
                    }
                }
            }
            if (!temp.renameTo(target)) throw IOException("Unable to save the VRM file")
            Uri.fromFile(target)
        } catch (error: Exception) {
            temp.delete()
            target.delete()
            throw error
        }
    }
