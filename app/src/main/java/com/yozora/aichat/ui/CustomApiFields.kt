package com.yozora.aichat.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yozora.aichat.data.remote.CustomApiConfig
import com.yozora.aichat.ui.theme.AppAccentSoft
import com.yozora.aichat.ui.theme.AppStroke
import com.yozora.aichat.ui.theme.AppSurface
import com.yozora.aichat.ui.theme.AppTextPrimary
import com.yozora.aichat.ui.theme.AppTextSecondary
import kotlinx.coroutines.launch

@Composable
internal fun CustomApiFields(
    config: CustomApiConfig,
    model: String,
    keyIdentity: String?,
    onConfigChange: (CustomApiConfig) -> Unit,
    onModelChange: (String) -> Unit,
    fetchModels: suspend (CustomApiConfig) -> Result<List<String>>
) {
    Column {
        Text("Connect to an OpenAI-compatible API.", color = AppTextSecondary,
            style = MaterialTheme.typography.bodySmall)
        ConnectionField("Base URL", config.baseUrl, "https://api.example.com/v1",
            onValueChange = { onConfigChange(config.copy(baseUrl = it)) })
        TextButton(onClick = { onConfigChange(config.copy(baseUrl = CustomApiConfig.GOOGLE_BASE_URL)) }) {
            Text("Use Google Gemini URL", color = AppAccentSoft)
        }
        ConnectionField("Proxy URL (optional)", config.proxyUrl, "socks5://127.0.0.1:1080",
            onValueChange = { onConfigChange(config.copy(proxyUrl = it)) })
        config.validationError()?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp))
        }
        ConnectionField("Model ID", model, "Enter the model ID from your provider",
            onValueChange = onModelChange)
        Text("Pick the matching key below. You can enter any model ID or fetch the provider's list.",
            color = AppTextSecondary, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 6.dp))
        key(config, keyIdentity) {
            CustomModelLookup(config, onModelChange, fetchModels)
        }
    }
}

@Composable
private fun CustomModelLookup(
    config: CustomApiConfig,
    onModelChange: (String) -> Unit,
    fetchModels: suspend (CustomApiConfig) -> Result<List<String>>
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var models by remember { mutableStateOf(emptyList<String>()) }
    var error by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(enabled = !loading && config.validationError() == null, onClick = {
            scope.launch {
                loading = true
                error = null
                try {
                    fetchModels(config).fold(
                        onSuccess = { models = it; expanded = true },
                        onFailure = { error = it.message?.take(180) ?: "Could not fetch models. Enter a model ID manually." }
                    )
                } finally {
                    loading = false
                }
            }
        }) {
            Text(if (loading) "Fetching models..." else "Fetch models", color = AppAccentSoft)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            models.forEach { model ->
                DropdownMenuItem(text = { Text(model, color = AppTextPrimary) }, onClick = {
                    onModelChange(model)
                    expanded = false
                })
            }
        }
    }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
}

@Composable
private fun ConnectionField(label: String, value: String, placeholder: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = AppTextPrimary, unfocusedTextColor = AppTextPrimary,
            focusedBorderColor = AppAccentSoft, unfocusedBorderColor = AppStroke,
            focusedContainerColor = AppSurface, unfocusedContainerColor = AppSurface
        )
    )
}
