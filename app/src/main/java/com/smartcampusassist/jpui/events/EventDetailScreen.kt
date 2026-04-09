package com.smartcampusassist.jpui.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.smartcampusassist.jpui.components.LoadingState
import com.smartcampusassist.jpui.components.MessageState
import com.smartcampusassist.jpui.components.SectionHeader
import com.smartcampusassist.ui.components.AppBackground
import com.smartcampusassist.ui.components.GlassCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun EventDetailScreen(
    navController: NavController,
    eventId: String
) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    var event by remember { mutableStateOf<EventItem?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(eventId) {
        isLoading = true
        errorMessage = null
        try {
            val snapshot = withContext(Dispatchers.IO) {
                firestore.collection("events")
                    .document(eventId)
                    .get()
                    .await()
            }
            event = snapshot.toObject(EventItem::class.java)?.copy(id = snapshot.id)
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Unable to load event details."
        } finally {
            isLoading = false
        }
    }

    AppBackground {
        when {
            isLoading -> LoadingState("Loading event details")
            errorMessage != null -> MessageState("Event unavailable", errorMessage ?: "")
            event == null -> MessageState("Event not found", "This event is no longer available.")
            else -> {
                val currentEvent = event!!
                val generated = remember(
                    currentEvent.id,
                    currentEvent.title,
                    currentEvent.description,
                    currentEvent.coverTheme,
                    currentEvent.imageTags
                ) {
                    generateEventVisual(
                        title = currentEvent.title,
                        description = currentEvent.description,
                        coverTheme = currentEvent.coverTheme,
                        imageTags = currentEvent.imageTags
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        SectionHeader(
                            title = "Event Details",
                            subtitle = "Poster, highlights, story, and generated gallery",
                            onBack = { navController.popBackStack() }
                        )
                    }

                    item {
                        EventHeroBanner(
                            event = currentEvent,
                            generated = generated,
                            compact = false
                        )
                    }

                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(30.dp),
                            contentPadding = PaddingValues(18.dp)
                        ) {
                            Text(
                                text = "Campus Spotlight",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = currentEvent.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                                ,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            FlowRow(
                                modifier = Modifier.padding(top = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                EventMetaChip(Icons.Outlined.CalendarMonth, formatEventDate(currentEvent.date))
                                EventMetaChip(Icons.Outlined.LocationOn, currentEvent.location.ifBlank { "Campus venue" })
                                EventMetaChip(Icons.Outlined.AutoAwesome, generated.eyebrow)
                            }

                            Text(
                                text = currentEvent.longDescription.ifBlank {
                                    buildGeneratedLongDescription(
                                        title = currentEvent.title,
                                        description = currentEvent.description,
                                        location = currentEvent.location,
                                        date = currentEvent.date
                                    )
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }

                    item {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Text(
                                text = "Generated Event Gallery",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Visual story blocks and uploaded images for the event.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }

                    itemsIndexed(generated.galleryTags) { index, tag ->
                        EventGalleryCard(
                            event = currentEvent,
                            label = tag,
                            imageUrl = currentEvent.imageUrls.getOrNull(index).orEmpty()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventGalleryCard(
    event: EventItem,
    label: String,
    imageUrl: String
) {
    val generated = remember(event.title, event.coverTheme, label) {
        generateEventVisual(
            title = "${event.title} $label",
            description = event.description,
            coverTheme = event.coverTheme,
            imageTags = listOf(label)
        )
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            generated.palette.first,
                            generated.palette.second
                        )
                    )
                )
                .padding(18.dp)
        ) {
            if (imageUrl.isNotBlank()) {
                RemoteEventImage(
                    imageUrl = imageUrl,
                    contentDescription = label,
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(0.48f),
                    contentScale = ContentScale.Crop
                )
            }

            Text(
                text = label.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
                },
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "The admin can add a real image, caption, and story block here in the future.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.padding(top = 40.dp)
            )
        }
    }
}
