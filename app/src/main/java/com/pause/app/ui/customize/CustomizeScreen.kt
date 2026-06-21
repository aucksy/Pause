package com.pause.app.ui.customize

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pause.app.R
import com.pause.app.domain.model.MessagePresets
import com.pause.app.ui.AppViewModel
import com.pause.app.ui.components.CutoutText
import com.pause.app.ui.components.PauseCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CustomizeScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val processing by viewModel.imageProcessing.collectAsStateWithLifecycle()
    val imageError by viewModel.imageError.collectAsStateWithLifecycle()

    // Local message state so the text field never lags behind DataStore writes.
    var messageText by remember { mutableStateOf(settings.overlayMessage) }

    val customBitmap by produceState<ImageBitmap?>(initialValue = null, settings.customImagePath) {
        value = settings.customImagePath?.let { path ->
            withContext(Dispatchers.IO) {
                runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
            }
        }
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.setCustomImage(uri)
    }

    LaunchedEffect(imageError) {
        if (imageError) {
            kotlinx.coroutines.delay(4000)
            viewModel.consumeImageError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                    ),
                ),
            ),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 24.dp)
                    .height(64.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.width(4.dp))
                Text("Reminder style", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
            ) {
                Text(
                    "A live preview of what you'll see when Pause appears.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                ReminderPreview(
                    message = messageText,
                    showImage = settings.showImage,
                    showText = settings.showText,
                    customBitmap = customBitmap,
                )

                Spacer(Modifier.height(20.dp))
                SectionLabel("What appears")
                PauseCard(Modifier.fillMaxWidth()) {
                    Column {
                        ToggleRow(
                            title = "Show image",
                            checked = settings.showImage,
                            onCheckedChange = { viewModel.setShowImage(it) },
                        )
                        ThinDivider()
                        ToggleRow(
                            title = "Show message",
                            checked = settings.showText,
                            onCheckedChange = { viewModel.setShowText(it) },
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                SectionLabel("Image")
                PauseCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ImageThumbnail(customBitmap)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (settings.customImagePath != null) "Your image" else "Default character",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "Any size works — we resize it for you.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = {
                                    pickImage.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
                                enabled = !processing,
                            ) {
                                Text(if (settings.customImagePath != null) "Replace image" else "Choose image")
                            }
                            if (settings.customImagePath != null) {
                                Spacer(Modifier.width(8.dp))
                                TextButton(onClick = { viewModel.clearCustomImage() }, enabled = !processing) {
                                    Text("Remove")
                                }
                            }
                            if (processing) {
                                Spacer(Modifier.width(12.dp))
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                        }
                        if (imageError) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Couldn't use that image — try a different one.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Tip: a transparent PNG floats with no background.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                SectionLabel("Message")
                PauseCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        val trimmedMessage = messageText.trim()
                        val count = trimmedMessage.codePointCount(0, trimmedMessage.length)
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { input ->
                                val limited = limitCodePoints(input, MessagePresets.MAX_MESSAGE_LENGTH)
                                messageText = limited
                                viewModel.setOverlayMessage(limited)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Your message") },
                            placeholder = { Text("e.g. 🐌 Slow down a sec") },
                            supportingText = { Text("$count/${MessagePresets.MAX_MESSAGE_LENGTH}") },
                            singleLine = false,
                            maxLines = 3,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Or pick one of ours",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                        MessagePresets.messages.forEach { preset ->
                            PresetRow(
                                text = preset,
                                selected = preset == messageText.trim(),
                                onClick = {
                                    messageText = preset
                                    viewModel.setOverlayMessage(preset)
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun ReminderPreview(
    message: String,
    showImage: Boolean,
    showText: Boolean,
    customBitmap: ImageBitmap?,
) {
    val renderImage = showImage
    val renderText = showText || !showImage
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(228.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(listOf(Color(0xFF1B1830), Color(0xFF0E0C16))),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (renderImage) {
                if (customBitmap != null) {
                    Image(
                        bitmap = customBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(96.dp),
                    )
                } else {
                    Image(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.ic_pause_character),
                        contentDescription = null,
                        modifier = Modifier.size(96.dp),
                    )
                }
                if (renderText) Spacer(Modifier.height(12.dp))
            }
            if (renderText) {
                if (message.isNotBlank()) {
                    CutoutText(
                        text = message,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                }
                // Mirror the overlay, which always shows this caption when text is on.
                Text(
                    text = "5 minutes on Instagram",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.74f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ImageThumbnail(customBitmap: ImageBitmap?) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (customBitmap != null) {
            Image(
                bitmap = customBitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(48.dp),
            )
        } else {
            Image(
                painter = androidx.compose.ui.res.painterResource(R.drawable.ic_pause_character),
                contentDescription = null,
                modifier = Modifier.size(46.dp),
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PresetRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        if (selected) {
            Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ThinDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

/** Limit a string to [max] code points without splitting a surrogate pair (emoji). */
private fun limitCodePoints(input: String, max: Int): String {
    if (input.codePointCount(0, input.length) <= max) return input
    val end = input.offsetByCodePoints(0, max)
    return input.substring(0, end)
}
