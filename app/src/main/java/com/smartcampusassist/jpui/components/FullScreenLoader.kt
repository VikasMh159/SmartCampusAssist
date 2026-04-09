package com.smartcampusassist.jpui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun FullScreenLoader() {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.background.copy(alpha = 0.6f)
            ),
        contentAlignment = Alignment.Center
    ) {

        CircularProgressIndicator(
            strokeWidth = 4.dp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}