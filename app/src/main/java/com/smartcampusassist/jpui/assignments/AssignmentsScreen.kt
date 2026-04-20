package com.smartcampusassist.jpui.assignments

import android.content.Intent
import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentSnapshot
import com.smartcampusassist.jpui.components.LoadingState
import com.smartcampusassist.jpui.components.MessageState
import com.smartcampusassist.jpui.components.SectionHeader
import com.smartcampusassist.jpui.navigation.Screen
import com.smartcampusassist.jpui.profile.UserProfile
import com.smartcampusassist.jpui.profile.UserRepository
import com.smartcampusassist.ui.components.AppBackground
import com.smartcampusassist.ui.components.GlassCard

data class Assignment(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val fileUrl: String = "",
    val fileName: String = "",
    val mimeType: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val className: String = "",
    val subjectTitle: String = "",
    val subjectCode: String = "",
    val branch: String = "",
    val semester: Int = 0,
    val createdAt: Long = 0L
)

private enum class AssignmentFilter(val label: String) {
    All("All"),
    WithFiles("Files"),
    TextOnly("Notes")
}

@Composable
fun AssignmentsScreen(
    navController: NavController,
    onBack: () -> Unit = { navController.popBackStack() }
) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    val userRepository = remember { UserRepository() }
    val context = navController.context

    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var assignments by remember { mutableStateOf(listOf<Assignment>()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var editingAssignment by remember { mutableStateOf<Assignment?>(null) }
    var selectedFilter by remember { mutableStateOf(AssignmentFilter.All) }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        try {
            profile = userRepository.getUserProfile()
                ?: throw IllegalStateException("User profile not found.")
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Unable to load assignments."
            isLoading = false
        }
    }

    DisposableEffect(profile?.uid, profile?.role) {
        val currentProfile = profile
        if (currentProfile == null) {
            onDispose { }
        } else {
            isLoading = true
            val registration: ListenerRegistration = firestore.collection("assignments")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        assignments = emptyList()
                        errorMessage = error.localizedMessage ?: "Unable to load assignments."
                        isLoading = false
                        return@addSnapshotListener
                    }

                    try {
                        assignments = snapshot?.documents.orEmpty()
                            .mapNotNull { document ->
                                document.toAssignment()
                            }
                        errorMessage = null
                    } catch (exception: Exception) {
                        assignments = emptyList()
                        errorMessage = exception.localizedMessage ?: "Unable to read assignments."
                    } finally {
                        isLoading = false
                    }
                }

            onDispose {
                registration.remove()
            }
        }
    }

    val visibleAssignments = remember(assignments, profile) {
        assignments.filter { assignment ->
            val currentProfile = profile ?: return@filter true
            if (currentProfile.role != "student") return@filter true

            val semesterMatches = assignment.semester <= 0 || assignment.semester == currentProfile.semester
            val branchMatches = assignment.branch.isBlank() ||
                currentProfile.branch.isBlank() ||
                assignment.branch.equals(currentProfile.branch, ignoreCase = true)
            semesterMatches && branchMatches
        }
    }
    val fileCount = remember(visibleAssignments) { visibleAssignments.count { it.fileUrl.isNotBlank() } }
    val textPostCount = remember(visibleAssignments) { visibleAssignments.count { it.fileUrl.isBlank() } }
    val filteredAssignments = remember(visibleAssignments, selectedFilter) {
        visibleAssignments.filter { assignment ->
            when (selectedFilter) {
                AssignmentFilter.All -> true
                AssignmentFilter.WithFiles -> assignment.fileUrl.isNotBlank()
                AssignmentFilter.TextOnly -> assignment.fileUrl.isBlank()
            }
        }
    }

    AppBackground {
        when {
            isLoading -> LoadingState("Loading assignments")
            errorMessage != null -> MessageState("Assignments unavailable", errorMessage ?: "")
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        SectionHeader(
                            title = "Assignments",
                            subtitle = if (profile?.role == "teacher") {
                                "Manage coursework"
                            } else {
                                "View coursework"
                            },
                            onBack = onBack
                        )
                    }

                    item {
                        AssignmentHero(
                            isTeacher = profile?.role == "teacher",
                            assignmentCount = assignments.size,
                            fileCount = fileCount
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AssignmentStatCard(
                                title = "Total",
                                value = assignments.size.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            AssignmentStatCard(
                                title = "Files",
                                value = fileCount.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            AssignmentStatCard(
                                title = "Text Posts",
                                value = textPostCount.toString(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .size(18.dp)
                            )
                            AssignmentFilter.values().forEach { filter ->
                                val isSelected = filter == selectedFilter
                                AssistChip(
                                    onClick = { selectedFilter = filter },
                                    label = { Text(filter.label) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                        labelColor = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                )
                            }
                        }
                    }

                    if (filteredAssignments.isEmpty()) {
                        item {
                            MessageState(
                                title = if (visibleAssignments.isEmpty()) "No assignments" else "No matches",
                                message = if (visibleAssignments.isEmpty()) {
                                    "Assignments will appear here as soon as a teacher publishes them."
                                } else {
                                    "No assignments match the selected filter."
                                }
                            )
                        }
                    } else {
                        items(
                            items = filteredAssignments,
                            key = { assignment -> assignment.id },
                            contentType = { "assignment" }
                        ) { assignment ->
                            AssignmentCard(
                                assignment = assignment,
                                isTeacher = profile?.role == "teacher",
                                onOpenFile = { url ->
                                    if (url.isBlank()) return@AssignmentCard
                                    openAssignmentFile(
                                        context = context,
                                        navController = navController,
                                        url = url,
                                        title = assignment.fileName.ifBlank { assignment.title }
                                    )
                                },
                                onEdit = { editingAssignment = assignment },
                                onDelete = {
                                    firestore.collection("assignments")
                                        .document(assignment.id)
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

    val currentEditingAssignment = editingAssignment
    if (currentEditingAssignment != null && profile?.role == "teacher") {
        EditAssignmentDialog(
            assignment = currentEditingAssignment,
            onDismiss = { editingAssignment = null },
            onSave = { updatedTitle, updatedDescription ->
                firestore.collection("assignments")
                    .document(currentEditingAssignment.id)
                    .update(
                        mapOf(
                            "title" to updatedTitle.trim(),
                            "description" to updatedDescription.trim()
                        )
                    )
                    .addOnSuccessListener {
                        editingAssignment = null
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

private fun DocumentSnapshot.toAssignment(): Assignment? {
    val payload = data ?: return null

    return Assignment(
        id = id,
        title = payload["title"]?.toString().orEmpty(),
        description = payload["description"]?.toString().orEmpty(),
        fileUrl = payload["fileUrl"]?.toString().orEmpty(),
        fileName = payload["fileName"]?.toString().orEmpty(),
        mimeType = payload["mimeType"]?.toString().orEmpty(),
        teacherId = payload["teacherId"]?.toString().orEmpty(),
        teacherName = payload["teacherName"]?.toString().orEmpty(),
        className = payload["className"]?.toString().orEmpty(),
        subjectTitle = payload["subjectTitle"]?.toString().orEmpty(),
        subjectCode = payload["subjectCode"]?.toString().orEmpty(),
        branch = payload["branch"]?.toString().orEmpty(),
        semester = (payload["semester"] as? Number)?.toInt()
            ?: payload["semester"]?.toString()?.toIntOrNull()
            ?: 0,
        createdAt = (payload["createdAt"] as? Number)?.toLong()
            ?: payload["createdAt"]?.toString()?.toLongOrNull()
            ?: 0L
    )
}

private fun openAssignmentFile(
    context: android.content.Context,
    navController: NavController,
    url: String,
    title: String
) {
    val externalIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    val openedExternally = runCatching {
        context.startActivity(externalIntent)
    }.isSuccess

    if (openedExternally) return

    runCatching {
        navController.navigate(
            Screen.AssignmentViewer.createRoute(
                url = url,
                title = title
            )
        )
    }.onFailure {
        Toast.makeText(
            context,
            "Unable to open this assignment file.",
            Toast.LENGTH_SHORT
        ).show()
    }
}

@Composable
private fun AssignmentHero(
    isTeacher: Boolean,
    assignmentCount: Int,
    fileCount: Int
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
                text = if (isTeacher) "Assignments" else "Coursework",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (isTeacher) {
                    "Review and post assignments."
                } else {
                    "See the latest updates."
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "$assignmentCount items | $fileCount files",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun AssignmentStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(14.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun AssignmentCard(
    assignment: Assignment,
    isTeacher: Boolean,
    onOpenFile: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        contentPadding = PaddingValues(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (assignment.fileUrl.isNotBlank()) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            }
                        )
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = if (assignment.fileUrl.isNotBlank()) {
                            Icons.Default.Description
                        } else {
                            Icons.AutoMirrored.Filled.MenuBook
                        },
                        contentDescription = null,
                        tint = if (assignment.fileUrl.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        }
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssignmentPill(
                            text = if (assignment.fileUrl.isNotBlank()) "Attachment" else "Text update"
                        )
                        AssignmentPill(
                            text = relativeDateLabel(assignment.createdAt),
                            containerBrush = Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                )
                            )
                        )
                    }

                    Text(
                        text = assignment.title.ifBlank { "Untitled assignment" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 10.dp)
                    )

                    if (assignment.description.isNotBlank()) {
                        Text(
                            text = assignment.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (assignment.fileName.isNotBlank()) {
                        Text(
                            text = assignment.fileName,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 10.dp)
                        )
                    }

                    val assignmentMeta = listOfNotNull(
                        assignment.subjectTitle.ifBlank {
                            assignment.subjectCode.ifBlank { null }
                        },
                        assignment.className.ifBlank { null },
                        assignment.semester.takeIf { it > 0 }?.let { "Sem $it" }
                    )
                    if (assignmentMeta.isNotEmpty()) {
                        Text(
                            text = assignmentMeta.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Text(
                        text = "Published by ${assignment.teacherName.ifBlank { assignment.teacherId.ifBlank { "Unknown teacher" } }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (assignment.fileUrl.isNotBlank()) {
                Button(
                    onClick = { onOpenFile(assignment.fileUrl) },
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Open File",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            if (isTeacher) {
                FilledTonalButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Draw,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Edit",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                FilledTonalButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Delete",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AssignmentPill(
    text: String,
    containerBrush: Brush = Brush.horizontalGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        )
    )
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color.Transparent
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(containerBrush)
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .wrapContentWidth()
        )
    }
}

private fun relativeDateLabel(createdAt: Long): String {
    if (createdAt <= 0L) return "Recently"
    return DateUtils.getRelativeTimeSpanString(
        createdAt,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    ).toString()
}

@Composable
private fun EditAssignmentDialog(
    assignment: Assignment,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember(assignment.id) { mutableStateOf(assignment.title) }
    var description by remember(assignment.id) { mutableStateOf(assignment.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Assignment") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title, description) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
