package com.smartcampusassist.jpui.auth

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.smartcampusassist.jpui.navigation.Screen
import com.smartcampusassist.ui.components.GlassCard
import kotlinx.coroutines.launch

@Composable
fun SignUpScreen(
    navController: NavController,
    repository: FirebaseAuthRepository = remember { FirebaseAuthRepository() }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var formState by remember { mutableStateOf(AccountProfileInput(role = "student")) }
    var isLoading by remember { mutableStateOf(false) }

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
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }

                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        contentPadding = PaddingValues(22.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "Account Request",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }

                        Text(
                            text = "Create account",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 14.dp)
                        )
                        Text(
                            text = "Submit your details for approval.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
                        )

                        AccountProfileForm(
                            state = formState,
                            onStateChange = { formState = it },
                            includePassword = false
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                val validationMessage = validateAccountInput(
                                    input = formState,
                                    requirePassword = false
                                )

                                if (validationMessage != null) {
                                    Toast.makeText(context, validationMessage, Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                scope.launch {
                                    isLoading = true
                                    try {
                                        repository.submitAccountRequest(
                                            formState.copy(email = formState.normalizedEmail())
                                        )

                                        Toast.makeText(
                                            context,
                                            "Request sent. You will get your credentials by email after approval.",
                                            Toast.LENGTH_LONG
                                        ).show()

                                        navController.navigate(Screen.Login.route) {
                                            popUpTo(Screen.SignUp.route) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            e.localizedMessage ?: "Unable to submit request",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(18.dp),
                            enabled = !isLoading
                        ) {
                            Text("Send Request")
                        }

                        Text(
                            text = "Same flow for students and teachers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        )

                        TextButton(
                            onClick = { navController.popBackStack() },
                            modifier = Modifier.align(Alignment.End),
                            enabled = !isLoading
                        ) {
                            Text("Back to sign in")
                        }
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

internal fun validateAccountInput(
    input: AccountProfileInput,
    confirmPassword: String = "",
    requirePassword: Boolean
): String? {
    val normalizedEmail = input.normalizedEmail()

    if (input.fullName.trim().isBlank()) {
        return "Enter full name"
    }

    if (!Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
        return "Enter a valid email address"
    }

    if (requirePassword && input.password.trim().length < 6) {
        return "Password must be at least 6 characters"
    }

    if (requirePassword && input.password != confirmPassword) {
        return "Passwords do not match"
    }

    if (input.department.trim().isBlank()) {
        return "Enter department"
    }

    if (input.role == "teacher") {
        if (input.subject.trim().isBlank()) {
            return "Enter subject"
        }
    } else if (input.semester <= 0) {
        return "Enter a valid semester"
    }

    return null
}
