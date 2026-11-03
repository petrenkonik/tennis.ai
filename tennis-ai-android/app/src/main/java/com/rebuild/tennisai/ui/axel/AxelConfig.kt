package com.rebuild.tennisai.ui.axel

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rebuild.tennisai.axel.AxelBrain

/** GLM API dialog (key/baseURL/model). Opened from Axel and from "App Version" on Profile. */
@Composable
fun GlmConfigDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("axel", Context.MODE_PRIVATE) }
    var apiKey by remember { mutableStateOf(prefs.getString(AxelBrain.KEY_API_KEY, "") ?: "") }
    var baseUrl by remember { mutableStateOf(prefs.getString(AxelBrain.KEY_BASE_URL, AxelBrain.DEFAULT_BASE_URL) ?: AxelBrain.DEFAULT_BASE_URL) }
    var model by remember { mutableStateOf(prefs.getString(AxelBrain.KEY_MODEL, AxelBrain.DEFAULT_MODEL) ?: AxelBrain.DEFAULT_MODEL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("GLM API") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API key") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Model") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                prefs.edit()
                    .putString(AxelBrain.KEY_API_KEY, apiKey.trim())
                    .putString(AxelBrain.KEY_BASE_URL, baseUrl.trim())
                    .putString(AxelBrain.KEY_MODEL, model.trim())
                    .apply()
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
