package com.smartcampusassist

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.smartcampusassist.jpui.navigation.AppNavGraph
import com.smartcampusassist.jpui.navigation.AppViewModel
import com.smartcampusassist.theme.SmartCampusAssistTheme
import kotlin.system.exitProcess

private const val CRASH_GUARD_TAG = "CrashGuard"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installCrashRecoveryHandler()

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

    private fun installCrashRecoveryHandler() {
        val existingHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(CRASH_GUARD_TAG, "Uncaught exception on ${thread.name}", throwable)
            scheduleAppRestart()
            existingHandler?.uncaughtException(thread, throwable) ?: run {
                finishAffinity()
                exitProcess(10)
            }
        }
    }

    private fun scheduleAppRestart() {
        val restartIntent = Intent(applicationContext, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            2001,
            restartIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        alarmManager?.setExact(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 700L,
            pendingIntent
        )
    }
}
