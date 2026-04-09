package com.smartcampusassist.jpui.events

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowOutward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.smartcampusassist.BuildConfig
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.smartcampusassist.jpui.components.LoadingState
import com.smartcampusassist.jpui.components.MessageState
import com.smartcampusassist.jpui.components.SectionHeader
import com.smartcampusassist.jpui.navigation.Screen
import com.smartcampusassist.jpui.profile.UserProfile
import com.smartcampusassist.jpui.profile.UserRepository
import com.smartcampusassist.ui.components.AppBackground
import com.smartcampusassist.ui.components.GlassCard
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class EventItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val longDescription: String = "",
    val date: String = "",
    val location: String = "",
    val coverTheme: String = "",
    val imageTags: List<String> = emptyList(),
    val imageUrls: List<String> = emptyList(),
    val createdAt: Long = 0L
)

private data class EventImageSelection(
    val uri: Uri,
    val fileName: String,
    val mimeType: String
)

internal data class EventVisual(
    val palette: Pair<Color, Color>,
    val icon: ImageVector,
    val eyebrow: String,
    val galleryTags: List<String>,
    val generatedDescription: String
)

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun EventsScreen(
    navController: NavController,
    onBack: () -> Unit = { navController.popBackStack() }
) {
    val context = LocalContext.current
    val firestore = remember { FirebaseFirestore.getInstance() }
    val storage = remember {
        FirebaseStorage.getInstance("gs://${BuildConfig.FIREBASE_STORAGE_BUCKET}")
    }
    val userRepository = remember { UserRepository() }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var events by remember { mutableStateOf(listOf<EventItem>()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var editingEvent by remember { mutableStateOf<EventItem?>(null) }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var longDescription by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var coverTheme by remember { mutableStateOf("") }
    var imageTagsInput by remember { mutableStateOf("") }
    var selectedImages by remember { mutableStateOf(listOf<EventImageSelection>()) }
    var isSubmitting by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        selectedImages = uris.map { context.resolveEventImageSelection(it) }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        try {
            profile = userRepository.getUserProfile()
                ?: throw IllegalStateException("User profile not found.")
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Unable to load events."
            isLoading = false
        }
    }

    DisposableEffect(profile?.uid) {
        if (profile == null) {
            onDispose { }
        } else {
            val registration = firestore.collection("events")
                .orderBy("date", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        errorMessage = error.localizedMessage ?: "Unable to load events."
                        isLoading = false
                        return@addSnapshotListener
                    }

                    events = snapshot?.documents.orEmpty()
                        .mapNotNull { document ->
                            document.toObject(EventItem::class.java)?.copy(id = document.id)
                        }
                    errorMessage = null
                    isLoading = false
                }

            onDispose {
                registration.remove()
            }
        }
    }

    AppBackground {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                SectionHeader(
                    title = "Events",
                    subtitle = if (profile?.role == "teacher") {
                        "Manage events"
                    } else {
                        "Browse events"
                    },
                    onBack = onBack
                )
            }

            when {
                isLoading -> item { LoadingState("Loading events") }
                errorMessage != null -> item {
                    MessageState("Events unavailable", errorMessage ?: "")
                }
                else -> {
                    item {
                        EventsHero(
                            count = events.size,
                            isTeacher = profile?.role == "teacher"
                        )
                    }

                    if (profile?.role == "teacher") {
                        item {
                            TeacherEventComposer(
                                title = title,
                                onTitleChange = { title = it },
                                description = description,
                                onDescriptionChange = { description = it },
                                longDescription = longDescription,
                                onLongDescriptionChange = { longDescription = it },
                                date = date,
                                onDateChange = { date = it },
                                location = location,
                                onLocationChange = { location = it },
                                coverTheme = coverTheme,
                                onCoverThemeChange = { coverTheme = it },
                                imageTagsInput = imageTagsInput,
                                onImageTagsInputChange = { imageTagsInput = it },
                                selectedImages = selectedImages,
                                onPickImages = {
                                    imagePicker.launch(arrayOf("image/*"))
                                },
                                onClearImages = { selectedImages = emptyList() },
                                isSubmitting = isSubmitting,
                                onSubmit = {
                                    if (title.isBlank() || date.isBlank()) {
                                        Toast.makeText(
                                            context,
                                            "Title and date are required",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@TeacherEventComposer
                                    }

                                    scope.launch {
                                        isSubmitting = true
                                        try {
                                            val generated = generateEventVisual(
                                                title = title,
                                                description = description,
                                                coverTheme = coverTheme,
                                                imageTags = parseEventTags(imageTagsInput)
                                            )
                                            val teacherId = profile?.uid.orEmpty().ifBlank { "teacher" }
                                            val uploadedImages = selectedImages.map { image ->
                                                uploadEventImage(storage, image, teacherId)
                                            }

                                            firestore.collection("events")
                                                .add(
                                                    mapOf(
                                                        "title" to title.trim(),
                                                        "description" to description.trim().ifBlank {
                                                            generated.generatedDescription
                                                        },
                                                        "longDescription" to longDescription.trim().ifBlank {
                                                            buildGeneratedLongDescription(
                                                                title = title,
                                                                description = description,
                                                                location = location,
                                                                date = date
                                                            )
                                                        },
                                                        "date" to date.trim(),
                                                        "location" to location.trim(),
                                                        "coverTheme" to coverTheme.trim(),
                                                        "imageTags" to parseEventTags(imageTagsInput)
                                                            .ifEmpty { generated.galleryTags },
                                                        "imageUrls" to uploadedImages,
                                                        "createdAt" to System.currentTimeMillis()
                                                    )
                                                )
                                                .await()

                                            title = ""
                                            description = ""
                                            longDescription = ""
                                            date = ""
                                            location = ""
                                            coverTheme = ""
                                            imageTagsInput = ""
                                            selectedImages = emptyList()
                                        } catch (throwable: Exception) {
                                            Toast.makeText(
                                                context,
                                                throwable.localizedMessage ?: "Event save failed",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } finally {
                                            isSubmitting = false
                                        }
                                    }
                                }
                            )
                        }
                    }

                    if (events.isEmpty()) {
                        item {
                            MessageState(
                                "No events",
                                "Events will appear here."
                            )
                        }
                    } else {
                        items(
                            items = events,
                            key = { event -> event.id },
                            contentType = { "event" }
                        ) { event ->
                            EventOverviewCard(
                                event = event,
                                isTeacher = profile?.role == "teacher",
                                onOpen = {
                                    navController.navigate(Screen.EventDetail.createRoute(event.id))
                                },
                                onEdit = { editingEvent = event },
                                onDelete = {
                                    firestore.collection("events")
                                        .document(event.id)
                                        .delete()
                                        .addOnFailureListener { throwable ->
                                            Toast.makeText(
                                                context,
                                                throwable.localizedMessage ?: "Delete failed",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (editingEvent != null && profile?.role == "teacher") {
        EditEventDialog(
            event = editingEvent!!,
            storage = storage,
            teacherId = profile?.uid.orEmpty(),
            onDismiss = { editingEvent = null },
            onSave = { updated ->
                firestore.collection("events")
                    .document(updated.id)
                    .set(
                        mapOf(
                            "title" to updated.title.trim(),
                            "description" to updated.description.trim(),
                            "longDescription" to updated.longDescription.trim(),
                            "date" to updated.date.trim(),
                            "location" to updated.location.trim(),
                            "coverTheme" to updated.coverTheme.trim(),
                            "imageTags" to updated.imageTags,
                            "imageUrls" to updated.imageUrls
                        ),
                        SetOptions.merge()
                    )
                    .addOnSuccessListener {
                        editingEvent = null
                    }
                    .addOnFailureListener { throwable ->
                        Toast.makeText(
                            context,
                            throwable.localizedMessage ?: "Update failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        )
    }
}

@Composable
private fun TeacherEventComposer(
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    longDescription: String,
    onLongDescriptionChange: (String) -> Unit,
    date: String,
    onDateChange: (String) -> Unit,
    location: String,
    onLocationChange: (String) -> Unit,
    coverTheme: String,
    onCoverThemeChange: (String) -> Unit,
    imageTagsInput: String,
    onImageTagsInputChange: (String) -> Unit,
    selectedImages: List<EventImageSelection>,
    onPickImages: () -> Unit,
    onClearImages: () -> Unit,
    isSubmitting: Boolean,
    onSubmit: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        contentPadding = PaddingValues(18.dp)
    ) {
        Text(
            text = "Create Event",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Add event details.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )

        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Title") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            shape = RoundedCornerShape(18.dp),
            colors = eventInputColors()
        )
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Short description") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            shape = RoundedCornerShape(18.dp),
            colors = eventInputColors()
        )
        OutlinedTextField(
            value = longDescription,
            onValueChange = onLongDescriptionChange,
            label = { Text("Long description") },
            minLines = 4,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            shape = RoundedCornerShape(18.dp),
            colors = eventInputColors()
        )
        OutlinedTextField(
            value = date,
            onValueChange = onDateChange,
            label = { Text("Date (YYYY-MM-DD)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            shape = RoundedCornerShape(18.dp),
            colors = eventInputColors()
        )
        OutlinedTextField(
            value = location,
            onValueChange = onLocationChange,
            label = { Text("Location") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            shape = RoundedCornerShape(18.dp),
            colors = eventInputColors()
        )
        OutlinedTextField(
            value = coverTheme,
            onValueChange = onCoverThemeChange,
            label = { Text("Cover theme keyword") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            shape = RoundedCornerShape(18.dp),
            colors = eventInputColors()
        )
        OutlinedTextField(
            value = imageTagsInput,
            onValueChange = onImageTagsInputChange,
            label = { Text("Gallery tags (comma separated)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            shape = RoundedCornerShape(18.dp),
            colors = eventInputColors()
        )
        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                onClick = onPickImages,
                enabled = !isSubmitting,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Select Images")
            }
            if (selectedImages.isNotEmpty()) {
                FilledTonalButton(
                    onClick = onClearImages,
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Clear")
                }
            }
        }
        if (selectedImages.isNotEmpty()) {
            Text(
                text = "Selected: ${selectedImages.joinToString { it.fileName }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .padding(top = 14.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            enabled = !isSubmitting
        ) {
            Text(if (isSubmitting) "Publishing..." else "Publish Event")
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun EventOverviewCard(
    event: EventItem,
    isTeacher: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val generated = remember(event.id, event.title, event.coverTheme, event.imageTags) {
        generateEventVisual(
            title = event.title,
            description = event.description,
            coverTheme = event.coverTheme,
            imageTags = event.imageTags
        )
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(30.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        EventHeroBanner(
            event = event,
            generated = generated,
            compact = true
        )

        Text(
            text = event.description.ifBlank { generated.generatedDescription },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 14.dp),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        FlowRow(
            modifier = Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EventMetaChip(Icons.Outlined.CalendarMonth, formatEventDate(event.date))
            EventMetaChip(Icons.Outlined.LocationOn, event.location.ifBlank { "Campus venue" })
            generated.galleryTags.take(2).forEach { tag ->
                EventMetaChip(Icons.Outlined.AutoAwesome, tag.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
                })
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isTeacher) {
                    FilledTonalButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Edit") }
                    FilledTonalButton(
                        onClick = onDelete,
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("Delete") }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Open details",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Outlined.ArrowOutward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun EventsHero(
    count: Int,
    isTeacher: Boolean
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .padding(20.dp)
        ) {
            Text(
                text = "Events",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (isTeacher) {
                    "Manage campus events."
                } else {
                    "See upcoming events."
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "$count event${if (count == 1) "" else "s"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun eventInputColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline
)

@Composable
private fun EditEventDialog(
    event: EventItem,
    storage: FirebaseStorage,
    teacherId: String,
    onDismiss: () -> Unit,
    onSave: (EventItem) -> Unit
) {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var title by remember(event.id) { mutableStateOf(event.title) }
    var description by remember(event.id) { mutableStateOf(event.description) }
    var longDescription by remember(event.id) { mutableStateOf(event.longDescription) }
    var date by remember(event.id) { mutableStateOf(event.date) }
    var location by remember(event.id) { mutableStateOf(event.location) }
    var coverTheme by remember(event.id) { mutableStateOf(event.coverTheme) }
    var imageTagsInput by remember(event.id) { mutableStateOf(event.imageTags.joinToString(", ")) }
    var imageUrls by remember(event.id) { mutableStateOf(event.imageUrls) }
    var pendingImages by remember(event.id) { mutableStateOf(listOf<EventImageSelection>()) }
    var isSaving by remember(event.id) { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        pendingImages = pendingImages + uris.map { context.resolveEventImageSelection(it) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Edit Event", fontWeight = FontWeight.Bold)
                Text(
                    text = "Update event details.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    shape = RoundedCornerShape(16.dp),
                    colors = eventInputColors()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Short description") },
                    shape = RoundedCornerShape(16.dp),
                    colors = eventInputColors()
                )
                OutlinedTextField(
                    value = longDescription,
                    onValueChange = { longDescription = it },
                    label = { Text("Long description") },
                    minLines = 4,
                    shape = RoundedCornerShape(16.dp),
                    colors = eventInputColors()
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date") },
                    shape = RoundedCornerShape(16.dp),
                    colors = eventInputColors()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    shape = RoundedCornerShape(16.dp),
                    colors = eventInputColors()
                )
                OutlinedTextField(
                    value = coverTheme,
                    onValueChange = { coverTheme = it },
                    label = { Text("Cover theme") },
                    shape = RoundedCornerShape(16.dp),
                    colors = eventInputColors()
                )
                OutlinedTextField(
                    value = imageTagsInput,
                    onValueChange = { imageTagsInput = it },
                    label = { Text("Gallery tags") },
                    shape = RoundedCornerShape(16.dp),
                    colors = eventInputColors()
                )
                if (imageUrls.isNotEmpty()) {
                    Text(
                        text = "Current images",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    imageUrls.forEach { imageUrl ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RemoteEventImage(
                                imageUrl = imageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Text(
                                text = "Uploaded image",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodySmall
                            )
                            TextButton(
                                onClick = { imageUrls = imageUrls.filterNot { it == imageUrl } },
                                enabled = !isSaving
                            ) {
                                Text("Remove")
                            }
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = { imagePicker.launch(arrayOf("image/*")) },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Add Images")
                    }
                    if (pendingImages.isNotEmpty()) {
                        TextButton(
                            onClick = { pendingImages = emptyList() },
                            enabled = !isSaving
                        ) {
                            Text("Clear New")
                        }
                    }
                }
                if (pendingImages.isNotEmpty()) {
                    Text(
                        text = "New images: ${pendingImages.joinToString { it.fileName }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        isSaving = true
                        try {
                            val uploadedImageUrls = pendingImages.map { image ->
                                uploadEventImage(
                                    storage = storage,
                                    image = image,
                                    teacherId = teacherId.ifBlank { "teacher" }
                                )
                            }
                            onSave(
                                event.copy(
                                    title = title,
                                    description = description,
                                    longDescription = longDescription,
                                    date = date,
                                    location = location,
                                    coverTheme = coverTheme,
                                    imageTags = parseEventTags(imageTagsInput),
                                    imageUrls = imageUrls + uploadedImageUrls
                                )
                            )
                        } catch (throwable: Exception) {
                            Toast.makeText(
                                context,
                                throwable.localizedMessage ?: "Image update failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        } finally {
                            isSaving = false
                        }
                    }
                },
                enabled = !isSaving
            ) {
                Text(if (isSaving) "Saving..." else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel")
            }
        }
    )
}

@Composable
internal fun EventHeroBanner(
    event: EventItem,
    generated: EventVisual,
    compact: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 220.dp else 280.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        generated.palette.first,
                        generated.palette.second,
                        generated.palette.first.copy(alpha = 0.72f)
                    )
                )
            )
            .padding(if (compact) 18.dp else 22.dp)
    ) {
        val primaryImageUrl = event.imageUrls.firstOrNull().orEmpty()
        if (primaryImageUrl.isNotBlank()) {
            RemoteEventImage(
                imageUrl = primaryImageUrl,
                contentDescription = event.title,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.42f),
                contentScale = ContentScale.Crop
            )
        }

        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.18f),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                imageVector = generated.icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.padding(12.dp)
            )
        }

        Column(
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            Text(
                text = generated.eyebrow,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.9f)
            )
            Text(
                text = event.title.ifBlank { "Campus Event" },
                style = if (compact) {
                    MaterialTheme.typography.headlineSmall
                } else {
                    MaterialTheme.typography.headlineMedium
                },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                text = "${formatEventDate(event.date)}  -  ${event.location.ifBlank { "Campus venue" }}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
internal fun EventMetaChip(
    icon: ImageVector,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

internal fun formatEventDate(value: String): String {
    val parts = value.split("-")
    if (parts.size != 3) return value.ifBlank { "-" }

    val month = when (parts[1]) {
        "01" -> "Jan"
        "02" -> "Feb"
        "03" -> "Mar"
        "04" -> "Apr"
        "05" -> "May"
        "06" -> "Jun"
        "07" -> "Jul"
        "08" -> "Aug"
        "09" -> "Sep"
        "10" -> "Oct"
        "11" -> "Nov"
        "12" -> "Dec"
        else -> return value
    }

    return "${parts[2]} $month"
}

private fun parseEventTags(value: String): List<String> {
    return value.split(",")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
}

internal fun generateEventVisual(
    title: String,
    description: String,
    coverTheme: String,
    imageTags: List<String>
): EventVisual {
    val seed = buildString {
        append(title.lowercase())
        append(" ")
        append(description.lowercase())
        append(" ")
        append(coverTheme.lowercase())
    }

    val palette = when {
        "ai" in seed || "innovation" in seed -> Color(0xFF0F766E) to Color(0xFF2DD4BF)
        "hackathon" in seed || "coding" in seed || "contest" in seed -> Color(0xFF1D4ED8) to Color(0xFF60A5FA)
        "cloud" in seed -> Color(0xFF4338CA) to Color(0xFF93C5FD)
        "quiz" in seed || "challenge" in seed -> Color(0xFFEA580C) to Color(0xFFFACC15)
        "internship" in seed || "career" in seed || "guidance" in seed -> Color(0xFFBE123C) to Color(0xFFFB7185)
        "project" in seed || "review" in seed -> Color(0xFF7C3AED) to Color(0xFFC084FC)
        else -> Color(0xFF334155) to Color(0xFF06B6D4)
    }

    val icon = when {
        "ai" in seed || "intelligence" in seed -> Icons.Outlined.AutoAwesome
        "hackathon" in seed || "coding" in seed || "contest" in seed -> Icons.Outlined.Code
        "cloud" in seed -> Icons.Outlined.Cloud
        "quiz" in seed -> Icons.Outlined.Lightbulb
        "career" in seed || "guidance" in seed || "internship" in seed -> Icons.Outlined.Groups
        else -> Icons.Outlined.EventAvailable
    }

    val eyebrow = when {
        "workshop" in seed -> "Interactive workshop"
        "hackathon" in seed -> "Build sprint"
        "lecture" in seed -> "Guest session"
        "review" in seed -> "Academic milestone"
        "quiz" in seed -> "Team challenge"
        else -> "Campus spotlight"
    }

    val gallery = imageTags.ifEmpty {
        when {
            "ai" in seed -> listOf("robotics desk", "mentor discussion", "demo screen")
            "hackathon" in seed || "coding" in seed -> listOf("night coding", "team huddle", "scoreboard")
            "cloud" in seed -> listOf("server wall", "speaker stage", "network sketch")
            "career" in seed || "internship" in seed -> listOf("career booth", "resume corner", "mentor table")
            else -> listOf("campus crowd", "stage lights", "activity zone")
        }
    }

    val generatedDescription = when {
        description.isNotBlank() -> description.trim()
        "hackathon" in seed -> "A high-energy build challenge where teams shape practical ideas into working campus solutions."
        "workshop" in seed -> "A guided hands-on session designed to turn concepts into concrete skills and live demos."
        "lecture" in seed -> "A focused expert talk connecting classroom learning with real industry perspectives."
        else -> "A campus event crafted to bring students, mentors, and ideas together in one active learning space."
    }

    return EventVisual(
        palette = palette,
        icon = icon,
        eyebrow = eyebrow,
        galleryTags = gallery,
        generatedDescription = generatedDescription
    )
}

internal fun buildGeneratedLongDescription(
    title: String,
    description: String,
    location: String,
    date: String
): String {
    val opening = description.trim().ifBlank {
        "This event is designed to create a stronger campus experience around learning, collaboration, and participation."
    }

    return buildString {
        append(opening)
        append(" ")
        append("$title will bring students and faculty together through guided activities, live interaction, and a focused event atmosphere.")
        if (location.isNotBlank()) {
            append(" The main experience will unfold at $location, with a setup aimed at making the session feel active and welcoming.")
        }
        if (date.isNotBlank()) {
            append(" It is scheduled for $date, giving participants a clear touchpoint to plan around and join with their teams.")
        }
        append(" Future updates can include richer descriptions, agenda blocks, speaker names, and real event images uploaded by the admin.")
    }
}

private suspend fun uploadEventImage(
    storage: FirebaseStorage,
    image: EventImageSelection,
    teacherId: String
): String {
    val extension = image.fileName.substringAfterLast('.', "jpg")
    val reference: StorageReference = storage.reference
        .child("events")
        .child(teacherId)
        .child("${UUID.randomUUID()}.$extension")

    val metadataBuilder = StorageMetadata.Builder()
    if (image.mimeType.isNotBlank()) {
        metadataBuilder.setContentType(image.mimeType)
    }

    reference.putFile(image.uri, metadataBuilder.build()).await()
    return reference.downloadUrl.await().toString()
}

private fun Context.resolveEventImageSelection(uri: Uri): EventImageSelection {
    try {
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    } catch (_: SecurityException) {
    }

    var displayName = "event-image"
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && index >= 0) {
            displayName = cursor.getString(index).orEmpty().ifBlank { displayName }
        }
    }

    return EventImageSelection(
        uri = uri,
        fileName = displayName,
        mimeType = contentResolver.getType(uri).orEmpty()
    )
}
