package com.smartcampusassist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.smartcampusassist.jpui.navigation.AppNavGraph
import com.smartcampusassist.jpui.navigation.AppViewModel
import com.smartcampusassist.theme.SmartCampusAssistTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SmartCampusAssistTheme {
                val navController = rememberNavController()
                val appViewModel: AppViewModel = viewModel()

                AppNavGraph(
                    navController = navController,
                    appViewModel = appViewModel
                )
            }
        }
    }
}
