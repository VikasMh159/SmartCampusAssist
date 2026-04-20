package com.smartcampusassist.jpui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smartcampusassist.ui.components.GlassCard

@Composable
fun DashboardCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    badgeCount: Int? = null,
    badgeIsWarning: Boolean = false,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier
            .height(126.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(28.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (badgeCount != null && badgeCount > 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .wrapContentSize(),
                    shape = RoundedCornerShape(999.dp),
                    color = if (badgeIsWarning) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                ) {
                    Text(
                        text = badgeCount.toString(),
                        color = if (badgeIsWarning) {
                            MaterialTheme.colorScheme.onError
                        } else {
                            MaterialTheme.colorScheme.onPrimary
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = dashboardIcon(title),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(10.dp).size(20.dp)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.Transparent
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowOutward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private fun dashboardIcon(title: String): ImageVector {
    return when (title) {
        "Schedule" -> Icons.Default.CalendarMonth
        "Assignments", "Upload Assignment", "Account Requests" -> Icons.AutoMirrored.Filled.Assignment
        "AI Chat Bot" -> Icons.Default.SupportAgent
        "Profile" -> Icons.Default.Person
        "Reminders" -> Icons.Default.Campaign
        "Events" -> Icons.Default.Event
        else -> Icons.AutoMirrored.Filled.Assignment
    }
}
