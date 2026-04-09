package com.smartcampusassist.jpui.auth

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import com.smartcampusassist.BuildConfig
import com.smartcampusassist.jpui.profile.UserRepository
import kotlinx.coroutines.launch

@Composable
fun AdminProvisionScreen(
    navController: NavController,
    repository: FirebaseAuthRepository = remember { FirebaseAuthRepository() },
    userRepository: UserRepository = remember { UserRepository() }
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val adminEmail = BuildConfig.ALLOWED_LOGIN_EMAIL.trim().lowercase()

    var requests by remember { mutableStateOf(listOf<AccountRequest>()) }
    var isCheckingAccess by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }
    var accessDenied by remember { mutableStateOf(false) }

    BackHandler {
        navController.popBackStack()
    }

    LaunchedEffect(Unit) {
        val profile = userRepository.getUserProfile()
        val currentEmail = profile?.email?.trim()?.lowercase().orEmpty()
        val isAuthorized = profile?.role == "admin" ||
            (adminEmail.isNotBlank() && currentEmail == adminEmail)

        accessDenied = !isAuthorized
        isCheckingAccess = false
    }

    DisposableEffect(accessDenied, isCheckingAccess) {
        if (accessDenied || isCheckingAccess) {
            onDispose { }
        } else {
            val registration = repository.observeManageableAccountRequests(
                onChange = { pendingRequests ->
                    requests = pendingRequests
                },
                onError = { exception ->
                    Toast.makeText(
                        context,
                        exception.localizedMessage ?: "Unable to load requests",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )

            onDispose {
                registration.remove()
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            when {
                isCheckingAccess -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                accessDenied -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Access denied",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Only the configured admin account can manage user requests.",
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        TextButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            Text("Back")
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(28.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(22.dp)
                                ) {
                                    Text(
                                        text = "Account Requests",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Approve karte hi backend account create karega aur automatic email bhejega.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                            }
                        }

                        if (requests.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp)
                                    ) {
                                        Text(
                                            text = "No open requests",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Pending aur failed delivery requests yahan realtime show hongi.",
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            items(
                                items = requests,
                                key = { request -> request.email }
                            ) { request ->
                                AccountRequestCard(
                                    request = request,
                                    isProcessing = isProcessing,
                                    onApprove = {
                                        scope.launch {
                                            isProcessing = true
                                            try {
                                                repository.approveAccountRequest(request)
                                                Toast.makeText(
                                                    context,
                                                    "Request approved. Backend email queue started.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(
                                                    context,
                                                    e.localizedMessage ?: "Unable to approve request",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } finally {
                                                isProcessing = false
                                            }
                                        }
                                    },
                                    onReject = {
                                        scope.launch {
                                            isProcessing = true
                                            try {
                                                repository.rejectAccountRequest(request)
                                                Toast.makeText(
                                                    context,
                                                    "Request rejected",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(
                                                    context,
                                                    e.localizedMessage ?: "Unable to reject request",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } finally {
                                                isProcessing = false
                                            }
                                        }
                                    },
                                    onRetry = {
                                        scope.launch {
                                            isProcessing = true
                                            try {
                                                repository.retryAccountRequestDelivery(request)
                                                Toast.makeText(
                                                    context,
                                                    "Delivery retry queued",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(
                                                    context,
                                                    e.localizedMessage ?: "Unable to retry delivery",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } finally {
                                                isProcessing = false
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun AccountRequestCard(
    request: AccountRequest,
    isProcessing: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = request.fullName.ifBlank { request.email },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(text = request.email)
            Text(text = "Role: ${request.role.ifBlank { "student" }}")
            Text(text = "Department: ${request.department.ifBlank { "-" }}")
            if (request.status == "approved") {
                Text(text = "Delivery State: ${request.processingState.ifBlank { "queued" }}")
            }
            if (request.deliveryError.isNotBlank()) {
                Text(text = "Error: ${request.deliveryError}")
            }

            if (request.role == "teacher") {
                Text(text = "Subject: ${request.subject.ifBlank { "-" }}")
                Text(
                    text = "Teacher ID: ${request.teacherId.ifBlank { request.employeeId.ifBlank { "-" } }}"
                )
            } else {
                Text(text = "Enrollment: ${request.enrollment.ifBlank { "-" }}")
                Text(text = "Semester: ${request.semester.takeIf { it > 0 } ?: "-"}")
            }

            Text(text = "Academic Year: ${request.academicYear.ifBlank { "-" }}")

            Spacer(modifier = Modifier.height(4.dp))

            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (request.status == "pending") {
                    Button(
                        onClick = onApprove,
                        enabled = !isProcessing
                    ) {
                        Text("Approve")
                    }

                    OutlinedButton(
                        onClick = onReject,
                        enabled = !isProcessing
                    ) {
                        Text("Reject")
                    }
                } else {
                    Button(
                        onClick = onRetry,
                        enabled = !isProcessing
                    ) {
                        Text("Retry Email")
                    }
                }
            }
        }
    }
}
