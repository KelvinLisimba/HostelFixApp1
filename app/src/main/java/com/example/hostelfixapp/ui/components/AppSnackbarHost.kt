package com.example.hostelfixapp.ui.components

import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun AppSnackbarHost(snackbarHostState: SnackbarHostState) {
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.imePadding(),
        snackbar = { data ->
            val msg = data.visuals.message
            val isError = msg.contains("⚠️") ||
                    msg.contains("not registered", ignoreCase = true) ||
                    msg.contains("incorrect", ignoreCase = true) ||
                    msg.contains("error", ignoreCase = true) ||
                    msg.contains("failed", ignoreCase = true) ||
                    msg.contains("please fill", ignoreCase = true) ||
                    msg.contains("must contain", ignoreCase = true)

            val containerBg = if (isError) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
            val textColor = if (isError) Color(0xFFC62828) else Color(0xFF2E7D32)

            Snackbar(
                snackbarData = data,
                containerColor = containerBg,
                contentColor = textColor,
                actionColor = textColor
            )
        }
    )
}
