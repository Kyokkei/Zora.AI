package com.yozora.aichat.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yozora.aichat.data.remote.CustomApiConfig
import com.yozora.aichat.ui.theme.AppAccentSoft
import com.yozora.aichat.ui.theme.AppBackground
import com.yozora.aichat.ui.theme.AppStroke
import com.yozora.aichat.ui.theme.AppSurface
import com.yozora.aichat.ui.theme.AppSurface2
import com.yozora.aichat.ui.theme.AppTextPrimary
import com.yozora.aichat.ui.theme.AppTextSecondary
import kotlinx.coroutines.launch

private val ClearKeyColor = Color(0xFFFFA0AA)

@Composable
internal fun CustomApiFields(
    config: CustomApiConfig,
    model: String,
    keyIdentity: String?,
    onConfigChange: (CustomApiConfig) -> Unit,
    onModelChange: (String) -> Unit,
    fetchModels: suspend (CustomApiConfig) -> Result<List<String>>,
    onEditApiKey: () -> Unit,
    onClearApiKey: () -> Unit
) {
    Column {
        Text("Connect to an OpenAI-compatible API.", color = AppTextSecondary,
            style = MaterialTheme.typography.bodySmall)
        ConnectionField("Service name (optional)", config.name, "My gateway",
            onValueChange = { onConfigChange(config.copy(name = it)) })
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
        CustomApiKeyRow(keyIdentity = keyIdentity, onEdit = onEditApiKey, onClear = onClearApiKey)
        Text("Model", color = AppTextPrimary, style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 16.dp))
        Text(
            text = if (model.isBlank()) "Selected model: none" else "Selected model: $model",
            color = if (model.isBlank()) AppTextSecondary else AppAccentSoft,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
        ConnectionField("Model ID", model, "Enter the model ID from your provider",
            onValueChange = onModelChange)
        Text("Enter any model ID, or fetch the provider's list to pick one.",
            color = AppTextSecondary, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 6.dp))
        key(config.baseUrl, config.proxyUrl, keyIdentity) {
            CustomModelLookup(config, model, onModelChange, fetchModels)
        }
    }
}

@Composable
private fun CustomApiKeyRow(keyIdentity: String?, onEdit: () -> Unit, onClear: () -> Unit) {
    Surface(
        color = AppBackground.copy(alpha = 0.32f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AppStroke),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("API key", color = AppTextSecondary, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = keyIdentity ?: "No key",
                color = AppTextPrimary,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(modifier = Modifier.padding(top = 4.dp)) {
                TextButton(onClick = onEdit) {
                    Text(if (keyIdentity == null) "Add key" else "Replace key", color = AppAccentSoft)
                }
                if (keyIdentity != null) {
                    TextButton(onClick = onClear) {
                        Text("Clear", color = ClearKeyColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomModelLookup(
    config: CustomApiConfig,
    selectedModel: String,
    onModelChange: (String) -> Unit,
    fetchModels: suspend (CustomApiConfig) -> Result<List<String>>
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var models by remember { mutableStateOf(emptyList<String>()) }
    var error by remember { mutableStateOf<String?>(null) }
    var pickerOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    TextButton(enabled = !loading && config.validationError() == null, onClick = {
        scope.launch {
            loading = true
            error = null
            try {
                fetchModels(config).fold(
                    onSuccess = { models = it; query = ""; pickerOpen = it.isNotEmpty() },
                    onFailure = {
                        pickerOpen = false
                        error = it.message?.take(180) ?: "Could not fetch models. Enter a model ID manually."
                    }
                )
            } finally {
                loading = false
            }
        }
    }) {
        Text(if (loading) "Fetching models..." else "Fetch models", color = AppAccentSoft)
    }
    error?.let {
        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
    if (pickerOpen) {
        val filtered = remember(models, query) {
            if (query.isBlank()) models else models.filter { it.contains(query.trim(), ignoreCase = true) }
        }
        Surface(
            color = AppSurface2,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, AppStroke),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search models") },
                    placeholder = { Text("Type to filter") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTextPrimary, unfocusedTextColor = AppTextPrimary,
                        focusedBorderColor = AppAccentSoft, unfocusedBorderColor = AppStroke,
                        focusedContainerColor = AppSurface, unfocusedContainerColor = AppSurface
                    )
                )
                Text(
                    text = "${filtered.size} of ${models.size} models",
                    color = AppTextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp)
                )
                if (filtered.isEmpty()) {
                    Text(
                        text = "No models match this search.",
                        color = AppTextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        filtered.forEach { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onModelChange(entry)
                                        pickerOpen = false
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = entry,
                                    color = if (entry == selectedModel) AppAccentSoft else AppTextPrimary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (entry == selectedModel) {
                                    Text("Selected", color = AppAccentSoft,
                                        style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = { pickerOpen = false }) {
                    Text("Close", color = AppTextSecondary)
                }
            }
        }
    }
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
