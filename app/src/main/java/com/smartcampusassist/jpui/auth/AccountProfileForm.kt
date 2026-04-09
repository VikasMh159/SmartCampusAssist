package com.smartcampusassist.jpui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun AccountProfileForm(
    state: AccountProfileInput,
    onStateChange: (AccountProfileInput) -> Unit,
    includePassword: Boolean,
    footer: @Composable (() -> Unit)? = null
) {
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RoleButton(
                label = "Student",
                selected = state.role != "teacher",
                onClick = { onStateChange(state.copy(role = "student")) },
                modifier = Modifier.weight(1f)
            )

            RoleButton(
                label = "Teacher",
                selected = state.role == "teacher",
                onClick = { onStateChange(state.copy(role = "teacher")) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        ProfileTextField(
            value = state.department,
            onValueChange = { onStateChange(state.copy(department = it)) },
            label = "Department"
        )

        Spacer(modifier = Modifier.height(12.dp))

        ProfileTextField(
            value = state.academicYear,
            onValueChange = { onStateChange(state.copy(academicYear = it)) },
            label = "Academic year"
        )

        if (state.role == "teacher") {
            Spacer(modifier = Modifier.height(12.dp))

            ProfileTextField(
                value = state.subject,
                onValueChange = { onStateChange(state.copy(subject = it)) },
                label = "Subject"
            )

            Spacer(modifier = Modifier.height(12.dp))

            ProfileTextField(
                value = state.teacherId,
                onValueChange = { onStateChange(state.copy(teacherId = it)) },
                label = "Teacher ID"
            )

            Spacer(modifier = Modifier.height(12.dp))

            ProfileTextField(
                value = state.employeeId,
                onValueChange = { onStateChange(state.copy(employeeId = it)) },
                label = "Employee ID"
            )
        } else {
            Spacer(modifier = Modifier.height(12.dp))

            ProfileTextField(
                value = state.enrollment,
                onValueChange = { onStateChange(state.copy(enrollment = it)) },
            label = "Enrollment number"
            
        )

            Spacer(modifier = Modifier.height(12.dp))

            ProfileTextField(
                value = if (state.semester > 0) state.semester.toString() else "",
                onValueChange = {
                    onStateChange(
                        state.copy(semester = it.trim().toIntOrNull() ?: 0)
                    )
                },
                label = "Semester",
                keyboardType = KeyboardType.Number
            )
        }

        footer?.invoke()
    }
}

@Composable
private fun RoleButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = if (selected) {
            ButtonDefaults.buttonColors()
        } else {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    ) {
        Text(text = label, fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium)
    }
}

@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
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
