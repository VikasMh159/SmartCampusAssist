package com.smartcampusassist.jpui.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartcampusassist.jpui.main.MainScreenDestination

@Composable
fun BottomBar(
    currentRole: String?,
    selectedDestination: MainScreenDestination,
    onDestinationSelected: (MainScreenDestination) -> Unit
) {
    Surface(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        shape = MaterialTheme.shapes.extraLarge,
        tonalElevation = 6.dp,
        shadowElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            modifier = Modifier.height(74.dp)
        ) {

            NavigationBarItem(
                selected = selectedDestination == MainScreenDestination.Home,
                onClick = {
                    onDestinationSelected(MainScreenDestination.Home)
                },
                label = {
                    Text(
                        text = "Home",
                        fontWeight = if (selectedDestination == MainScreenDestination.Home) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Medium
                        }
                    )
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = if (currentRole == "teacher") "Teacher Home" else "Student Home"
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSurface,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            NavigationBarItem(
                selected = selectedDestination == MainScreenDestination.Assistant,
                onClick = {
                    onDestinationSelected(MainScreenDestination.Assistant)
                },
                label = {
                    Text(
                        text = "AI Chat",
                        fontWeight = if (selectedDestination == MainScreenDestination.Assistant) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Medium
                        }
                    )
                },
                icon = {
                    Icon(Icons.Default.SupportAgent, contentDescription = "AI Chat Bot")
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSurface,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            NavigationBarItem(
                selected = selectedDestination == MainScreenDestination.Profile,
                onClick = {
                    onDestinationSelected(MainScreenDestination.Profile)
                },
                label = {
                    Text(
                        text = "Profile",
                        fontWeight = if (selectedDestination == MainScreenDestination.Profile) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Medium
                        }
                    )
                },
                icon = {
                    Icon(Icons.Default.Person, contentDescription = "Profile")
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSurface,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
