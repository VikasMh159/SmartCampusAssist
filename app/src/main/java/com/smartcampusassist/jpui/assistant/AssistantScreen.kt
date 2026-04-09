package com.smartcampusassist.jpui.assistant

import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.smartcampusassist.R
import com.smartcampusassist.jpui.profile.UserRepository
import com.smartcampusassist.jpui.profile.UserProfile
import com.smartcampusassist.ui.components.GlassCard
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val text: String = "",
    val isUser: Boolean = false,
    val summary: String = "",
    val chips: List<String> = emptyList(),
    val cards: List<MessageCard> = emptyList()
)

private val starterPrompts = listOf(
    "Show my class schedule for today",
    "Summarize upcoming assignments",
    "Suggest ideas for a campus event",
    "Create a quick exam study plan"
)

private const val TYPING_STATUS = "Assistant is typing..."

@Composable
@Suppress("UNUSED_PARAMETER")
fun AssistantScreen(navController: NavController) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val viewModel: AssistantViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userRepository = remember { UserRepository() }

    var userInput by remember { mutableStateOf(TextFieldValue("")) }
    var searchHistoryQuery by remember { mutableStateOf(TextFieldValue("")) }
    var savedChatsQuery by remember { mutableStateOf(TextFieldValue("")) }
    var showSearchHistory by remember { mutableStateOf(false) }
    var showSavedChats by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var pendingDeleteMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var pendingDeleteSavedChatId by remember { mutableStateOf<Long?>(null) }
    var renamingSavedChat by remember { mutableStateOf<AssistantSavedChat?>(null) }
    var currentUserLabel by remember { mutableStateOf("Student") }

    LaunchedEffect(Unit) {
        val profile = userRepository.getUserProfile()
        currentUserLabel = resolveCurrentUserLabel(profile)
    }

    val filteredMessages = remember(uiState.messages, searchHistoryQuery.text, showSearchHistory) {
        val query = if (showSearchHistory) searchHistoryQuery.text.trim() else ""
        if (query.isBlank()) uiState.messages else uiState.messages.filter {
            it.text.contains(query, ignoreCase = true)
        }
    }
    val filteredSavedChats = remember(uiState.savedChats, savedChatsQuery.text) {
        val query = savedChatsQuery.text.trim()
        if (query.isBlank()) {
            uiState.savedChats
        } else {
            uiState.savedChats.filter { chat ->
                chat.title.contains(query, ignoreCase = true) ||
                    chat.preview.contains(query, ignoreCase = true)
            }
        }
    }
    val timelineItems = remember(filteredMessages) { buildChatTimeline(filteredMessages) }

    LaunchedEffect(uiState.messages.size) {
        if (timelineItems.isNotEmpty()) {
            listState.animateScrollToItem(timelineItems.lastIndex)
        }
    }

    val isAtBottom by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val totalItems = listState.layoutInfo.totalItemsCount
            totalItems == 0 || lastVisible >= totalItems - 1
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        floatingActionButton = {
            if (!isAtBottom && uiState.messages.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        if (timelineItems.isNotEmpty()) {
                            scope.launch { listState.animateScrollToItem(timelineItems.lastIndex) }
                        }
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "Scroll to latest messages"
                    }
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Scroll to latest")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
                        )
                    )
                )
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .imePadding()
        ) {
            TopAssistantBar(
                showMenu = showMenu,
                onToggleMenu = { showMenu = !showMenu },
                onDismissMenu = { showMenu = false },
                onNewChat = {
                    showMenu = false
                    viewModel.startNewChat()
                    showSearchHistory = false
                    showSavedChats = false
                    searchHistoryQuery = TextFieldValue("")
                    savedChatsQuery = TextFieldValue("")
                },
                onToggleSearchHistory = {
                    showMenu = false
                    showSearchHistory = !showSearchHistory
                    if (showSavedChats) showSavedChats = false
                    if (!showSearchHistory) searchHistoryQuery = TextFieldValue("")
                },
                onToggleSavedChats = {
                    showMenu = false
                    showSavedChats = !showSavedChats
                    if (showSearchHistory) showSearchHistory = false
                    if (!showSavedChats) savedChatsQuery = TextFieldValue("")
                },
                searchHistoryVisible = showSearchHistory,
                savedChatsVisible = showSavedChats
            )

            Spacer(modifier = Modifier.height(10.dp))

            AssistantHero()

            AnimatedVisibility(
                visible = showSearchHistory,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = searchHistoryQuery,
                        onValueChange = { searchHistoryQuery = it },
                        placeholder = { Text("Search chat history...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = inputColors(),
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search history")
                        },
                        singleLine = true
                    )
                }
            }

            AnimatedVisibility(
                visible = showSavedChats,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    SavedChatsPanel(
                        searchQuery = savedChatsQuery,
                        onSearchQueryChange = { savedChatsQuery = it },
                        savedChats = filteredSavedChats,
                        totalChatCount = uiState.savedChats.size,
                        onOpenChat = { chatId ->
                            viewModel.openSavedChat(chatId)
                            showSavedChats = false
                            savedChatsQuery = TextFieldValue("")
                        },
                        onRenameChat = { chat ->
                            renamingSavedChat = chat
                        },
                        onDeleteChat = { chatId ->
                            pendingDeleteSavedChatId = chatId
                        }
                    )
                }
            }

            uiState.lastSyncedAt?.let { syncedAt ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = buildString {
                        append("Last synced ${formatMessageTime(syncedAt)}")
                        if (uiState.hasPendingSync) append(" | Sync pending")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            if (uiState.messages.size <= 1) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Suggestions",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(starterPrompts) { prompt ->
                        StarterPromptCard(
                            prompt = prompt,
                            enabled = !uiState.isSending,
                            onClick = {
                                userInput = TextFieldValue("")
                                viewModel.sendMessage(prompt)
                            }
                        )
                    }
                }

            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                if (uiState.isLoadingHistory) {
                    item("history_loading") {
                        AssistantStatusBubble("Your cloud chat history is loading...")
                    }
                }

                items(timelineItems, key = {
                    when (it) {
                        is ChatTimelineItem.DayHeader -> "day_${it.label}"
                        is ChatTimelineItem.MessageItem -> it.message.id
                    }
                }) { item ->
                    when (item) {
                        is ChatTimelineItem.DayHeader -> DayHeader(item.label)
                        is ChatTimelineItem.MessageItem -> ChatBubble(
                            message = item.message,
                            currentUserLabel = currentUserLabel,
                            onDelete = { pendingDeleteMessage = item.message },
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(item.message.text))
                                Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                uiState.statusMessage?.takeIf { uiState.isSending }?.let { status ->
                    item("assistant_status") { AssistantStatusBubble(status) }
                }

                if (!uiState.isLoadingHistory && filteredMessages.isEmpty() && uiState.messages.isNotEmpty()) {
                    item("search_empty") {
                        AssistantStatusBubble("No chat messages matched your search.")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            MessageComposer(
                value = userInput,
                enabled = !uiState.isSending,
                onValueChange = { userInput = it },
                onSend = {
                    val question = userInput.text.trim()
                    userInput = TextFieldValue("")
                    viewModel.sendMessage(question)
                }
            )
        }
    }

    pendingDeleteMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { pendingDeleteMessage = null },
            title = { Text("Delete message") },
            text = { Text("This message will be permanently removed from your synced chat history.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMessage(message.id)
                    pendingDeleteMessage = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteMessage = null }) { Text("Cancel") }
            }
        )
    }

    pendingDeleteSavedChatId?.let { chatId ->
        AlertDialog(
            onDismissRequest = { pendingDeleteSavedChatId = null },
            title = { Text("Delete saved chat") },
            text = { Text("This saved chat will be removed from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSavedChat(chatId)
                    pendingDeleteSavedChatId = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteSavedChatId = null }) { Text("Cancel") }
            }
        )
    }

    renamingSavedChat?.let { chat ->
        var title by remember(chat.id) { mutableStateOf(chat.title) }
        AlertDialog(
            onDismissRequest = { renamingSavedChat = null },
            title = { Text("Rename chat") },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Chat name") },
                    shape = RoundedCornerShape(16.dp),
                    colors = inputColors(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameSavedChat(chat.id, title)
                    renamingSavedChat = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renamingSavedChat = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TopAssistantBar(
    showMenu: Boolean,
    onToggleMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onNewChat: () -> Unit,
    onToggleSearchHistory: () -> Unit,
    onToggleSavedChats: () -> Unit,
    searchHistoryVisible: Boolean,
    savedChatsVisible: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Assistant", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }

        Box {
            IconButton(onClick = onToggleMenu) {
                Icon(Icons.Default.MoreVert, contentDescription = "Chat options")
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = onDismissMenu
            ) {
                DropdownMenuItem(
                    text = { Text("New chat") },
                    onClick = onNewChat,
                    leadingIcon = { Icon(Icons.Default.SupportAgent, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text(if (searchHistoryVisible) "Hide search history" else "Search history") },
                    onClick = onToggleSearchHistory,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text(if (savedChatsVisible) "Hide saved chats" else "Saved chats") },
                    onClick = onToggleSavedChats,
                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null) }
                )
            }
        }
    }
}

@Composable
private fun AssistantHero() {
    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), contentPadding = PaddingValues(0.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f)
                        )
                    )
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primary) {
                Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                    AssistantLogoBadge(size = 28.dp)
                }
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text("Campus Assistant", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Ask about schedules, assignments, exams, reminders, or events.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun MessageComposer(
    value: TextFieldValue,
    enabled: Boolean,
    onValueChange: (TextFieldValue) -> Unit,
    onSend: () -> Unit
) {
    val canSend = value.text.isNotBlank() && enabled
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), tonalElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("Ask a campus question...") },
                modifier = Modifier.weight(1f),
                enabled = enabled,
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    disabledBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface
                ),
                maxLines = 4
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { if (canSend) onSend() },
                enabled = canSend,
                modifier = Modifier.size(46.dp).clip(CircleShape)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape,
                    color = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.ArrowUpward,
                            contentDescription = "Send",
                            tint = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StarterPromptCard(prompt: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .width(165.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            .semantics { contentDescription = "Starter prompt: $prompt" }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                Box(modifier = Modifier.padding(7.dp), contentAlignment = Alignment.Center) {
                    AssistantLogoBadge(size = 20.dp)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                prompt,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SavedChatsPanel(
    searchQuery: TextFieldValue,
    onSearchQueryChange: (TextFieldValue) -> Unit,
    savedChats: List<AssistantSavedChat>,
    totalChatCount: Int,
    onOpenChat: (Long) -> Unit,
    onRenameChat: (AssistantSavedChat) -> Unit,
    onDeleteChat: (Long) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Saved chats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "$totalChatCount chats",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search saved chats") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            shape = RoundedCornerShape(18.dp),
            colors = inputColors(),
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search saved chats")
            },
            singleLine = true
        )

        if (savedChats.isEmpty()) {
            Text(
                text = "No saved chats yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )
        } else {
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                savedChats.take(8).forEach { chat ->
                    SavedChatCard(
                        chat = chat,
                        onOpen = { onOpenChat(chat.id) },
                        onRename = { onRenameChat(chat) },
                        onDelete = { onDeleteChat(chat.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedChatCard(
    chat: AssistantSavedChat,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(8.dp).size(16.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chat.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = chat.preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SavedChatMetaChip(text = "${chat.messages.count { it.isUser }} asks")
                    SavedChatMetaChip(text = formatMessageTime(chat.updatedAt))
                }
            }

            Column {
                IconButton(onClick = onRename, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename saved chat", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete saved chat", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SavedChatMetaChip(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun AssistantStatusBubble(status: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f), shape = RoundedCornerShape(22.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                    Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                        AssistantLogoBadge(size = 22.dp)
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                if (status == TYPING_STATUS) {
                    TypingDotsIndicator()
                } else {
                    Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun DayHeader(label: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun ChatBubble(
    message: ChatMessage,
    currentUserLabel: String,
    onDelete: () -> Unit,
    onCopy: () -> Unit
) {
    val isUserMessage = message.isUser
    val timeLabel = remember(message.id) { DateFormat.format("hh:mm a", Date(message.id)).toString() }
    var showMessageMenu by remember(message.id) { mutableStateOf(false) }
    val bubbleColor = if (isUserMessage) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    val bubbleTextColor = if (isUserMessage) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val bubbleBorder = if (isUserMessage) {
        null
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
    }
    val nameLabel = if (isUserMessage) currentUserLabel else "Assistant"
    val messageShape = if (isUserMessage) {
        RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomStart = 18.dp,
            bottomEnd = 6.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 6.dp,
            topEnd = 18.dp,
            bottomStart = 18.dp,
            bottomEnd = 18.dp
        )
    }

    DropdownMenu(
        expanded = showMessageMenu,
        onDismissRequest = { showMessageMenu = false }
    ) {
        DropdownMenuItem(
            text = { Text("Copy") },
            onClick = {
                showMessageMenu = false
                onCopy()
            },
            leadingIcon = {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
            }
        )
        DropdownMenuItem(
            text = { Text("Delete") },
            onClick = {
                showMessageMenu = false
                onDelete()
            },
            leadingIcon = {
                Icon(Icons.Default.DeleteOutline, contentDescription = null)
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = if (isUserMessage) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUserMessage) {
            ChatAvatar(isUserMessage = false, currentUserLabel = currentUserLabel)
            Spacer(modifier = Modifier.width(8.dp))
        }

        BoxWithConstraints {
            val bubbleMaxWidth = maxWidth * 0.78f
            Column(
                modifier = Modifier.widthIn(max = bubbleMaxWidth),
                horizontalAlignment = if (isUserMessage) Alignment.End else Alignment.Start
            ) {
                Text(
                    text = nameLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
                Surface(
                    color = bubbleColor,
                    contentColor = bubbleTextColor,
                    shape = messageShape,
                    border = bubbleBorder,
                    shadowElevation = if (isUserMessage) 0.dp else 1.dp,
                    modifier = Modifier.combinedClickable(
                        onClick = {},
                        onLongClick = { showMessageMenu = true }
                    )
                ) {
                    Text(
                        text = message.text,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        textAlign = TextAlign.Start
                    )
                }
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        if (isUserMessage) {
            Spacer(modifier = Modifier.width(8.dp))
            ChatAvatar(isUserMessage = true, currentUserLabel = currentUserLabel)
        }
    }
}

@Composable
private fun ChatAvatar(
    isUserMessage: Boolean,
    currentUserLabel: String
) {
    if (isUserMessage) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    UserAvatarBadge(size = 26.dp)
                }
            }
            Text(
                text = buildUserInitials(currentUserLabel),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    } else {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                AssistantLogoBadge(size = 26.dp)
            }
        }
    }
}

@Composable
private fun TypingDotsIndicator() {
    val transition = rememberInfiniteTransition(label = "typing_dots")
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 500, delayMillis = index * 150),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "typing_dot_$index"
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
            ) {
                Box(modifier = Modifier.size(8.dp))
            }
        }
    }
}

@Composable
private fun inputColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surface
)

@Composable
private fun AssistantLogoBadge(size: androidx.compose.ui.unit.Dp) {
    Image(
        painter = painterResource(id = R.drawable.app_logo),
        contentDescription = null,
        modifier = Modifier.size(size)
    )
}

@Composable
private fun UserAvatarBadge(size: androidx.compose.ui.unit.Dp) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.58f)
            )
        }
    }
}

private fun buildUserInitials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (parts.isEmpty()) return "You"
    return parts.take(2).joinToString("") { it.take(1).uppercase() }
}

private fun resolveCurrentUserLabel(profile: UserProfile?): String {
    if (profile == null) return "Student"

    val normalizedRole = profile.role.trim().lowercase()
    val fallbackLabel = if (normalizedRole == "teacher") "Teacher" else "Student"

    return profile.fullName
        .trim()
        .takeIf { it.isNotBlank() }
        ?: fallbackLabel
}

private fun formatMessageTime(timestamp: Long): String {
    return DateFormat.format("dd MMM, hh:mm a", Date(timestamp)).toString()
}

private fun buildChatTimeline(messages: List<ChatMessage>): List<ChatTimelineItem> {
    val timeline = mutableListOf<ChatTimelineItem>()
    var previousDayKey: String? = null
    messages.sortedBy { it.id }.forEach { message ->
        val dayKey = DateFormat.format("yyyy-MM-dd", Date(message.id)).toString()
        if (dayKey != previousDayKey) {
            timeline += ChatTimelineItem.DayHeader(formatDayLabel(message.id))
            previousDayKey = dayKey
        }
        timeline += ChatTimelineItem.MessageItem(message)
    }
    return timeline
}

private fun formatDayLabel(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diffDays = TimeUnit.MILLISECONDS.toDays(startOfDay(now) - startOfDay(timestamp))
    return when (diffDays) {
        0L -> "Today"
        1L -> "Yesterday"
        else -> DateFormat.format("dd MMM yyyy", Date(timestamp)).toString()
    }
}

private fun startOfDay(timestamp: Long): Long {
    val date = Date(timestamp)
    val year = DateFormat.format("yyyy", date).toString().toInt()
    val month = DateFormat.format("MM", date).toString().toInt() - 1
    val day = DateFormat.format("dd", date).toString().toInt()
    val calendar = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.YEAR, year)
        set(java.util.Calendar.MONTH, month)
        set(java.util.Calendar.DAY_OF_MONTH, day)
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    return calendar.timeInMillis
}

private sealed interface ChatTimelineItem {
    data class DayHeader(val label: String) : ChatTimelineItem
    data class MessageItem(val message: ChatMessage) : ChatTimelineItem
}
