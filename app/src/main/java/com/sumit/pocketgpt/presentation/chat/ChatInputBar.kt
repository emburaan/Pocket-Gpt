package com.sumit.pocketgpt.presentation.chat

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sumit.pocketgpt.ui.theme.PocketGPTTheme

@Composable
internal fun ChatInputBar(
    onSend: (String) -> Unit,
    sendEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var input by remember { mutableStateOf("") }

    Row(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier.weight(1f),
            placeholder = { Text("Message PocketGPT…") },
            maxLines = 4,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Send
            )
        )
        IconButton(
            onClick = {
                onSend(input)
                input = ""
            },
            enabled = sendEnabled && input.isNotBlank(),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send message",
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatInputBarPreview() {
    PocketGPTTheme {
        ChatInputBar(onSend = {}, sendEnabled = true)
    }
}