package com.smartcampusassist.jpui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.smartcampusassist.campus.CampusRoles
import com.smartcampusassist.jpui.components.ScrollableDropdownMenuContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountProfileForm(
    state: AccountProfileInput,
    onStateChange: (AccountProfileInput) -> Unit,
    instituteOptions: List<String>,
    institutesLoading: Boolean = false,
    departmentOptions: List<String>,
    departmentsLoading: Boolean = false,
    branchOptions: List<String>,
    branchesLoading: Boolean = false,
    includePassword: Boolean,
    footer: @Composable (() -> Unit)? = null
) {
    val isStudent = state.role == CampusRoles.STUDENT
    val usesDepartmentSelection = state.role == CampusRoles.TEACHER
    val usesBranchSelection = isStudent || state.role == CampusRoles.PRINCIPAL || state.role == CampusRoles.HOD || state.role == CampusRoles.CLERK || state.role == CampusRoles.ADMIN
    val showsOptionalSubject = state.role == CampusRoles.TEACHER || state.role == CampusRoles.HOD

    Column {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = "Details",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ProfileTextField(
            value = state.fullName,
            onValueChange = { onStateChange(state.copy(fullName = it)) },
            label = "Full name"
        )

        Spacer(modifier = Modifier.height(12.dp))

        ProfileTextField(
            value = state.email,
            onValueChange = { onStateChange(state.copy(email = it)) },
            label = "Email address",
            keyboardType = KeyboardType.Email
        )

        if (includePassword) {
            Spacer(modifier = Modifier.height(12.dp))
            ProfileTextField(
                value = state.password,
                onValueChange = { onStateChange(state.copy(password = it)) },
                label = "Password",
                keyboardType = KeyboardType.Password
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Role",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        RoleDropdownSelector(
            selectedRole = state.role,
            onRoleSelected = { role ->
                onStateChange(
                    state.copy(
                        role = role,
                        enrollment = if (role == CampusRoles.STUDENT) state.enrollment else "",
                        semester = if (role == CampusRoles.STUDENT) state.semester else 0,
                        subject = if (role == CampusRoles.TEACHER || role == CampusRoles.HOD) state.subject else "",
                        teacherId = "",
                        employeeId = "",
                        department = if (role == CampusRoles.TEACHER) state.department else "",
                        branch = if (role == CampusRoles.STUDENT || role == CampusRoles.PRINCIPAL || role == CampusRoles.HOD || role == CampusRoles.CLERK || role == CampusRoles.ADMIN) state.branch else ""
                    )
                )
            }
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = roleSummary(state.role),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        InstituteSelector(
            value = state.instituteName,
            instituteOptions = instituteOptions,
            institutesLoading = institutesLoading,
            onValueChange = { onStateChange(state.copy(instituteName = it)) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (usesDepartmentSelection) {
            ProfileTextField(
                value = state.department,
                onValueChange = { onStateChange(state.copy(department = it)) },
                branchOptions = departmentOptions,
                branchesLoading = departmentsLoading,
                label = "Department",
                supportingText = "Select department. Office is grouped under Staff."
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        if (usesBranchSelection) {
            ProfileTextField(
                value = state.branch,
                onValueChange = { onStateChange(state.copy(branch = it)) },
                branchOptions = branchOptions,
                branchesLoading = branchesLoading,
                label = "Branch / Program",
                supportingText = if (isStudent) "Student course or stream" else "Select branch or program"
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        ProfileTextField(
            value = state.academicYear,
            onValueChange = { onStateChange(state.copy(academicYear = it)) },
            label = if (isStudent) "Academic year" else "Joined in",
            supportingText = if (isStudent) "Example: 2026-27" else "Example: 2024 or August 2024"
        )

        if (isStudent) {
            Spacer(modifier = Modifier.height(12.dp))
            ProfileTextField(
                value = state.enrollment,
                onValueChange = { onStateChange(state.copy(enrollment = it)) },
                label = "Enrollment number",
                supportingText = "Use official student enrollment ID"
            )

            Spacer(modifier = Modifier.height(12.dp))
            ProfileTextField(
                value = if (state.semester > 0) state.semester.toString() else "",
                onValueChange = {
                    onStateChange(state.copy(semester = it.trim().toIntOrNull() ?: 0))
                },
                label = "Semester",
                keyboardType = KeyboardType.Number,
                supportingText = "Enter current semester"
            )

            Spacer(modifier = Modifier.height(12.dp))
            ProfileTextField(
                value = state.division,
                onValueChange = { onStateChange(state.copy(division = it)) },
                label = "Division",
                supportingText = "Examples: A, B, C"
            )
        }

        if (showsOptionalSubject) {
            Spacer(modifier = Modifier.height(12.dp))
            ProfileTextField(
                value = state.subject,
                onValueChange = { onStateChange(state.copy(subject = it)) },
                label = "Primary Subject (Optional)",
                supportingText = "Optional. You can continue without filling this."
            )
        }

        footer?.invoke()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleDropdownSelector(
    selectedRole: String,
    onRoleSelected: (String) -> Unit
) {
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
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ScrollableDropdownMenuContent(items = roleOptions()) { role ->
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

@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    branchOptions: List<String>? = null,
    branchesLoading: Boolean = false,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    supportingText: String? = null
) {
    if (!branchOptions.isNullOrEmpty()) {
        SelectableTextField(
            value = value,
            options = branchOptions,
            loading = branchesLoading,
            onValueChange = onValueChange,
            label = label,
            supportingText = supportingText
        )
        return
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = supportingText?.let { message ->
            { Text(message) }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        visualTransformation = if (keyboardType == KeyboardType.Password) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectableTextField(
    value: String,
    options: List<String>,
    loading: Boolean,
    onValueChange: (String) -> Unit,
    label: String,
    supportingText: String?
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            supportingText = {
                Text(
                    when {
                        loading -> "Loading options..."
                        !supportingText.isNullOrBlank() -> supportingText
                        else -> "Select or type a value"
                    }
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ScrollableDropdownMenuContent(items = options) { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InstituteSelector(
    value: String,
    instituteOptions: List<String>,
    institutesLoading: Boolean,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Institute") },
            supportingText = {
                Text(
                    if (institutesLoading) {
                        "Loading institutes..."
                    } else {
                        "Choose institute from Firebase data or type a custom institute name"
                    }
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            singleLine = true
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ScrollableDropdownMenuContent(items = instituteOptions.ifEmpty { defaultInstituteOptions() }) { institute ->
                DropdownMenuItem(
                    text = { Text(institute) },
                    onClick = {
                        onValueChange(institute)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun roleOptions(): List<String> = listOf(
    CampusRoles.STUDENT,
    CampusRoles.TEACHER,
    CampusRoles.PRINCIPAL,
    CampusRoles.HOD,
    CampusRoles.CLERK,
    CampusRoles.ADMIN
)

private fun defaultInstituteOptions(): List<String> = listOf(
    "SALITER",
    "SETI"
)

private fun roleSummary(role: String): String {
    return when (role) {
        CampusRoles.STUDENT -> "Student request will ask for enrollment, semester, and division."
        CampusRoles.TEACHER -> "Teacher request will collect department, joined-in details, and optional subject."
        CampusRoles.HOD -> "HOD request will collect branch or program details with optional subject."
        CampusRoles.PRINCIPAL -> "Principal request will be reviewed with branch or program and joined-in details."
        CampusRoles.CLERK -> "Clerk request will collect branch or program details for office access."
        CampusRoles.ADMIN -> "Admin request should only be used for full system management accounts."
        else -> "Fill role-specific details for approval."
    }
}

private fun String.toUiLabel(): String {
    if (isBlank()) return ""
    return replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}
