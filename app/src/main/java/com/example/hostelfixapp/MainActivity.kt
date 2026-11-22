package com.example.hostelfixapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.hostelfixapp.data.preferences.AppThemeMode
import com.example.hostelfixapp.data.preferences.ThemePreferences
import com.example.hostelfixapp.data.repository.AuthRepositoryImpl
import com.example.hostelfixapp.data.repository.ReportRepositoryImpl
import com.example.hostelfixapp.navigation.HostelFixNavGraph
import com.example.hostelfixapp.ui.theme.HostelFixAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val context = applicationContext
        val themePreferences = ThemePreferences(context)
        val authRepository = AuthRepositoryImpl(context)
        val reportRepository = ReportRepositoryImpl(context)

        setContent {
            val themeMode by themePreferences.themeMode.collectAsState()
            val systemDark = isSystemInDarkTheme()

            val useDarkTheme = when (themeMode) {
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
                AppThemeMode.SYSTEM -> systemDark
            }

            HostelFixAppTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    HostelFixNavGraph(
                        navController = navController,
                        authRepository = authRepository,
                        reportRepository = reportRepository,
                        themePreferences = themePreferences
                    )
                }
            }
        }
    }
}
