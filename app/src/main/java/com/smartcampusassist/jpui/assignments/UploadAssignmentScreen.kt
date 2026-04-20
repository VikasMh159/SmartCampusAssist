package com.smartcampusassist.jpui.assignments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.smartcampusassist.BuildConfig
import com.smartcampusassist.campus.CampusCollections
import com.smartcampusassist.campus.TeacherAssignment
import com.smartcampusassist.jpui.components.ScrollableDropdownMenuContent
import com.smartcampusassist.jpui.components.SectionHeader
import com.smartcampusassist.jpui.profile.UserProfile
import com.smartcampusassist.jpui.profile.UserRepository
import com.smartcampusassist.ui.components.AppBackground
import com.smartcampusassist.ui.components.GlassCard
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

private data class AssignmentFileSelection(
    val uri: Uri,
    val fileName: String,
    val mimeType: String
)

private data class TeacherAssignmentOption(
    val className: String = "",
    val semester: Int = 0,
    val subjectTitle: String = "",
    val subjectCode: String = "",
    val branch: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadAssignmentScreen(
    navController: NavController,
    onBack: () -> Unit = { navController.popBackStack() }
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val storage = remember { createAssignmentStorage() }
    val userRepository = remember { UserRepository() }
    val context = navController.context
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedFiles by remember { mutableStateOf(listOf<AssignmentFileSelection>()) }
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var assignmentOptions by remember { mutableStateOf(listOf<TeacherAssignmentOption>()) }
    var selectedClassName by remember { mutableStateOf("") }
    var selectedSemester by remember { mutableStateOf("") }
    var selectedSubjectTitle by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }
    val canPublish = selectedFiles.isNotEmpty() || title.trim().isNotEmpty()
    val semesterOptions = remember(assignmentOptions) {
        assignmentOptions
            .mapNotNull { option -> option.semester.takeIf { it > 0 }?.toString() }
            .distinct()
            .sortedBy { it.toIntOrNull() ?: Int.MAX_VALUE }
            .ifEmpty { (1..8).map(Int::toString) }
    }
    val classOptions = remember(assignmentOptions) {
        assignmentOptions.map { it.className }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val subjectOptions = remember(assignmentOptions, profile) {
        (assignmentOptions.map { it.subjectTitle } + listOf(profile?.subject.orEmpty()))
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        selectedFiles = uris.map { context.resolveFileSelection(it) }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        try {
            profile = userRepository.getUserProfile()
                ?: userRepository.getUserProfile(forceRefresh = true)
            val currentUser = auth.currentUser ?: return@LaunchedEffect
            assignmentOptions = loadTeacherAssignmentOptions(
                firestore = firestore,
                currentUserId = currentUser.uid,
                profile = profile
            )
            selectedSemester = assignmentOptions.firstOrNull { it.semester > 0 }?.semester?.toString().orEmpty()
            selectedSubjectTitle = assignmentOptions.firstOrNull()?.subjectTitle.orEmpty()
        } catch (e: Exception) {
            assignmentOptions = emptyList()
            Log.e("UploadAssignmentScreen", "Failed to load teacher assignment options", e)
        }
    }

    androidx.compose.runtime.LaunchedEffect(selectedClassName, selectedSemester, assignmentOptions) {
        val matchingOptions = assignmentOptions.filter { option ->
            (selectedClassName.isBlank() || option.className == selectedClassName) &&
                (selectedSemester.isBlank() || option.semester.toString() == selectedSemester)
        }
        if (selectedSubjectTitle.isBlank() || matchingOptions.none { it.subjectTitle == selectedSubjectTitle }) {
            selectedSubjectTitle = matchingOptions.firstOrNull()?.subjectTitle.orEmpty()
        }
    }

    androidx.compose.runtime.LaunchedEffect(selectedSubjectTitle, selectedSemester, assignmentOptions) {
        val matchingOption = assignmentOptions.firstOrNull { option ->
            option.subjectTitle == selectedSubjectTitle &&
                (selectedSemester.isBlank() || option.semester.toString() == selectedSemester)
        } ?: return@LaunchedEffect

        if (selectedSemester.isBlank() && matchingOption.semester > 0) {
            selectedSemester = matchingOption.semester.toString()
        }
        if (selectedClassName.isBlank() && matchingOption.className.isNotBlank()) {
            selectedClassName = matchingOption.className
        }
    }

    AppBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionHeader(
                    title = "Upload Assignment",
                    subtitle = "Post coursework",
                    onBack = onBack
                )
            }

            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(30.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Upload",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Add a new assignment.",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            text = "Add text or files.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    UploadStatCard(
                        title = "Mode",
                        value = if (selectedFiles.isEmpty()) "Text or file" else "Multi-file",
                        modifier = Modifier.weight(1f)
                    )
                    UploadStatCard(
                        title = "Selected",
                        value = selectedFiles.size.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(30.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Assignment title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isUploading,
                            shape = RoundedCornerShape(20.dp),
                            colors = inputColors()
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description or instructions") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isUploading,
                            minLines = 5,
                            shape = RoundedCornerShape(20.dp),
                            colors = inputColors()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CompactSelectField(
                                value = selectedSemester,
                                options = semesterOptions,
                                label = "Semester",
                                enabled = !isUploading,
                                onSelected = { selectedSemester = it },
                                modifier = Modifier.weight(1f)
                            )
                            CompactSelectField(
                                value = selectedSubjectTitle,
                                options = subjectOptions,
                                label = "Subject",
                                enabled = !isUploading,
                                onSelected = { selectedSubjectTitle = it },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (classOptions.isNotEmpty()) {
                            CompactSelectField(
                                value = selectedClassName,
                                options = classOptions,
                                label = "Class",
                                enabled = !isUploading,
                                onSelected = { selectedClassName = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    filePicker.launch(
                                        arrayOf(
                                            "application/pdf",
                                            "application/msword",
                                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                            "image/*"
                                        )
                                    )
                                },
                                enabled = !isUploading,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Select Files",
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }

                            if (selectedFiles.isNotEmpty()) {
                                FilledTonalButton(
                                    onClick = { selectedFiles = emptyList() },
                                    enabled = !isUploading,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Text("Clear Selection")
                                }
                            }
                        }

                        if (selectedFiles.isNotEmpty()) {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                contentPadding = PaddingValues(14.dp)
                            ) {
                                Text(
                                    text = "Attached Files",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                selectedFiles.forEach { file ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            androidx.compose.material3.Icon(
                                                imageVector = Icons.Default.Description,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = file.fileName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (file.mimeType.isNotBlank()) {
                                                Text(
                                                    text = file.mimeType,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    isUploading = true
                                    try {
                                        val currentProfile = profile
                                            ?: userRepository.getUserProfile()
                                            ?: userRepository.getUserProfile(forceRefresh = true)
                                        val currentUser = auth.currentUser
                                            ?: throw IllegalStateException("Please sign in again.")
                                        val teacherUid = currentProfile?.uid?.ifBlank { currentUser.uid } ?: currentUser.uid
                                        val teacherName = currentProfile?.fullName
                                            ?.trim()
                                            ?.ifBlank {
                                                currentUser.displayName
                                                    ?.trim()
                                                    ?.ifBlank { fallbackTeacherName(currentUser.email) }
                                            }
                                            ?: currentUser.displayName
                                                ?.trim()
                                                ?.ifBlank { fallbackTeacherName(currentUser.email) }
                                                ?: fallbackTeacherName(currentUser.email)
                                        val matchedOption = assignmentOptions.firstOrNull { option ->
                                            (selectedClassName.isBlank() || option.className == selectedClassName) &&
                                                (selectedSemester.isBlank() || option.semester.toString() == selectedSemester) &&
                                                (selectedSubjectTitle.isBlank() || option.subjectTitle == selectedSubjectTitle)
                                        }
                                        val resolvedSemester = selectedSemester.toIntOrNull()
                                            ?: matchedOption?.semester
                                            ?: 0
                                        val resolvedSubjectTitle = selectedSubjectTitle.ifBlank {
                                            matchedOption?.subjectTitle.orEmpty()
                                        }
                                        val resolvedClassName = selectedClassName.ifBlank {
                                            matchedOption?.className.orEmpty()
                                        }
                                        val resolvedSubjectCode = matchedOption?.subjectCode.orEmpty()
                                        val resolvedBranch = matchedOption?.branch.orEmpty()

                                        if (resolvedSemester <= 0 && selectedFiles.isNotEmpty()) {
                                            throw IllegalStateException("Select a valid semester before publishing.")
                                        }
                                        if (resolvedSubjectTitle.isBlank() && selectedFiles.isNotEmpty()) {
                                            throw IllegalStateException("Select a subject before publishing.")
                                        }

                                        when {
                                            selectedFiles.isNotEmpty() -> {
                                                selectedFiles.forEach { file ->
                                                    val uploaded = uploadAssignmentFile(storage, file, teacherUid)
                                                    firestore.collection("assignments")
                                                        .add(
                                                            mapOf(
                                                                "title" to title.trim().ifBlank {
                                                                    file.fileName.substringBeforeLast('.')
                                                                },
                                                                "description" to description.trim(),
                                                                "fileUrl" to uploaded.downloadUrl,
                                                                "fileName" to uploaded.fileName,
                                                                "mimeType" to uploaded.mimeType,
                                                                "teacherId" to teacherUid,
                                                                "teacherName" to teacherName,
                                                                "className" to resolvedClassName,
                                                                "subjectTitle" to resolvedSubjectTitle,
                                                                "subjectCode" to resolvedSubjectCode,
                                                                "semester" to resolvedSemester,
                                                                "branch" to resolvedBranch,
                                                                "createdAt" to System.currentTimeMillis()
                                                            )
                                                        )
                                                        .await()
                                                }
                                            }

                                            title.isNotBlank() -> {
                                                firestore.collection("assignments")
                                                    .add(
                                                        mapOf(
                                                            "title" to title.trim(),
                                                            "description" to description.trim(),
                                                            "fileUrl" to "",
                                                            "fileName" to "",
                                                            "mimeType" to "",
                                                            "teacherId" to teacherUid,
                                                            "teacherName" to teacherName,
                                                            "className" to resolvedClassName,
                                                            "subjectTitle" to resolvedSubjectTitle,
                                                            "subjectCode" to resolvedSubjectCode,
                                                            "semester" to resolvedSemester,
                                                            "branch" to resolvedBranch,
                                                            "createdAt" to System.currentTimeMillis()
                                                        )
                                                    )
                                                    .await()
                                            }

                                            else -> throw IllegalStateException("Add a title or files first.")
                                        }

                                        title = ""
                                        description = ""
                                        selectedFiles = emptyList()
                                        selectedClassName = ""
                                        selectedSemester = assignmentOptions.firstOrNull { it.semester > 0 }?.semester?.toString().orEmpty()
                                        selectedSubjectTitle = assignmentOptions.firstOrNull()?.subjectTitle.orEmpty()
                                        Toast.makeText(context, "Assignments uploaded", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            e.toUploadErrorMessage(),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } finally {
                                        isUploading = false
                                    }
                                }
                            },
                            enabled = !isUploading && canPublish,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            if (isUploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null
                                )
                                Text(
                                    text = "Publish Assignments",
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Publishing note",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Upload flow stays unchanged.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UploadStatCard(
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
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun inputColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactSelectField(
    value: String,
    options: List<String>,
    label: String,
    enabled: Boolean,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val resolvedOptions = options.filter { it.isNotBlank() }.distinct()
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            if (enabled && resolvedOptions.isNotEmpty()) {
                expanded = !expanded
            }
        },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = inputColors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ScrollableDropdownMenuContent(items = resolvedOptions) { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    }
                )
            }
        }
    }
}

private data class UploadedAssignmentFile(
    val fileName: String,
    val mimeType: String,
    val downloadUrl: String
)

private suspend fun uploadAssignmentFile(
    storage: FirebaseStorage,
    file: AssignmentFileSelection,
    teacherId: String
): UploadedAssignmentFile {
    val extension = file.fileName.substringAfterLast('.', "file")
    val reference: StorageReference = storage.getReference()
        .child("assignments")
        .child(teacherId.ifBlank { "teacher" })
        .child("${UUID.randomUUID()}.$extension")
    val metadataBuilder: StorageMetadata.Builder = StorageMetadata.Builder()
    if (file.mimeType.isNotBlank()) {
        metadataBuilder.setContentType(file.mimeType)
    }
    val metadata: StorageMetadata = metadataBuilder.build()

    reference.putFile(file.uri, metadata).await()
    val downloadUri: Uri = reference.downloadUrl.await()

    return UploadedAssignmentFile(
        fileName = file.fileName,
        mimeType = file.mimeType,
        downloadUrl = downloadUri.toString()
    )
}

private fun createAssignmentStorage(): FirebaseStorage {
    val bucket = BuildConfig.FIREBASE_STORAGE_BUCKET.trim()
    return if (bucket.isNotBlank()) {
        FirebaseStorage.getInstance("gs://$bucket")
    } else {
        FirebaseStorage.getInstance()
    }
}

private fun Context.resolveFileSelection(uri: Uri): AssignmentFileSelection {
    try {
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    } catch (_: SecurityException) {
    }

    var displayName = "file"
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && index >= 0) {
            displayName = cursor.getString(index).orEmpty().ifBlank { displayName }
        }
    }

    return AssignmentFileSelection(
        uri = uri,
        fileName = displayName,
        mimeType = contentResolver.getType(uri).orEmpty()
    )
}

private fun fallbackTeacherName(email: String?): String {
    val localPart = email?.substringBefore('@')?.trim().orEmpty()
    return localPart.ifBlank { "Teacher" }
}

private suspend fun loadTeacherAssignmentOptions(
    firestore: FirebaseFirestore,
    currentUserId: String,
    profile: UserProfile?
): List<TeacherAssignmentOption> {
    val teacherIdentifiers = listOf(
        currentUserId,
        profile?.teacherId.orEmpty(),
        profile?.employeeId.orEmpty()
    ).filter { it.isNotBlank() }.distinct()

    val directMatches = if (teacherIdentifiers.isEmpty()) {
        emptyList()
    } else {
        teacherIdentifiers.chunked(10)
        .flatMap { identifiers ->
            firestore.collection(CampusCollections.TEACHER_ASSIGNMENTS)
                .whereIn("teacherId", identifiers)
                .get()
                .await()
                .toObjects(TeacherAssignment::class.java)
        }
    }

    val fallbackMatches = if (directMatches.isEmpty() && profile?.instituteId?.isNotBlank() == true) {
        firestore.collection(CampusCollections.TEACHER_ASSIGNMENTS)
            .whereEqualTo("instituteId", profile.instituteId)
            .get()
            .await()
            .toObjects(TeacherAssignment::class.java)
            .filter { assignment ->
                val normalizedTeacherName = assignment.teacherName.trim()
                val normalizedProfileName = profile.fullName.trim()
                assignment.teacherId in teacherIdentifiers ||
                    (normalizedTeacherName.isNotBlank() &&
                        normalizedProfileName.isNotBlank() &&
                        normalizedTeacherName.equals(normalizedProfileName, ignoreCase = true))
            }
    } else {
        emptyList()
    }

    return (directMatches + fallbackMatches)
        .map { assignment ->
            TeacherAssignmentOption(
                className = assignment.className,
                semester = assignment.semester,
                subjectTitle = assignment.subjectTitle,
                subjectCode = assignment.subjectCode,
                branch = assignment.branch
            )
        }
        .filter { it.className.isNotBlank() || it.subjectTitle.isNotBlank() || it.semester > 0 }
        .distinct()
        .sortedWith(
            compareBy<TeacherAssignmentOption> { it.semester.takeIf { semester -> semester > 0 } ?: Int.MAX_VALUE }
                .thenBy { it.subjectTitle }
                .thenBy { it.className }
        )
}

private fun Exception.toUploadErrorMessage(): String {
    return when (this) {
        is StorageException -> {
            if (errorCode == StorageException.ERROR_NOT_AUTHORIZED) {
                "File upload blocked by Firebase Storage permissions."
            } else {
                localizedMessage ?: "File upload failed."
            }
        }
        else -> localizedMessage ?: "Upload failed"
    }
}
