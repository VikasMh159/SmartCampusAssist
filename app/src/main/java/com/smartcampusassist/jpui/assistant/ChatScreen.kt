package com.smartcampusassist.jpui.assistant

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.smartcampusassist.R
import java.util.Date

@Composable
@Suppress("UNUSED_PARAMETER")
fun ChatScreen(
    navController: NavController,
    viewModel: ChatViewModel = viewModel()
) {
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(viewModel.messages.size, viewModel.isTyping) {
        val extraItem = if (viewModel.isTyping) 1 else 0
        val targetIndex = (viewModel.messages.size + extraItem - 1).coerceAtLeast(0)
        if (viewModel.messages.isNotEmpty() || viewModel.isTyping) {
            listState.scrollToItem(targetIndex)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        bottomBar = {
            ChatInputBar(
                value = inputText,
                enabled = !viewModel.isTyping,
                onValueChange = { inputText = it },
                onSend = {
                    val content = inputText.trim()
                    if (content.isNotBlank()) {
                        viewModel.sendMessage(content)
                        inputText = ""
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
                        )
                    )
                )
                .padding(paddingValues)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                MinimalHeader(onNewChat = {
                    inputText = ""
                    viewModel.resetChat()
                })

                Spacer(modifier = Modifier.height(12.dp))

                if (viewModel.isLoading && viewModel.messages.isEmpty()) {
                    EmptyChatState(isLoading = true)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        if (viewModel.messages.size <= 1) {
                            item("quick_actions") {
                                QuickActionStrip(
                                    onPick = { prompt ->
                                        inputText = prompt
                                    }
                                )
                            }
                        }

                        items(
                            items = viewModel.messages,
                            key = { "${it.timestamp}_${it.isUser}_${it.text.hashCode()}" }
                        ) { message ->
                            MessageBubble(message = message)
                        }

                        item("typing_indicator") {
                            AnimatedVisibility(visible = viewModel.isTyping) {
                                TypingBubble()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MinimalHeader(onNewChat: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column {
                Text(
                    text = "Assistant",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Assignments, events, schedule",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        AssistChip(
            onClick = onNewChat,
            label = { Text("New chat") },
            leadingIcon = {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.surface,
                labelColor = MaterialTheme.colorScheme.primary
            ),
            border = AssistChipDefaults.assistChipBorder(
                enabled = true,
                borderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
    }
}

@Composable
private fun QuickActionStrip(onPick: (String) -> Unit) {
    val prompts = listOf("Show assignments", "Show events", "Today's schedule")
    val scrollState = rememberScrollState()

    Column {
        Text(
            text = "Quick actions",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            prompts.forEach { prompt ->
                AssistChip(
                    onClick = { onPick(prompt) },
                    label = { Text(prompt) }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Composable
private fun EmptyChatState(isLoading: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Text(
                text = "Start a conversation with the assistant.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MessageBubble(message: Message) {
    val isUser = message.isUser
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val bubbleBorder = if (isUser) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    val shape = if (isUser) {
        RoundedCornerShape(22.dp, 22.dp, 8.dp, 22.dp)
    } else {
        RoundedCornerShape(22.dp, 22.dp, 22.dp, 8.dp)
    }
    val timeLabel = remember(message.timestamp) {
        DateFormat.format("hh:mm a", Date(message.timestamp)).toString()
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Row(
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            if (!isUser) {
                ChatAvatar(isUser = false)
                Spacer(modifier = Modifier.width(8.dp))
            }

            Column(
                modifier = Modifier.widthIn(max = if (isUser) 300.dp else 360.dp),
                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
            ) {
                Surface(
                    shape = shape,
                    color = bubbleColor,
                    border = bubbleBorder,
                    tonalElevation = if (isUser) 0.dp else 1.dp,
                    shadowElevation = if (isUser) 0.dp else 1.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        if (!message.summary.isNullOrBlank()) {
                            Text(
                                text = message.summary,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isUser) textColor.copy(alpha = 0.88f) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        Text(
                            text = formatMessageText(message.text),
                            color = textColor,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            textAlign = TextAlign.Start
                        )

                        if (message.cards.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            message.cards.forEach { card ->
                                AssistantDataCard(card = card)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        if (message.chips.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                message.chips.forEach { chip ->
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (isUser) {
                                            Color.White.copy(alpha = 0.16f)
                                        } else {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        }
                                    ) {
                                        Text(
                                            text = chip,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isUser) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                            }
                        }
                    }
                }
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            if (isUser) {
                Spacer(modifier = Modifier.width(8.dp))
                ChatAvatar(isUser = true)
            }
        }
    }
}

@Composable
private fun AssistantDataCard(card: MessageCard) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(
                text = card.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (card.subtitle.isNotBlank()) {
                Text(
                    text = card.subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (card.supportingText.isNotBlank()) {
                Text(
                    text = card.supportingText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            if (card.meta.isNotBlank()) {
                Text(
                    text = card.meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun TypingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        ChatAvatar(isUser = false)
        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(22.dp, 22.dp, 22.dp, 8.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
            tonalElevation = 1.dp,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val transition = rememberInfiniteTransition(label = "typing_dots")
                repeat(3) { index ->
                    val alpha by transition.animateFloat(
                        initialValue = 0.25f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 450, easing = LinearEasing, delayMillis = index * 120),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "typing_$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .alpha(alpha)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatAvatar(isUser: Boolean) {
    Surface(
        modifier = Modifier.size(if (isUser) 36.dp else 38.dp),
        shape = CircleShape,
        color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isUser) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val canSend = enabled && value.trim().isNotBlank()

    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        tonalElevation = 3.dp,
        shadowElevation = 3.dp,
        modifier = Modifier.navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                enabled = enabled,
                placeholder = {
                    Text("Message Assistant")
                },
                shape = RoundedCornerShape(24.dp),
                maxLines = 6,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (canSend) {
                            onSend()
                            focusManager.clearFocus()
                        }
                    }
                )
            )

            Spacer(modifier = Modifier.width(10.dp))

            FloatingActionButton(
                onClick = {
                    if (canSend) {
                        onSend()
                        focusManager.clearFocus()
                    }
                },
                containerColor = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send"
                )
            }
        }
    }
}

private fun formatMessageText(text: String): String {
    return text
        .lines()
        .joinToString("\n") { it.trimEnd() }
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}
