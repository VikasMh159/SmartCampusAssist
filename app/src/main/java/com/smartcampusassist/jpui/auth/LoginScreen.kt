package com.smartcampusassist.jpui.auth

import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.smartcampusassist.R
import com.smartcampusassist.campus.CampusRoles
import com.smartcampusassist.jpui.components.ScrollableDropdownMenuContent
import com.smartcampusassist.jpui.navigation.AppViewModel
import com.smartcampusassist.jpui.navigation.Screen
import com.smartcampusassist.ui.components.GlassCard
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    navController: NavController,
    appViewModel: AppViewModel,
    viewModel: LoginViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var showPasswordStep by rememberSaveable { mutableStateOf(false) }
    var selectedRole by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            if (selectedRole.isBlank()) {
                Toast.makeText(context, "Select role first", Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken ?: return@rememberLauncherForActivityResult
            viewModel.loginWithGoogle(idToken, selectedRole)
        } catch (_: Exception) {
            Toast.makeText(context, "Google sign-in failed", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            val role = (uiState as LoginUiState.Success).role
            appViewModel.updateRole(role)
            delay(120L)

            navController.navigate(Screen.Main.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
                launchSingleTop = true
            }

            viewModel.resetState()
        }

        if (uiState is LoginUiState.Error) {
            Toast.makeText(context, (uiState as LoginUiState.Error).message, Toast.LENGTH_SHORT).show()
            viewModel.resetState()
        }

        if (uiState is LoginUiState.Info) {
            Toast.makeText(context, (uiState as LoginUiState.Info).message, Toast.LENGTH_SHORT).show()
            viewModel.resetState()
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
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item { Spacer(modifier = Modifier.height(36.dp)) }

                item {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                            Color.Transparent
                                        )
                                    ),
                                    CircleShape
                                )
                        )
                        Image(
                            painter = painterResource(id = R.drawable.app_logo),
                            contentDescription = "Smart Campus Assist logo",
                            modifier = Modifier.size(92.dp)
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                item {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(30.dp),
                        contentPadding = PaddingValues(22.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Campus Access",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }

                        Text(
                            text = "Welcome back",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 14.dp)
                        )

                        Text(
                            text = "Sign in to continue.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
                        )

                        Text(
                            text = "Login as",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        LoginRoleSelector(
                            selectedRole = selectedRole,
                            onRoleSelected = {
                                selectedRole = it
                                if (showPasswordStep) {
                                    password = ""
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                if (showPasswordStep) {
                                    showPasswordStep = false
                                    password = ""
                                }
                            },
                            label = { Text("Email address") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = authInputColors(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (showPasswordStep) {
                            Text(
                                text = email.trim(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = if (passwordVisible) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) {
                                                Icons.Default.Visibility
                                            } else {
                                                Icons.Default.VisibilityOff
                                            },
                                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(18.dp),
                                colors = authInputColors(),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = {
                                    val normalizedEmail = email.trim()
                                    if (password.isBlank()) {
                                        Toast.makeText(context, "Enter password", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (selectedRole.isBlank()) {
                                        Toast.makeText(context, "Select role first", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    viewModel.loginWithEmail(
                                        normalizedEmail,
                                        password,
                                        selectedRole
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                shape = RoundedCornerShape(18.dp),
                                enabled = uiState !is LoginUiState.Loading
                            ) {
                                Text("Login")
                            }

                            TextButton(
                                onClick = {
                                    showPasswordStep = false
                                    password = ""
                                    passwordVisible = false
                                },
                                modifier = Modifier.align(Alignment.End),
                                enabled = uiState !is LoginUiState.Loading
                            ) {
                                Text("Change email")
                            }

                            TextButton(
                                onClick = {
                                    val resetEmail = email.trim()
                                    if (!Patterns.EMAIL_ADDRESS.matcher(resetEmail).matches()) {
                                        Toast.makeText(context, "Enter a valid email address first", Toast.LENGTH_SHORT).show()
                                        return@TextButton
                                    }
                                    viewModel.sendPasswordReset(resetEmail)
                                },
                                modifier = Modifier.align(Alignment.End),
                                enabled = uiState !is LoginUiState.Loading
                            ) {
                                Text("Forgot password?")
                            }
                        } else {
                            Button(
                                onClick = {
                                    val normalizedEmail = email.trim()
                                    if (selectedRole.isBlank()) {
                                        Toast.makeText(context, "Select role first", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (!Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
                                        Toast.makeText(context, "Enter a valid email address", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    email = normalizedEmail
                                    password = ""
                                    passwordVisible = false
                                    showPasswordStep = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                shape = RoundedCornerShape(18.dp),
                                enabled = uiState !is LoginUiState.Loading
                            ) {
                                Text("Continue")
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    googleSignInClient.signOut()
                                    launcher.launch(googleSignInClient.signInIntent)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(58.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF202124)
                                ),
                                enabled = uiState !is LoginUiState.Loading,
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 6.dp,
                                    pressedElevation = 3.dp
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = Color(0xFFDADCE0)
                                )
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.google_logo),
                                    contentDescription = "Google logo",
                                    modifier = Modifier.size(30.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Continue with Google", fontWeight = FontWeight.Bold)
                            }

                            TextButton(
                                onClick = { navController.navigate(Screen.SignUp.route) },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Create account")
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }

                item {
                    Text(
                        text = "Role-wise access for student, teacher, principal, HOD, clerk, and admin",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (uiState is LoginUiState.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard(
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(20.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Signing you in...",
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginRoleSelector(
    selectedRole: String,
    onRoleSelected: (String) -> Unit
) {
    val roles = listOf(
        CampusRoles.STUDENT,
        CampusRoles.TEACHER,
        CampusRoles.PRINCIPAL,
        CampusRoles.HOD,
        CampusRoles.CLERK,
        CampusRoles.ADMIN
    )

    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedRole.toUiLabel(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Select Role") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(18.dp),
            colors = authInputColors()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ScrollableDropdownMenuContent(items = roles) { role ->
                DropdownMenuItem(
                    text = { Text(role.toUiLabel()) },
                    onClick = {
                        expanded = false
                        onRoleSelected(role)
                    }
                )
            }
        }
    }
}

private fun String.toUiLabel(): String {
    if (isBlank()) return ""
    return replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

@Composable
private fun authInputColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface
)
